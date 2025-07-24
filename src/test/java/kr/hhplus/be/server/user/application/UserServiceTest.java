package kr.hhplus.be.server.user.application;


import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.common.exception.InsufficientBalanceException;
import kr.hhplus.be.server.user.domain.PointHistory;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.PointHistoryRepository;
import kr.hhplus.be.server.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = new User();
        testUser.setUserId(userId);
        testUser.setPointBalance(1000);
        testUser.setUserGrade("NORMAL");
        // 기본 사용자는 존재한다고 가정
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("포인트 충전 성공 테스트")
    void chargePoint_Success() {
        // given
        int chargeAmount = 500;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // when
        userService.chargePoint(userId, chargeAmount);

        // then
        assertEquals(1500, testUser.getPointBalance());

        // 이력 저장 검증
        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());

        PointHistory savedHistory = historyCaptor.getValue();
        assertEquals(userId, savedHistory.getUserId());
        assertEquals(chargeAmount, savedHistory.getAmount());
        assertEquals("CHARGE", savedHistory.getPointType());
        assertNotNull(savedHistory.getCreatedAt());
    }

    @Test
    @DisplayName("잘못된 충전 금액(0 이하)으로 충전 시 예외 발생")
    void chargePoint_InvalidAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.chargePoint(userId, 0)
        );

        assertTrue(exception.getMessage().contains("충전 금액은 0보다 커야 합니다"));
        //이력 저장 호출 안 됨
        verify(pointHistoryRepository, never()).save(any());
    }
}
