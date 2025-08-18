package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.domain.dto.response.PaymentResponse;
import kr.hhplus.be.server.user.application.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentService paymentService;
    private final UserService userService;

    // 결제 프로세스 처리
    @Transactional
    public void processPayment(Long orderId, Long userId, int totalAmount, int usedPoints) {
        // 포인트 차감 (있는 경우)
        if (usedPoints > 0) {
            userService.usePoint(userId, usedPoints);
        }

        // 결제 정보 저장
        paymentService.processPayment(
                orderId,
                userId,
                totalAmount,
                usedPoints
        );

        log.info("결제 처리 완료: 주문 ID={}, 사용자 ID={}, 총액={}, 사용 포인트={}",
                orderId, userId, totalAmount, usedPoints);
    }

    // 결제 실패 처리
    @Transactional
    public void handlePaymentFailure(Long orderId, Long userId, int totalAmount) {
        try {
            // 결제 실패 정보 저장
            paymentService.saveFailedPayment(orderId, userId, totalAmount);
            log.info("결제 실패 정보 저장 완료: 주문 ID={}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류 발생: 주문 ID={}, 오류={}", orderId, e.getMessage(), e);
        }
    }

    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentService.findById(paymentId);
        return PaymentResponse.fromEntity(payment); // 엔티티를 DTO로 변환
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return PaymentResponse.fromEntity(payment); // 엔티티를 DTO로 변환
    }

    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {
    }
}