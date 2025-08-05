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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, UserTestDataLoader.class})
public class UserServiceConcurrencyTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;
    
    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    @Test
    @DisplayName("사용자가 두번의 포인트 충전 시 동시성 문제 발생 테스트")
        // 사용자가 두 번 포인트 충전 버튼을 눌렀을 때 두 번 모두 충전되어야 함
    void concurrentPointChargeTest() throws InterruptedException, ExecutionException {
        // given
        // 충분한 포인트를 가진 사용자 선택(5000)
        User user = userJpaRepository.findAll().get(1);
        Long userId = user.getUserId();
        int initialBalance = user.getPointBalance();
        int chargeAmount = 100;

        // 테스트 시작 시간 기록
        LocalDateTime testStartTime = LocalDateTime.now();
        System.out.println("테스트 시작 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance + ", 테스트 시작 시간: " + testStartTime);

        // 두 개의 쓰레드를 가진 풀 생성 (두 요청을 동시에 처리하기 위함)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // when
            // 첫번째 충전 요청 (첫 번째 쓰레드에서 실행)
            CompletableFuture<Void> firstCharge = CompletableFuture.runAsync(() ->{
                System.out.println("첫 번째 충전 요청 시작 - Thread: " + Thread.currentThread().getName());
                userService.chargePoint(userId, chargeAmount);
                System.out.println("첫 번째 충전 요청 완료");
            }, executor);

            // 두 번째 충전 요청 (두 번째 쓰레드에서 실행)
            CompletableFuture<Void> secondCharge = CompletableFuture.runAsync(() -> {
                System.out.println("두 번째 충전 요청 시작 - Thread: " + Thread.currentThread().getName());
                userService.chargePoint(userId, chargeAmount);
                System.out.println("두 번째 충전 요청 완료");
            }, executor);

            // 두 작업이 모두 완료될 때까지 대기
            CompletableFuture.allOf(firstCharge, secondCharge).get();
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
            int totalChargedAmount = newHistories.stream()
                    .mapToInt(PointHistory::getAmount)
                    .sum();

            System.out.println("초기 잔액: " + initialBalance);
            System.out.println("최종 잔액: " + finalBalance);
            System.out.println("예상 잔액: " + (initialBalance + (chargeAmount * 2)));

            // 잔액이 기대했던 금액만큼(200) 정확히 증가했는지 확인
            assertThat(finalBalance).isEqualTo(initialBalance + chargeAmount * 2);

            // 적어도 우리가 요청한 2개이상의 충전 이력이 있는지 확인
            assertThat(newHistories.size()).isGreaterThanOrEqualTo(2);

        }finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }



    }
}
