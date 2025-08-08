package kr.hhplus.be.server.user.application;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.common.exception.InsufficientBalanceException;
import kr.hhplus.be.server.testdata.UserTestDataLoader;
import kr.hhplus.be.server.user.domain.PointHistory;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.dto.response.PointBalanceResponse;
import kr.hhplus.be.server.user.infrastructure.repository.PointHistoryJpaRepository;
import kr.hhplus.be.server.user.infrastructure.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, UserTestDataLoader.class})
@ActiveProfiles("user-test")
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    @Test
    @DisplayName("사용자 포인트 잔액 조회")
    void getPointBalance() {
        // given
        User user = userJpaRepository.findAll().get(0);
        Long userId = user.getUserId();
        int expectedBalance = user.getPointBalance();

        // when
        PointBalanceResponse response = userService.getPointBalance(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPointBalance()).isEqualTo(expectedBalance);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 조회 시 예외 발생")
    void getPointBalance_UserNotFound() {
        // given
        Long nonExistentUserId = 9999L;

        // when & then
        assertThatThrownBy(() -> userService.getPointBalance(nonExistentUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint() {
        // given
        User user = userJpaRepository.findAll().get(1);
        Long userId = user.getUserId();
        int initialBalance = user.getPointBalance();
        int chargeAmount = 2000;

        // when
        userService.chargePoint(userId, chargeAmount);

        // then
        // 사용자 잔액 확인
        User updatedUser = userJpaRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getPointBalance()).isEqualTo(initialBalance + chargeAmount);

        // 포인트 이력 확인
        List<PointHistory> histories = pointHistoryJpaRepository.findAll().stream()
                .filter(history -> history.getUserId().equals(userId))
                .toList();

        assertThat(histories).isNotEmpty();
        PointHistory lastHistory = histories.get(histories.size() - 1);
        assertThat(lastHistory.getAmount()).isEqualTo(chargeAmount);
        assertThat(lastHistory.getPointType()).isEqualTo("CHARGE");
    }

    @Test
    @DisplayName("음수 금액 충전 시 예외 발생")
    void chargePoint_NegativeAmount() {
        // given
        User user = userJpaRepository.findAll().get(0);
        Long userId = user.getUserId();
        int negativeAmount = -1000;

        // when & then
        assertThatThrownBy(() -> userService.chargePoint(userId, negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("최대 한도 초과 충전 시 예외 발생")
    void chargePoint_ExceedMaxLimit() {
        // given
        User user = userJpaRepository.findAll().get(3); // 최대 한도 테스트용 사용자 (999,500)
        Long userId = user.getUserId();
        int chargeAmount = 1000; // 한도 초과 (999,500 + 1,000 > 1,000,000)

        // when & then
        assertThatThrownBy(() -> userService.chargePoint(userId, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 포인트 한도");
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint() {
        // given
        User user = userJpaRepository.findAll().get(1); // 5000 포인트 보유
        Long userId = user.getUserId();
        int initialBalance = user.getPointBalance();
        int useAmount = 2000;

        // when
        userService.usePoint(userId, useAmount);

        // then
        // 사용자 잔액 확인
        User updatedUser = userJpaRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getPointBalance()).isEqualTo(initialBalance - useAmount);

        // 포인트 이력 확인
        List<PointHistory> histories = pointHistoryJpaRepository.findAll().stream()
                .filter(history -> history.getUserId().equals(userId))
                .toList();

        assertThat(histories).isNotEmpty();
        PointHistory lastHistory = histories.get(histories.size() - 1);
        assertThat(lastHistory.getAmount()).isEqualTo(-useAmount); // 음수로 저장됨
        assertThat(lastHistory.getPointType()).isEqualTo("USE");
    }

    @Test
    @DisplayName("포인트 부족 시 사용 실패")
    void usePoint_InsufficientBalance() {
        // given
        User user = userJpaRepository.findAll().get(2); // 100 포인트 보유
        Long userId = user.getUserId();
        int useAmount = 500; // 보유량 초과

        // when & then
        assertThatThrownBy(() -> userService.usePoint(userId, useAmount))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("포인트가 부족합니다");
    }

    @Test
    @DisplayName("음수 금액 사용 시 예외 발생")
    void usePoint_NegativeAmount() {
        // given
        User user = userJpaRepository.findAll().get(0);
        Long userId = user.getUserId();
        int negativeAmount = -1000;

        // when & then
        assertThatThrownBy(() -> userService.usePoint(userId, negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("0 포인트 사용자가 포인트 사용 시 예외 발생")
    void usePoint_ZeroBalance() {
        // given
        User user = userJpaRepository.findAll().get(4); // 0 포인트 보유
        Long userId = user.getUserId();
        int useAmount = 100;

        // when & then
        assertThatThrownBy(() -> userService.usePoint(userId, useAmount))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("포인트가 부족합니다");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 충전 시 예외 발생")
    void chargePoint_UserNotFound() {
        // given
        Long nonExistentUserId = 9999L;
        int chargeAmount = 1000;

        // when & then
        assertThatThrownBy(() -> userService.chargePoint(nonExistentUserId, chargeAmount))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 사용 시 예외 발생")
    void usePoint_UserNotFound() {
        // given
        Long nonExistentUserId = 9999L;
        int useAmount = 1000;

        // when & then
        assertThatThrownBy(() -> userService.usePoint(nonExistentUserId, useAmount))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}