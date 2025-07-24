package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.infrastructure.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Long orderId;
    private Long userId;
    private int totalAmount;
    private int pointAmount;

    @BeforeEach
    void setUp() {
        orderId = 1L;
        userId = 100L;
        totalAmount = 10000;
        pointAmount = 2000;
    }

    @Test
    @DisplayName("결제 처리 성공 테스트")
    void processPayment_Success() {
        // Given
        Payment mockPayment = Payment.createSuccessedPayment(orderId, userId, totalAmount, pointAmount, 0, null);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        Payment result = paymentService.processPayment(orderId, userId, totalAmount, pointAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPaidAmount()).isEqualTo(totalAmount);
        assertThat(result.getPointUsedAmount()).isEqualTo(pointAmount);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
