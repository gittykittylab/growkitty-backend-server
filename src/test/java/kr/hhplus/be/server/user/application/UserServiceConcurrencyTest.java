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

    @Test
    @DisplayName("사용자가 여러번 포인트 충전 시 동시성 문제 발생 테스트")
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
    @DisplayName("사용자가 여러번 포인트 차감 시 동시성 문제 발생 테스트")
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

}
