package kr.hhplus.be.server.common.lock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;

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

    private final RedissonClient redissonClient;

    @Around("@annotation(kr.hhplus.be.server.common.lock.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = REDISSON_LOCK_PREFIX + getKey(distributedLock.key(), signature.getParameterNames(), joinPoint.getArgs());

        RLock rLock = redissonClient.getLock(key);

        log.info("# AOP 시작");

        try {
            boolean acquired = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());

            if(!acquired){
                log.warn("락 획득 실패: {}", key);
                throw new RuntimeException("이미 진행 중인 작업입니다.");
            }

            log.debug("락 획득 성공: {}",key);

            // 트랜잭션 시작
            return joinPoint.proceed();

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생 : {}", key, e);
            throw new RuntimeException("락 획득 중 인터럽트가 발생했습니다.");
        }finally {
            // 락 해제 시도
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.debug("락 해제: {}", key);
            }
            log.info("# AOP 종료");
        }
    }

    /**
     * 키 생성 메소드
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
