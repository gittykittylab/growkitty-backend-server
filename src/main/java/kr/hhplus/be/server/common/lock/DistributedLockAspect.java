package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @DistributedLock 선언 시 수행되는 Aop class
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // 트랜잭션(@Order(2))보다 먼저 실행되도록 설정
public class DistributedLockAspect {
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";
    private static final String MULTI_LOCK_PREFIX = "multi:";
    private static final String PRODUCT_LOCK_PREFIX = "PRODUCT:";

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(kr.hhplus.be.server.common.lock.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = distributedLock.key();

        // 다중 락 처리
        if (key.startsWith(MULTI_LOCK_PREFIX)) {
            return handleMultiLock(joinPoint, signature, distributedLock);
        }

        // 단일 락 처리
        return handleSingleLock(joinPoint, signature, distributedLock);
    }

    /**
     * 단일 락 처리 메소드
     */
    private Object handleSingleLock(ProceedingJoinPoint joinPoint, MethodSignature signature, DistributedLock distributedLock) throws Throwable {
        String key = REDISSON_LOCK_PREFIX + getKey(distributedLock.key(), signature.getParameterNames(), joinPoint.getArgs());
        RLock rLock = redissonClient.getLock(key);

        log.info("# AOP 단일 락 시작: {}", key);

        try {
            boolean acquired = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());

            if (!acquired) {
                log.warn("락 획득 실패: {}", key);
                throw new RuntimeException("이미 진행 중인 작업입니다.");
            }

            log.debug("락 획득 성공: {}", key);

            // 트랜잭션 시작
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생 : {}", key, e);
            throw new RuntimeException("락 획득 중 인터럽트가 발생했습니다.");
        } finally {
            // 락 해제 시도
            if (rLock != null && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.debug("락 해제: {}", key);
            }
            log.info("# AOP 단일 락 종료: {}", key);
        }
    }

    /**
     * 다중 락 처리 메소드
     */
    private Object handleMultiLock(ProceedingJoinPoint joinPoint, MethodSignature signature, DistributedLock distributedLock) throws Throwable {
        // multi: 접두사 제거
        String expressionString = distributedLock.key().substring(MULTI_LOCK_PREFIX.length());

        // SpEL 표현식 평가를 위한 컨텍스트 설정
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 표현식 평가
        Expression expression = parser.parseExpression(expressionString);
        Object result = expression.getValue(context);

        List<Long> productIds = new ArrayList<>();

        // 결과가 컬렉션인 경우 (여러 상품 ID)
        if (result instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) result;
            for (Object item : collection) {
                if (item instanceof Long) {
                    productIds.add((Long) item);
                } else if (item instanceof Number) {
                    productIds.add(((Number) item).longValue());
                }
            }
        }
        // 단일 값인 경우
        else if (result instanceof Long) {
            productIds.add((Long) result);
        }

        log.info("# AOP 다중 락 시작, 상품 수: {}", productIds.size());

        // 모든 락을 저장할 리스트
        List<RLock> locks = new ArrayList<>();

        try {
            // 모든 상품에 대해 락 획득 시도
            for (Long productId : productIds) {
                String lockKey = REDISSON_LOCK_PREFIX + PRODUCT_LOCK_PREFIX + productId;
                RLock lock = redissonClient.getLock(lockKey);

                boolean acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());

                if (!acquired) {
                    log.warn("상품 락 획득 실패: {}", lockKey);
                    // 이미 획득한 락들 모두 해제
                    for (RLock acquiredLock : locks) {
                        if (acquiredLock.isHeldByCurrentThread()) {
                            acquiredLock.unlock();
                        }
                    }
                    throw new RuntimeException("상품 " + productId + "에 대한 작업이 이미 진행 중입니다.");
                }

                locks.add(lock);
                log.debug("상품 락 획득 성공: {}", lockKey);
            }

            // 모든 락 획득 성공, 트랜잭션 시작
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("다중 락 획득 중 인터럽트 발생", e);
            throw new RuntimeException("락 획득 중 인터럽트가 발생했습니다.");
        } finally {
            // 모든 락 해제
            for (RLock lock : locks) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("락 해제: {}", lock.getName());
                }
            }
            log.info("# AOP 다중 락 종료, 해제된 락 수: {}", locks.size());
        }
    }

    /**
     * 키 생성 메소드 (기존 메소드)
     */
    private String getKey(String keyPattern, String[] parameterNames, Object[] args) {
        if (!keyPattern.contains("#")) {
            return keyPattern;
        }

        String resultKey = keyPattern;
        for (int i = 0; i < parameterNames.length; i++) {
            String paramName = "#" + parameterNames[i];
            if (resultKey.contains(paramName)) {
                resultKey = resultKey.replace(paramName, args[i] != null ? args[i].toString() : "null");
            }
        }

        return resultKey;
    }
}