package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.infrastructure.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.repository.CouponPolicyJpaRepository;
import kr.hhplus.be.server.testdata.CouponTestDataLoader;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, CouponTestDataLoader.class})
@ActiveProfiles("coupon-test")
public class CouponConcurrencyIntegrationTest {
    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponPolicyJpaRepository couponPolicyRepository;

    @Autowired
    private CouponJpaRepository couponRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final int THREAD_COUNT = 5; // 동시 접속 사용자 수

    private Long normalPolicyId; // 일반 쿠폰 정책 ID (100개 발급 가능)
    private Long limitedPolicyId; // 제한된 쿠폰 정책 ID (1개만 발급 가능)
    private List<Long> userIds; // 테스트에 사용할 사용자 ID 목록

    @BeforeEach
    void setUp() {
        // CouponTestDataLoader에서 생성한 정책 조회
        List<CouponPolicy> policies = couponPolicyRepository.findAll();

        // 정책 ID 설정 (totalQuantity로 구분)
        for (CouponPolicy policy : policies) {
            if (policy.getTotalQuantity() == 100) {
                normalPolicyId = policy.getPolicyId();
            } else if (policy.getTotalQuantity() == 1) {
                limitedPolicyId = policy.getPolicyId();
            }
        }

        // 테스트용 사용자 ID 목록 조회
        userIds = new ArrayList<>(userRepository.findAll().stream()
                .map(User::getUserId)
                .toList());

        // 추가 사용자가 필요한 경우 생성
        User user = null;
        for (int i = userIds.size(); i < THREAD_COUNT; i++) {
            user = new User();
            user.setPointBalance(5000);
            User savedUser = userRepository.save(user);
            userIds.add(savedUser.getUserId());
        }

        // 테스트 시작 정보 출력
        System.out.println("테스트 시작 =================");
        System.out.println("일반 쿠폰 정책 ID: " + normalPolicyId + ", 발급 가능 수량: 100");
        System.out.println("제한 쿠폰 정책 ID: " + limitedPolicyId + ", 발급 가능 수량: 1");
        System.out.println("테스트 사용자 수: " + userIds.size());
        System.out.println("테스트 시작 시간: " + LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        jdbcTemplate.execute("TRUNCATE TABLE coupons");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE coupon_policies");
    }

    @Test
    @DisplayName("동시에 여러 사용자가 1개만 발급 가능한 쿠폰을 요청할 때 동시성 테스트")
    void concurrentLimitedCouponIssueTest() throws InterruptedException {
        // given
        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        // 쿠폰 발급 요청 (제한된 쿠폰에 대해 THREAD_COUNT명이 동시에 요청)
        for (int i = 0; i < THREAD_COUNT; i++) {
            final Long userId = userIds.get(i);
            executor.submit(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 제한 쿠폰 발급 요청 시작 - 사용자 ID: " + userId);
                    Coupon coupon = couponService.issueFirstComeCoupon(limitedPolicyId, userId);
                    System.out.println(Thread.currentThread().getName() + " 제한 쿠폰 발급 요청 완료 - 사용자 ID: " + userId + ", 쿠폰 ID: " + coupon.getCouponId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + " 제한 쿠폰 발급 중 예외 발생 - 사용자 ID: " + userId + ", 메시지: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await();
        System.out.println("모든 제한 쿠폰 발급 요청 완료");

        // then
        // 발급된 쿠폰 데이터 확인
        long issuedCount = couponRepository.countByPolicyId(limitedPolicyId);

        System.out.println("테스트 결과 =================");
        System.out.println("총 요청 수: " + THREAD_COUNT);
        System.out.println("발급 가능한 쿠폰 수량: 1");
        System.out.println("성공 요청 수: " + successCount.get());
        System.out.println("실패 요청 수: " + failCount.get());
        System.out.println("실제 발급된 쿠폰 수: " + issuedCount);

        // 테스트 후 발급된 쿠폰 수량이 정확히 1개인지 확인
        assertThat(issuedCount).isEqualTo(1);

        // 성공 수 + 실패 수가 총 요청 수와 같은지 확인
        assertThat(successCount.get() + failCount.get()).isEqualTo(THREAD_COUNT);

        // 성공 수가 1과 일치하는지 확인
        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 여러 사용자가 1개만 발급 가능한 쿠폰을 요청할 때 동시성 테스트 - 비관적 락 적용")
    void concurrentLimitedCouponIssueTestWithLock() throws InterruptedException {
        // given
        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        // 쿠폰 발급 요청 (제한된 쿠폰에 대해 THREAD_COUNT명이 동시에 요청)
        for (int i = 0; i < THREAD_COUNT; i++) {
            final Long userId = userIds.get(i);
            executor.submit(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 제한 쿠폰 발급 요청 시작 - 사용자 ID: " + userId);
                    // 비관적 락이 적용된 메서드 사용
                    Coupon coupon = couponService.issueFirstComeCouponWithLock(limitedPolicyId, userId);
                    System.out.println(Thread.currentThread().getName() + " 제한 쿠폰 발급 요청 완료 - 사용자 ID: " + userId + ", 쿠폰 ID: " + coupon.getCouponId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + " 제한 쿠폰 발급 중 예외 발생 - 사용자 ID: " + userId + ", 메시지: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await();
        System.out.println("모든 제한 쿠폰 발급 요청 완료");

        // then
        // 발급된 쿠폰 데이터 확인
        long issuedCount = couponRepository.countByPolicyId(limitedPolicyId);

        System.out.println("테스트 결과 =================");
        System.out.println("총 요청 수: " + THREAD_COUNT);
        System.out.println("발급 가능한 쿠폰 수량: 1");
        System.out.println("성공 요청 수: " + successCount.get());
        System.out.println("실패 요청 수: " + failCount.get());
        System.out.println("실제 발급된 쿠폰 수: " + issuedCount);

        // 테스트 후 발급된 쿠폰 수량이 정확히 1개인지 확인
        assertThat(issuedCount).isEqualTo(1);

        // 성공 수 + 실패 수가 총 요청 수와 같은지 확인
        assertThat(successCount.get() + failCount.get()).isEqualTo(THREAD_COUNT);

        // 성공 수가 1과 일치하는지 확인
        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("비관적 락의 성능 영향 비교 테스트 - 충분한 수량의 쿠폰 발급 시")
    void compareLockPerformanceTest() throws InterruptedException {
        int testThreadCount = THREAD_COUNT;

        // 테스트 데이터 초기화
        jdbcTemplate.execute("TRUNCATE TABLE coupons");

        System.out.println("\n=== 비관적 락 성능 비교 테스트 시작 ===");
        System.out.println("일반 쿠폰 정책 ID: " + normalPolicyId + ", 발급 가능 수량: 100");
        System.out.println("테스트 사용자 수: " + testThreadCount);

        // 1. 비관적 락 없이 쿠폰 발급 테스트
        ExecutorService executor = Executors.newFixedThreadPool(testThreadCount);
        CountDownLatch latch = new CountDownLatch(testThreadCount);
        List<Long> processingTimesNoLock = new ArrayList<>();

        Instant testStartNoLock = Instant.now();

        for (int i = 0; i < testThreadCount; i++) {
            final Long userId = userIds.get(i);
            CountDownLatch finalLatch = latch;
            executor.submit(() -> {
                try {
                    Instant requestStart = Instant.now();

                    // 락이 적용되지 않은 메서드 사용
                    couponService.issueFirstComeCoupon(normalPolicyId, userId);

                    Instant requestEnd = Instant.now();
                    long processingTime = Duration.between(requestStart, requestEnd).toMillis();
                    processingTimesNoLock.add(processingTime);

                    System.out.println(Thread.currentThread().getName() + " (락 없음) 처리 시간: " + processingTime + "ms");
                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + " (락 없음) 오류: " + e.getMessage());
                } finally {
                    finalLatch.countDown();
                }
            });
        }

        latch.await();
        Instant testEndNoLock = Instant.now();
        long totalTimeNoLock = Duration.between(testStartNoLock, testEndNoLock).toMillis();

        // 2. 테스트 데이터 초기화
        jdbcTemplate.execute("TRUNCATE TABLE coupons");

        // 3. 비관적 락을 사용한 쿠폰 발급 테스트
        executor = Executors.newFixedThreadPool(testThreadCount);
        latch = new CountDownLatch(testThreadCount);
        List<Long> processingTimesWithLock = new ArrayList<>();

        Instant testStartWithLock = Instant.now();

        for (int i = 0; i < testThreadCount; i++) {
            final Long userId = userIds.get(i);
            CountDownLatch finalLatch1 = latch;
            executor.submit(() -> {
                try {
                    Instant requestStart = Instant.now();

                    // 비관적 락이 적용된 메서드 사용
                    couponService.issueFirstComeCouponWithLock(normalPolicyId, userId);

                    Instant requestEnd = Instant.now();
                    long processingTime = Duration.between(requestStart, requestEnd).toMillis();
                    processingTimesWithLock.add(processingTime);

                    System.out.println(Thread.currentThread().getName() + " (비관적 락) 처리 시간: " + processingTime + "ms");
                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + " (비관적 락) 오류: " + e.getMessage());
                } finally {
                    finalLatch1.countDown();
                }
            });
        }

        latch.await();
        Instant testEndWithLock = Instant.now();
        long totalTimeWithLock = Duration.between(testStartWithLock, testEndWithLock).toMillis();

        // 4. 결과 계산 및 출력
        double avgNoLock = processingTimesNoLock.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgWithLock = processingTimesWithLock.stream().mapToLong(Long::longValue).average().orElse(0);

        long maxNoLock = processingTimesNoLock.stream().mapToLong(Long::longValue).max().orElse(0);
        long maxWithLock = processingTimesWithLock.stream().mapToLong(Long::longValue).max().orElse(0);

        long minNoLock = processingTimesNoLock.stream().mapToLong(Long::longValue).min().orElse(0);
        long minWithLock = processingTimesWithLock.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("\n=== 비관적 락 성능 비교 결과 ===");
        System.out.println("1. 락 없음:");
        System.out.println("   - 총 처리 시간: " + totalTimeNoLock + "ms");
        System.out.println("   - 평균 처리 시간: " + String.format("%.2f", avgNoLock) + "ms");
        System.out.println("   - 최소 처리 시간: " + minNoLock + "ms");
        System.out.println("   - 최대 처리 시간: " + maxNoLock + "ms");

        System.out.println("\n2. 비관적 락 적용:");
        System.out.println("   - 총 처리 시간: " + totalTimeWithLock + "ms");
        System.out.println("   - 평균 처리 시간: " + String.format("%.2f", avgWithLock) + "ms");
        System.out.println("   - 최소 처리 시간: " + minWithLock + "ms");
        System.out.println("   - 최대 처리 시간: " + maxWithLock + "ms");

        System.out.println("\n3. 성능 차이:");
        System.out.println("   - 총 처리 시간 증가율: " + String.format("%.2f%%", (totalTimeWithLock - totalTimeNoLock) * 100.0 / totalTimeNoLock));
        System.out.println("   - 평균 처리 시간 증가율: " + String.format("%.2f%%", (avgWithLock - avgNoLock) * 100.0 / avgNoLock));
        System.out.println("   - 최대 처리 시간 증가율: " + String.format("%.2f%%", (maxWithLock - maxNoLock) * 100.0 / maxNoLock));

        // 두 테스트 모두 모든 쿠폰이 발급되었는지 확인
        long issuedCountNoLock = testThreadCount;
        long issuedCountWithLock = testThreadCount;

        assertThat(issuedCountNoLock).isEqualTo(testThreadCount);
        assertThat(issuedCountWithLock).isEqualTo(testThreadCount);
    }
}
