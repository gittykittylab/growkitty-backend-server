package kr.hhplus.be.server.payment.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import kr.hhplus.be.server.user.application.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class PaymentFacadeTest {
    @Mock
    private PaymentService paymentService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PaymentFacade paymentFacade;

    private Long orderId;
    private Long userId;
    private int totalAmount;
    private int usedPoints;

    @BeforeEach
    void setUp() {
        // 테스트에 공통으로 사용되는 값 설정
        orderId = 1L;
        userId = 100L;
        totalAmount = 10000;
        usedPoints = 2000; // 기본 포인트 사용량
    }


    @Test
    @DisplayName("결제 처리 성공")
    void processPayment_WithPoints_Success() {
        // given
        int usedPoints = 2000;

        // when
        paymentFacade.processPayment(orderId, userId, totalAmount, usedPoints);

        // then
        verify(userService).usePoint(userId, usedPoints);
        verify(paymentService).processPayment(orderId, userId, totalAmount, usedPoints);
    }
}
