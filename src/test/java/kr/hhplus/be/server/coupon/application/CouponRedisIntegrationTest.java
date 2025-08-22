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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
@SqlGroup(@Sql(scripts = {"/sql/clear-data.sql", "/sql/insert-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD))
public class CouponRedisIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            Thread.sleep(100);
            log.info("Redis 초기화 완료");
        } catch (Exception e) {
            throw new RuntimeException("Redis 연결 실패", e);
        }
    }

    @Test
    @DisplayName("기본 대기열 진입 테스트")
    void requestCoupon_shouldEnterQueue() {
        // given
        Long policyId = 1L;
        Long userId = 1001L;

        // when
        WaitingQueueResponse response = couponService.requestCoupon(policyId, userId);

        // then
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getPosition()).isEqualTo(1);

        String queueKey = String.format("coupon:queue:%d", policyId);
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        assertThat(queueSize).isEqualTo(1L);
    }

    @Test
    @DisplayName("동일 사용자 동시 요청 - 중복 방지")
    void requestCoupon_sameUserConcurrent_shouldPreventDuplicate() throws InterruptedException {
        // given
        Long policyId = 1L;
        Long userId = 6000L;
        int concurrentCount = 10;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentCount);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentCount);

        // when - 동시 요청
        for (int i = 0; i < concurrentCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    couponService.requestCoupon(policyId, userId);
                } catch (Exception e) {
                    log.debug("요청 실패: {}", e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then - 대기열에 1번만 등록
        String queueKey = String.format("coupon:queue:%d", policyId);
        List<String> queue = redisTemplate.opsForList().range(queueKey, 0, -1);

        long userInQueueCount = queue.stream()
                .filter(id -> userId.toString().equals(id))
                .count();

        assertThat(userInQueueCount).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 사용자 동시 요청 - 모두 진입")
    void requestCoupon_differentUsersConcurrent_shouldAllEnterQueue() throws InterruptedException {
        // given
        Long policyId = 1L;
        int userCount = 10;
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            userIds.add(7000L + i);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(userCount);

        // when
        for (Long userId : userIds) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    WaitingQueueResponse response = couponService.requestCoupon(policyId, userId);
                    if ("WAITING".equals(response.getStatus())) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.debug("사용자 {} 요청 실패: {}", userId, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        String queueKey = String.format("coupon:queue:%d", policyId);
        Long queueSize = redisTemplate.opsForList().size(queueKey);

        assertThat(queueSize).isEqualTo(userCount);
        assertThat(successCount.get()).isEqualTo(userCount);
    }

    @Test
    @DisplayName("비동기 큐 처리 - 정상 발급 완료")
    void processWaitingQueue_shouldIssueCoupons() throws InterruptedException {
        // given
        Long policyId = 4L; // 충분한 재고 (1000개)
        List<Long> userIds = List.of(8001L, 8002L, 8003L, 8004L, 8005L);

        // 대기열에 사용자 추가
        for (Long userId : userIds) {
            WaitingQueueResponse response = couponService.requestCoupon(policyId, userId);
            assertThat(response.getStatus()).isEqualTo("WAITING");
        }

        long initialIssuedCount = couponRepository.countByPolicyId(policyId);

        // when - 비동기 처리
        couponService.processWaitingQueues();

        // 처리 완료까지 대기
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    long completedUsers = userIds.stream()
                            .mapToLong(userId -> {
                                WaitingQueueResponse status = couponService.getQueueStatus(policyId, userId);
                                return "COMPLETED".equals(status.getStatus()) ? 1 : 0;
                            })
                            .sum();
                    assertThat(completedUsers).isEqualTo(userIds.size());
                });

        // then
        String queueKey = String.format("coupon:queue:%d", policyId);
        Long finalQueueSize = redisTemplate.opsForList().size(queueKey);
        long finalIssuedCount = couponRepository.countByPolicyId(policyId);
        long newlyIssuedCount = finalIssuedCount - initialIssuedCount;

        assertThat(finalQueueSize).isEqualTo(0); // 대기열 비워짐
        assertThat(newlyIssuedCount).isEqualTo(userIds.size()); // 모든 사용자 발급

        // 개별 검증
        for (Long userId : userIds) {
            WaitingQueueResponse status = couponService.getQueueStatus(policyId, userId);
            boolean isIssued = couponRepository.existsByUserIdAndPolicyId(userId, policyId);

            assertThat(status.getStatus()).isEqualTo("COMPLETED");
            assertThat(isIssued).isTrue();
        }
    }

    @Test
    @DisplayName("재고 부족 시 비동기 처리 - 일부만 발급")
    void processWaitingQueue_limitedStock_shouldStopWhenOutOfStock() throws InterruptedException {
        // given
        Long policyId = 3L; // 재고 5개
        int userCount = 8;
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            userIds.add(9000L + i);
        }

        // 모든 사용자를 대기열에 추가
        for (Long userId : userIds) {
            WaitingQueueResponse response = couponService.requestCoupon(policyId, userId);
            assertThat(response.getStatus()).isEqualTo("WAITING");
        }

        long initialIssuedCount = couponRepository.countByPolicyId(policyId);
        int availableStock = 5 - (int) initialIssuedCount;

        // when - 비동기 처리
        couponService.processWaitingQueues();

        // 재고 소진까지 대기
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    long currentIssuedCount = couponRepository.countByPolicyId(policyId);
                    assertThat(currentIssuedCount).isLessThanOrEqualTo(5);

                    // 더 이상 처리되지 않는지 확인
                    Thread.sleep(1000);
                    long secondCheckCount = couponRepository.countByPolicyId(policyId);
                    assertThat(secondCheckCount).isEqualTo(currentIssuedCount);
                });

        // then
        long finalIssuedCount = couponRepository.countByPolicyId(policyId);
        long newlyIssuedCount = finalIssuedCount - initialIssuedCount;
        String queueKey = String.format("coupon:queue:%d", policyId);
        Long remainingQueueSize = redisTemplate.opsForList().size(queueKey);

        assertThat(finalIssuedCount).isEqualTo(5); // 재고 한계 준수
        assertThat(newlyIssuedCount).isEqualTo(availableStock);
        assertThat(remainingQueueSize).isGreaterThan(0); // 미처리 사용자 존재

        // 발급받은 사용자와 대기 중인 사용자 구분
        long completedUsers = userIds.stream()
                .mapToLong(userId -> {
                    WaitingQueueResponse status = couponService.getQueueStatus(policyId, userId);
                    return "COMPLETED".equals(status.getStatus()) ? 1 : 0;
                })
                .sum();

        assertThat(completedUsers).isEqualTo(availableStock);
    }
}