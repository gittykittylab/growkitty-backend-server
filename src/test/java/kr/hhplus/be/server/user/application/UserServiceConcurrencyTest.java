package kr.hhplus.be.server.user.application;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.testdata.UserTestDataLoader;
import kr.hhplus.be.server.user.domain.PointHistory;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.repository.PointHistoryJpaRepository;
import kr.hhplus.be.server.user.infrastructure.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, UserTestDataLoader.class})
@ActiveProfiles("user-test")
public class UserServiceConcurrencyTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;
    
    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사용자가 여러번 포인트 충전 시 동시성 문제 발생 테스트 - 락 미적용")
        // 사용자가 여러번 포인트 충전을 요청했을 때, 모두 충전되어야 함
    void concurrentPointChargeTest() throws InterruptedException{
        // given
        // 충분한 포인트를 가진 사용자 선택
        User user = userJpaRepository.findAll().get(0);
        Long userId = user.getUserId();
        int initialBalance = user.getPointBalance();
        int chargeAmount = 100;
        int threadCount = 5;

        // 테스트 시작 시간 기록
        LocalDateTime testStartTime = LocalDateTime.now();
        System.out.println("테스트 시작 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance + ", 테스트 시작 시간: " + testStartTime);

        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // when
            // 충전 요청
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + "번째 충전 요청 시작");
                        userService.chargePoint(userId, chargeAmount);
                        System.out.println(Thread.currentThread().getName() + "번째 충전 요청 완료");
                    } finally {
                        latch.countDown(); // 작업이 완료되면 카운트 감소
                    }
                });
            }

            // 모든 작업이 완료될 때까지 대기
            latch.await();
            System.out.println("모든 충전 요청 완료");

            // then
            // 충전 후 데이터
            User updatedUser = userJpaRepository.findById(userId).orElseThrow();
            int finalBalance = updatedUser.getPointBalance();

            // 테스트 중 생성된 이력 조회 (testStartTime 이후 생성된 것만)
            List<PointHistory> newHistories = pointHistoryJpaRepository.findAll().stream()
                    .filter(history -> history.getUserId().equals(userId)
                            && history.getPointType().equals("CHARGE")
                            && history.getCreatedAt().isAfter(testStartTime))
                    .toList();
            
            // 총 충전 금액
            int totalChargeAmount = newHistories.stream()
                    .mapToInt(PointHistory::getAmount)
                    .sum();

            System.out.println("초기 잔액: " + initialBalance);
            System.out.println("최종 잔액: " + finalBalance);
            System.out.println("예상 잔액: " + (initialBalance + (chargeAmount * threadCount)));

            // 잔액이 기대했던 금액만큼(200) 정확히 증가했는지 확인
            assertThat(finalBalance).isEqualTo(initialBalance + chargeAmount * threadCount);

            // 적어도 우리가 요청한 5개 이상의 충전 이력이 있는지 확인
            assertThat(newHistories.size()).isGreaterThanOrEqualTo(threadCount);

            // 충전 금액의 총합이 기대하는 금액과 일치하는지 확인
            assertThat(Math.abs(totalChargeAmount)).isEqualTo(chargeAmount * threadCount);

        }finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("사용자가 여러번 포인트 차감 시 동시성 문제 발생 테스트 - 락 미적용")
    void concurrentPointUseTest() throws InterruptedException {
        // given
        // 충분한 포인트를 가진 사용자 선택
        User user = userJpaRepository.findAll().get(0);
        Long userId = user.getUserId();
        int initialBalance = user.getPointBalance();

        int useAmount = 100;
        int threadCount = 5;

        // 테스트 시작 시간 기록
        LocalDateTime testStartTime = LocalDateTime.now();
        System.out.println("테스트 시작 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance + ", 테스트 시작 시간: " + testStartTime);

        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // when
            // 차감 요청
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + "번째 차감 요청 시작");
                        userService.usePoint(userId, useAmount);
                        System.out.println(Thread.currentThread().getName() + "번째 차감 요청 완료");
                    } finally {
                        latch.countDown(); // 작업이 완료되면 카운트 감소
                    }
                });
            }

            // 모든 작업이 완료될 때까지 대기
            latch.await();
            System.out.println("모든 차감 요청 완료");

            // then
            // 차감 후 데이터
            User updatedUser = userJpaRepository.findById(userId).orElseThrow();
            int finalBalance = updatedUser.getPointBalance();

            // 테스트 중 생성된 이력 조회 (testStartTime 이후 생성된 것만)
            List<PointHistory> newHistories = pointHistoryJpaRepository.findAll().stream()
                    .filter(history -> history.getUserId().equals(userId)
                            && history.getPointType().equals("USE")
                            && history.getCreatedAt().isAfter(testStartTime))
                    .toList();

            // 총 차감 금액
            int totalUsedAmount = newHistories.stream()
                    .mapToInt(PointHistory::getAmount)
                    .sum();

            System.out.println("초기 잔액: " + initialBalance);
            System.out.println("최종 잔액: " + finalBalance);
            System.out.println("예상 잔액: " + (initialBalance - (useAmount * threadCount)));

            // 잔액이 기대했던 금액만큼 정확히 감소했는지 확인
            assertThat(finalBalance).isEqualTo(initialBalance - useAmount * threadCount);

            // 적어도 우리가 요청한 5개 이상의 차감 이력이 있는지 확인
            assertThat(newHistories.size()).isGreaterThanOrEqualTo(threadCount);

            // 차감 금액의 총합이 기대 금액과 일치하는지 확인
            assertThat(Math.abs(totalUsedAmount)).isEqualTo(useAmount * threadCount);

        } finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }
    
    @Test
    @DisplayName("비관적 락을 이용한 포인트 충전 동시성 테스트")
    void pessimisticLockPointChargeTest() throws InterruptedException {
        // given
        // 테스트 사용자 선택
        User user = userJpaRepository.findAll().get(0);
        Long userId = user.getUserId();
        int initialBalance = user.getPointBalance();
        int chargeAmount = 100;
        int threadCount = 10; // 동시 요청 수

        // 테스트 시작 시간 기록
        LocalDateTime testStartTime = LocalDateTime.now();
        System.out.println("테스트 시작 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance + ", 테스트 시작 시간: " + testStartTime);

        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // when
            // 충전 요청
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + " 충전 요청 시작");
                        userService.chargePointWithPessimisticLock(userId, chargeAmount);
                        System.out.println(Thread.currentThread().getName() + " 충전 요청 완료");
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 충전 중 오류: " + e.getMessage());
                    } finally {
                        latch.countDown(); // 작업이 완료되면 카운트 감소
                    }
                });
            }

            // 모든 작업이 완료될 때까지 대기 (최대 30초)
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            System.out.println("모든 충전 요청 완료 여부: " + completed);

            // then
            // 충전 후 데이터
            User updatedUser = userJpaRepository.findById(userId).orElseThrow();
            int finalBalance = updatedUser.getPointBalance();

            // 테스트 중 생성된 이력 조회 (testStartTime 이후 생성된 것만)
            List<PointHistory> newHistories = pointHistoryJpaRepository.findAll().stream()
                    .filter(history -> history.getUserId().equals(userId)
                            && history.getPointType().equals("CHARGE")
                            && history.getCreatedAt().isAfter(testStartTime))
                    .toList();

            // 총 충전 금액
            int totalChargeAmount = newHistories.stream()
                    .mapToInt(PointHistory::getAmount)
                    .sum();

            System.out.println("초기 잔액: " + initialBalance);
            System.out.println("최종 잔액: " + finalBalance);
            System.out.println("예상 잔액: " + (initialBalance + (chargeAmount * threadCount)));
            System.out.println("성공한 충전 요청 수: " + newHistories.size());

            // 비관적 락이 제대로 동작했는지 검증
            // 모든 요청이 성공해야 함
            assertThat(finalBalance).isEqualTo(initialBalance + chargeAmount * threadCount);
            assertThat(newHistories.size()).isEqualTo(threadCount);

            // 충전 금액 총합 검증
            assertThat(Math.abs(totalChargeAmount)).isEqualTo(chargeAmount * threadCount);
        } finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("비관적 락을 이용한 포인트 사용 동시성 테스트")
    void pessimisticLockPointUseTest() throws InterruptedException {
        // given
        // 충분한 포인트를 가진 사용자 선택
        User user = userJpaRepository.findAll().get(0);
        Long userId = user.getUserId();

        // 충분한 잔액이 있는지 확인하고, 필요하면 충전
        int requiredBalance = 1000; // 테스트에 필요한 최소 잔액
        if (user.getPointBalance() < requiredBalance) {
            userService.chargePoint(userId, requiredBalance - user.getPointBalance());
            user = userJpaRepository.findById(userId).orElseThrow();
        }

        int initialBalance = user.getPointBalance();
        int useAmount = 100;
        int threadCount = 10;

        // 테스트 시작 시간 기록
        LocalDateTime testStartTime = LocalDateTime.now();
        System.out.println("테스트 시작 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance + ", 테스트 시작 시간: " + testStartTime);

        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // when
            // 사용 요청
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + " 포인트 사용 요청 시작");
                        userService.usePointWithPessimisticLock(userId, useAmount);
                        System.out.println(Thread.currentThread().getName() + " 포인트 사용 요청 완료");
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 포인트 사용 중 오류: " + e.getMessage());
                    } finally {
                        latch.countDown(); // 작업이 완료되면 카운트 감소
                    }
                });
            }

            // 모든 작업이 완료될 때까지 대기 (최대 30초)
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            System.out.println("모든 포인트 사용 요청 완료 여부: " + completed);

            // then
            // 사용 후 데이터
            User updatedUser = userJpaRepository.findById(userId).orElseThrow();
            int finalBalance = updatedUser.getPointBalance();

            // 테스트 중 생성된 이력 조회 (testStartTime 이후 생성된 것만)
            List<PointHistory> newHistories = pointHistoryJpaRepository.findAll().stream()
                    .filter(history -> history.getUserId().equals(userId)
                            && history.getPointType().equals("USE")
                            && history.getCreatedAt().isAfter(testStartTime))
                    .toList();

            // 총 사용 금액
            int totalUsedAmount = newHistories.stream()
                    .mapToInt(PointHistory::getAmount)
                    .sum();

            System.out.println("초기 잔액: " + initialBalance);
            System.out.println("최종 잔액: " + finalBalance);
            System.out.println("예상 잔액: " + (initialBalance - (useAmount * threadCount)));
            System.out.println("성공한 사용 요청 수: " + newHistories.size());

            // 비관적 락이 제대로 동작했는지 검증
            // 모든 요청이 성공해야 함
            assertThat(finalBalance).isEqualTo(initialBalance - useAmount * threadCount);
            assertThat(newHistories.size()).isEqualTo(threadCount);

            // 사용 금액 총합 검증
            assertThat(Math.abs(totalUsedAmount)).isEqualTo(useAmount * threadCount);
        } finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }

//    @Test
//    @DisplayName("낙관적 락을 이용한 포인트 충전 동시성 테스트")
//    void optimisticLockPointChargeTest() throws InterruptedException {
//        // given
//        // 테스트 사용자 선택
//        User user = userJpaRepository.findAll().get(0);
//        Long userId = user.getUserId();
//        int initialBalance = user.getPointBalance();
//        int chargeAmount = 100;
//        int threadCount = 10; // 충돌 가능성을 높이기 위해 스레드 수 증가
//        int maxRetries = 10;   // 최대 재시도 횟수
//
//        // 테스트 시작 시간 기록
//        LocalDateTime testStartTime = LocalDateTime.now();
//        System.out.println("테스트 시작 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance + ", 테스트 시작 시간: " + testStartTime);
//
//        // 오류 발생 카운터
//        AtomicInteger errorCount = new AtomicInteger(0);
//
//        // 쓰레드 풀 생성
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        try {
//            // when
//            // 충전 요청
//            for (int i = 0; i < threadCount; i++) {
//                executor.submit(() -> {
//                    try {
//                        System.out.println(Thread.currentThread().getName() + " 충전 요청 시작");
//
//                        // 스레드 간 충돌 가능성을 높이기 위해 랜덤 지연 추가
//                        if (ThreadLocalRandom.current().nextBoolean()) {
//                            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
//                        }
//
//                        userService.chargePointWithOptimisticLock(userId, chargeAmount, maxRetries);
//                        System.out.println(Thread.currentThread().getName() + " 충전 요청 완료");
//                    } catch (Exception e) {
//                        System.out.println(Thread.currentThread().getName() + " 충전 중 오류: " + e.getMessage());
//                        errorCount.incrementAndGet();
//                    } finally {
//                        latch.countDown(); // 작업이 완료되면 카운트 감소
//                    }
//                });
//            }
//
//            // 모든 작업이 완료될 때까지 대기 (최대 30초)
//            boolean completed = latch.await(30, TimeUnit.SECONDS);
//            System.out.println("모든 충전 요청 완료 여부: " + completed + ", 오류 수: " + errorCount.get());
//
//            // then
//            // 충전 후 데이터
//            User updatedUser = userJpaRepository.findById(userId).orElseThrow();
//            int finalBalance = updatedUser.getPointBalance();
//
//            // 테스트 중 생성된 이력 조회 (testStartTime 이후 생성된 것만)
//            List<PointHistory> newHistories = pointHistoryJpaRepository.findAll().stream()
//                    .filter(history -> history.getUserId().equals(userId)
//                            && history.getPointType().equals("CHARGE")
//                            && history.getCreatedAt().isAfter(testStartTime))
//                    .toList();
//
//            // 총 충전 금액
//            int totalChargeAmount = newHistories.stream()
//                    .mapToInt(PointHistory::getAmount)
//                    .sum();
//
//            System.out.println("초기 잔액: " + initialBalance);
//            System.out.println("최종 잔액: " + finalBalance);
//            System.out.println("예상 잔액: " + (initialBalance + (chargeAmount * threadCount)));
//            System.out.println("성공한 충전 요청 수: " + newHistories.size());
//            System.out.println("실패한 충전 요청 수: " + errorCount.get());
//
//            // 낙관적 락이 제대로 동작했는지 검증
//            // 1. 오류가 발생하지 않았다면 모든 충전이 성공해야 함
//            if (errorCount.get() == 0) {
//                assertThat(finalBalance).isEqualTo(initialBalance + chargeAmount * threadCount);
//                assertThat(newHistories.size()).isEqualTo(threadCount);
//            }
//            // 2. 오류가 발생했다면 성공한 충전만큼만 잔액이 증가해야 함
//            else {
//                int successCount = threadCount - errorCount.get();
//                assertThat(finalBalance).isEqualTo(initialBalance + chargeAmount * successCount);
//                assertThat(newHistories.size()).isEqualTo(successCount);
//            }
//
//            // 3. 충전 금액 총합 검증
//            assertThat(Math.abs(totalChargeAmount)).isEqualTo(chargeAmount * newHistories.size());
//
//        } finally {
//            // 쓰레드 풀 종료
//            executor.shutdown();
//        }
//    }

//    @Test
//    @DisplayName("낙관적 락을 이용한 포인트 사용 동시성 테스트")
//    void optimisticLockPointUseTest() throws InterruptedException {
//        // given
//        // 충분한 포인트를 가진 사용자 선택
//        User user = userJpaRepository.findAll().get(0);
//        Long userId = user.getUserId();
//
//        // 충분한 잔액이 있는지 확인하고, 필요하면 충전
//        int requiredBalance = 1000; // 테스트에 필요한 최소 잔액
//        if (user.getPointBalance() < requiredBalance) {
//            userService.chargePoint(userId, requiredBalance - user.getPointBalance());
//            user = userJpaRepository.findById(userId).orElseThrow();
//        }
//
//        int initialBalance = user.getPointBalance();
//        int useAmount = 100;
//        int threadCount = 10;
//        int maxRetries = 5;
//
//        // 테스트 시작 시간 기록
//        LocalDateTime testStartTime = LocalDateTime.now();
//        System.out.println("테스트 시작 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance + ", 테스트 시작 시간: " + testStartTime);
//
//        // 오류 발생 카운터
//        AtomicInteger errorCount = new AtomicInteger(0);
//
//        // 쓰레드 풀 생성
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        try {
//            // when
//            // 사용 요청
//            for (int i = 0; i < threadCount; i++) {
//                executor.submit(() -> {
//                    try {
//                        System.out.println(Thread.currentThread().getName() + " 포인트 사용 요청 시작");
//
//                        // 스레드 간 충돌 가능성을 높이기 위해 랜덤 지연 추가
//                        if (ThreadLocalRandom.current().nextBoolean()) {
//                            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
//                        }
//
//                        userService.usePointWithOptimisticLock(userId, useAmount, maxRetries);
//                        System.out.println(Thread.currentThread().getName() + " 포인트 사용 요청 완료");
//                    } catch (Exception e) {
//                        System.out.println(Thread.currentThread().getName() + " 포인트 사용 중 오류: " + e.getMessage());
//                        errorCount.incrementAndGet();
//                    } finally {
//                        latch.countDown(); // 작업이 완료되면 카운트 감소
//                    }
//                });
//            }
//
//            // 모든 작업이 완료될 때까지 대기 (최대 30초)
//            boolean completed = latch.await(30, TimeUnit.SECONDS);
//            System.out.println("모든 포인트 사용 요청 완료 여부: " + completed + ", 오류 수: " + errorCount.get());
//
//            // then
//            // 사용 후 데이터
//            User updatedUser = userJpaRepository.findById(userId).orElseThrow();
//            int finalBalance = updatedUser.getPointBalance();
//
//            // 테스트 중 생성된 이력 조회 (testStartTime 이후 생성된 것만)
//            List<PointHistory> newHistories = pointHistoryJpaRepository.findAll().stream()
//                    .filter(history -> history.getUserId().equals(userId)
//                            && history.getPointType().equals("USE")
//                            && history.getCreatedAt().isAfter(testStartTime))
//                    .toList();
//
//            // 총 사용 금액
//            int totalUsedAmount = newHistories.stream()
//                    .mapToInt(PointHistory::getAmount)
//                    .sum();
//
//            System.out.println("초기 잔액: " + initialBalance);
//            System.out.println("최종 잔액: " + finalBalance);
//            System.out.println("예상 잔액: " + (initialBalance - (useAmount * (threadCount - errorCount.get()))));
//            System.out.println("성공한 사용 요청 수: " + newHistories.size());
//            System.out.println("실패한 사용 요청 수: " + errorCount.get());
//
//            // 낙관적 락이 제대로 동작했는지 검증
//            // 1. 성공한 사용 요청 수만큼 포인트가 차감되었는지 확인
//            int successCount = threadCount - errorCount.get();
//            assertThat(finalBalance).isEqualTo(initialBalance - useAmount * successCount);
//            assertThat(newHistories.size()).isEqualTo(successCount);
//
//            // 2. 사용 금액 총합 검증
//            assertThat(Math.abs(totalUsedAmount)).isEqualTo(useAmount * successCount);
//
//        } finally {
//            // 쓰레드 풀 종료
//            executor.shutdown();
//        }
//    }
}
