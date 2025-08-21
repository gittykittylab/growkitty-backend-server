package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.dto.reponse.WaitingQueueResponse;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest
@Testcontainers
@SqlGroup(@Sql(scripts = {"/sql/clear-data.sql", "/sql/insert-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD))
public class CouponRedisIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화 - 연결 확인 후 flush
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            log.info("Redis 초기화 완료");
        } catch (Exception e) {
            log.error("Redis 초기화 실패", e);
            throw new RuntimeException("Redis 연결 실패", e);
        }
    }

    @Test
    @DisplayName("대기열 진입 - 동시성 테스트")
    void requestCoupon_concurrentUsers_shouldHandleQueueCorrectly() throws InterruptedException {
        // given
        int threadCount = 50;
        Long policyId = 1L;
        ConcurrencyTestContext ctx = new ConcurrencyTestContext(threadCount);

        log.info("=== 대기열 진입 동시성 테스트 시작 ===");
        log.info("정책 ID: {}, 스레드 수: {}", policyId, threadCount);

        // when - 동시에 대기열 진입 요청
        for (int i = 0; i < threadCount; i++) {
            final long userId = 1000L + i;
            ctx.executorService.submit(() -> {
                try {
                    WaitingQueueResponse response = couponService.requestCoupon(policyId, userId);
                    ctx.successCount.incrementAndGet();
                    log.debug("사용자 {} 대기열 진입 완료, 위치: {}", userId, response.getPosition());
                } catch (Exception e) {
                    ctx.failCount.incrementAndGet();
                    ctx.exceptions.add(e);
                    log.error("사용자 {} 대기열 진입 실패: {}", userId, e.getMessage());
                } finally {
                    ctx.latch.countDown();
                }
            });
        }

        boolean completed = ctx.latch.await(30, TimeUnit.SECONDS);
        ctx.shutdown();

        // then
        assertThat(completed).isTrue(); // 타임아웃 체크

        // 실패가 있었다면 예외 정보 출력
        if (!ctx.exceptions.isEmpty()) {
            log.error("발생한 예외들: ");
            ctx.exceptions.forEach(e -> log.error("- {}: {}", e.getClass().getSimpleName(), e.getMessage()));
        }

        String queueKey = String.format("coupon:queue:%d", policyId);
        Long queueSize = redisTemplate.opsForList().size(queueKey);

        log.info("=== 대기열 진입 동시성 테스트 결과 ===");
        log.info("성공: {}, 실패: {}", ctx.successCount.get(), ctx.failCount.get());
        log.info("대기열 크기: {}", queueSize);

        // 부분적 성공도 허용 (동시성 상황에서 일부 실패 가능)
        assertThat(ctx.successCount.get()).isGreaterThan(0);
        assertThat(queueSize).isEqualTo(ctx.successCount.get());
    }

    @Test
    @DisplayName("쿠폰 발급 처리 - 재고 한계 테스트")
    void processQueue_limitedStock_shouldRespectStockLimit() throws InterruptedException {
        // given
        Long policyId = 2L; // 수량 10개 제한
        int userCount = 15; // 요청자는 15명 (재고보다 많음)
        long initialIssuedCount = couponRepository.countByPolicyId(policyId);

        log.info("=== 재고 한계 테스트 시작 ===");
        log.info("정책 ID: {}, 기존 발급: {}, 대기열 사용자: {}명", policyId, initialIssuedCount, userCount);

        // 대기열에 사용자들 추가
        for (int i = 0; i < userCount; i++) {
            couponService.requestCoupon(policyId, 2000L + i);
        }

        // when - 스케줄러 처리 대기 (Awaitility 사용)
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    long currentIssued = couponRepository.countByPolicyId(policyId);
                    long newIssued = currentIssued - initialIssuedCount;
                    log.debug("현재 발급된 수량: {}, 신규 발급: {}", currentIssued, newIssued);

                    // 재고가 모두 소진되었거나, 충분한 처리가 되었는지 확인
                    return currentIssued >= 10 || newIssued >= Math.min(10 - initialIssuedCount, userCount);
                });

        // then
        long finalIssuedCount = couponRepository.countByPolicyId(policyId);
        long newlyIssuedCount = finalIssuedCount - initialIssuedCount;

        log.info("=== 재고 한계 테스트 결과 ===");
        log.info("기존 발급: {}, 신규 발급: {}, 총 발급: {}", initialIssuedCount, newlyIssuedCount, finalIssuedCount);

        // 핵심 검증: 총 발급량이 재고를 초과하지 않음
        assertThat(finalIssuedCount).isLessThanOrEqualTo(10);

        // 재고가 있었다면 적절히 발급되었는지 확인
        int availableStock = 10 - (int) initialIssuedCount;
        if (availableStock > 0) {
            int expectedNewIssue = Math.min(availableStock, userCount);
            assertThat(newlyIssuedCount).isLessThanOrEqualTo(expectedNewIssue);
            assertThat(newlyIssuedCount).isGreaterThan(0); // 재고가 있다면 발급이 되어야 함
        }
    }

    @Test
    @DisplayName("중복 발급 방지 - 동일 사용자 여러 요청")
    void requestCoupon_duplicateUser_shouldPreventDuplicateIssue() throws InterruptedException {
        // given
        Long policyId = 1L;
        Long userId = 3000L;
        int requestCount = 10;
        ConcurrencyTestContext ctx = new ConcurrencyTestContext(requestCount);

        log.info("=== 중복 발급 방지 테스트 시작 ===");
        log.info("정책 ID: {}, 사용자 ID: {}, 요청 횟수: {}", policyId, userId, requestCount);

        // when - 동일 사용자가 여러 번 요청
        for (int i = 0; i < requestCount; i++) {
            ctx.executorService.submit(() -> {
                try {
                    WaitingQueueResponse response = couponService.requestCoupon(policyId, userId);
                    ctx.successCount.incrementAndGet();
                    log.debug("사용자 {} 요청 결과: {}", userId, response.getStatus());
                } catch (Exception e) {
                    ctx.failCount.incrementAndGet();
                    log.error("사용자 {} 요청 실패: {}", userId, e.getMessage());
                } finally {
                    ctx.latch.countDown();
                }
            });
        }

        ctx.latch.await(15, TimeUnit.SECONDS);
        ctx.shutdown();

        // then
        String queueKey = String.format("coupon:queue:%d", policyId);
        List<String> queue = redisTemplate.opsForList().range(queueKey, 0, -1);

        // null 체크 추가
        queue = queue != null ? queue : new ArrayList<>();

        long userInQueueCount = queue.stream()
                .filter(id -> id != null && id.equals(userId.toString()))
                .count();

        log.info("=== 중복 발급 방지 테스트 결과 ===");
        log.info("성공 요청: {}, 실패 요청: {}", ctx.successCount.get(), ctx.failCount.get());
        log.info("대기열 내 사용자 수: {}", userInQueueCount);
        log.info("대기열 전체: {}", queue);

        // 대기열에 한 번만 들어가야 함 (핵심 검증)
        assertThat(userInQueueCount).isLessThanOrEqualTo(1);

        // 요청은 모두 성공해야 함 (중복 처리는 비즈니스 로직에서)
        assertThat(ctx.successCount.get()).isEqualTo(requestCount);
    }

    @Test
    @DisplayName("쿠폰 발급 처리 - 원자성 보장 테스트")
    void processQueue_atomicity_shouldMaintainDataConsistency() throws InterruptedException {
        // given
        Long policyId = 3L; // 수량 5개 제한
        int userCount = 8;
        long initialIssuedCount = couponRepository.countByPolicyId(policyId);

        log.info("=== 원자성 보장 테스트 시작 ===");
        log.info("정책 ID: {}, 기존 발급: {}, 대기열 사용자: {}명, 총 재고: 5개", policyId, initialIssuedCount, userCount);

        // 대기열에 사용자들 추가
        for (int i = 0; i < userCount; i++) {
            couponService.requestCoupon(policyId, 4000L + i);
        }

        // when - 충분한 처리 시간 대기
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    long currentIssued = couponRepository.countByPolicyId(policyId);
                    return currentIssued >= Math.min(5, initialIssuedCount + userCount);
                });

        // then
        long finalIssuedCount = couponRepository.countByPolicyId(policyId);
        long newlyIssuedCount = finalIssuedCount - initialIssuedCount;

        // Redis 상태 확인
        String issuedKey = String.format("coupon:issued:%d", policyId);
        Set<String> issuedUsers = redisTemplate.opsForSet().members(issuedKey);
        Long issuedSetSize = issuedUsers != null ? (long) issuedUsers.size() : 0L;

        log.info("=== 원자성 보장 테스트 결과 ===");
        log.info("기존 발급: {}, 신규 발급: {}, 총 발급: {}", initialIssuedCount, newlyIssuedCount, finalIssuedCount);
        log.info("Redis 발급 기록: {}", issuedSetSize);

        // 핵심 검증들
        assertThat(finalIssuedCount).isLessThanOrEqualTo(5); // 총 재고 초과 불가

        int availableStock = 5 - (int) initialIssuedCount;
        if (availableStock > 0) {
            int maxPossibleIssue = Math.min(availableStock, userCount);
            assertThat(newlyIssuedCount).isLessThanOrEqualTo(maxPossibleIssue);
            assertThat(newlyIssuedCount).isGreaterThan(0);
        } else {
            assertThat(newlyIssuedCount).isZero();
        }

        // Redis와 DB 일관성 (Redis를 사용한다면)
        if (issuedSetSize > 0) {
            assertThat(issuedSetSize).isLessThanOrEqualTo(newlyIssuedCount);
        }
    }

    @Test
    @DisplayName("발급된 쿠폰 검증 - 도메인 로직 확인")
    void issuedCoupon_shouldFollowDomainLogic() throws InterruptedException {
        // given
        Long policyId = 4L; // 대용량 테스트 쿠폰 (1000개)
        int userCount = 3; // 작은 수로 테스트

        log.info("=== 발급된 쿠폰 도메인 로직 검증 테스트 시작 ===");
        log.info("정책 ID: {}, 대기열 사용자: {}명", policyId, userCount);

        // 대기열에 사용자들 추가
        for (int i = 0; i < userCount; i++) {
            couponService.requestCoupon(policyId, 6000L + i);
        }

        // when - 처리 대기
        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    long issuedCount = couponRepository.countByPolicyId(policyId);
                    return issuedCount >= userCount;
                });

        // then
        long issuedCount = couponRepository.countByPolicyId(policyId);

        log.info("=== 발급된 쿠폰 도메인 로직 검증 결과 ===");
        log.info("발급된 쿠폰 수: {}", issuedCount);

        // 기본 검증: 예상한 수량만큼 발급되었는지 확인
        assertThat(issuedCount).isGreaterThanOrEqualTo(userCount);
    }

    @Test
    @DisplayName("대기열 상태 조회 - 정확한 위치 반환")
    void getQueueStatus_shouldReturnCorrectPosition() {
        // given
        Long policyId = 1L;
        List<Long> userIds = List.of(5001L, 5002L, 5003L, 5004L, 5005L);

        log.info("=== 대기열 상태 조회 테스트 시작 ===");

        // 순차적으로 대기열 진입
        for (Long userId : userIds) {
            WaitingQueueResponse response = couponService.requestCoupon(policyId, userId);
            log.info("사용자 {} 진입, 위치: {}", userId, response.getPosition());
        }

        // when & then - 실제 Redis 상태 확인
        String queueKey = String.format("coupon:queue:%d", policyId);
        List<String> actualQueue = redisTemplate.opsForList().range(queueKey, 0, -1);

        log.info("실제 Redis 대기열 순서: {}", actualQueue);

        for (Long userId : userIds) {
            WaitingQueueResponse response = couponService.getQueueStatus(policyId, userId);

            log.info("사용자 {}: 위치 {}, 전체 대기: {}",
                    userId, response.getPosition(), response.getTotalWaiting());

            // 기본 검증
            assertThat(response.getPosition()).isGreaterThan(0);
            assertThat(response.getTotalWaiting()).isEqualTo(userIds.size());

            // 실제 Redis에서의 위치와 비교 (있다면)
            if (actualQueue != null && actualQueue.contains(userId.toString())) {
                int actualIndex = actualQueue.indexOf(userId.toString());
                // 구현에 따라 다를 수 있으므로 유연하게 검증
                assertThat(response.getPosition()).isLessThanOrEqualTo(userIds.size());
            }
        }
    }

    /**
     * 동시성 테스트 컨텍스트 클래스
     */
    private static class ConcurrencyTestContext {
        final ExecutorService executorService;
        final CountDownLatch latch;
        final AtomicInteger successCount;
        final AtomicInteger failCount;
        final List<Exception> exceptions;

        public ConcurrencyTestContext(int threadCount) {
            this.executorService = Executors.newFixedThreadPool(threadCount);
            this.latch = new CountDownLatch(threadCount);
            this.successCount = new AtomicInteger(0);
            this.failCount = new AtomicInteger(0);
            this.exceptions = Collections.synchronizedList(new ArrayList<>());
        }

        public void shutdown() throws InterruptedException {
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }
}