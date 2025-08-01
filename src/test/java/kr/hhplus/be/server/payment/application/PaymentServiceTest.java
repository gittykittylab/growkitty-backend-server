package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.common.exception.PaymentException;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.domain.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Test
    @DisplayName("결제 처리 실패 시 예외 발생 테스트")
    void processPayment_ThrowsException() {
        // Given
        when(paymentRepository.save(any(Payment.class))).thenThrow(new RuntimeException("DB 오류"));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(orderId, userId, totalAmount, pointAmount);
        });

        assertThat(exception.getMessage()).contains("결제 처리 실패");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 실패 정보 저장 테스트")
    void saveFailedPayment_Success() {
        // Given
        Payment expectedPayment = Payment.createFailedPayment(orderId, userId, totalAmount);
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);

        // When
        Payment result = paymentService.saveFailedPayment(orderId, userId, totalAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 실패 정보 저장 실패 시 예외 발생")
    void saveFailedPayment_ThrowsException() {
        // Given
        when(paymentRepository.save(any(Payment.class))).thenThrow(new RuntimeException("DB 오류"));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.saveFailedPayment(orderId, userId, totalAmount);
        });

        assertThat(exception.getMessage()).contains("결제 실패 정보 저장 실패");
        verify(paymentRepository).save(any(Payment.class));
    }
}
