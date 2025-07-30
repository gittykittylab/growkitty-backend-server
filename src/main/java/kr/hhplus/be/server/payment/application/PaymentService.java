package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.common.exception.PaymentException;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    //결제 정보 저장 (이미 포인트가 차감된 상태)
    @Transactional
    public Payment processPayment(Long orderId, Long userId, int totalAmount, int pointAmount) {
        try {
            // Payment 엔티티의 정적 팩토리 메서드를 사용하여 결제 객체 생성
            Payment payment = Payment.createSuccessedPayment(
                    orderId,
                    userId,
                    totalAmount,
                    pointAmount,
                    0, // 할인 금액
                    null // 쿠폰 ID
            );

            return paymentRepository.save(payment);

        } catch (Exception e) {
            throw new PaymentException("결제 처리 실패: " + e.getMessage());
        }
    }
    // 결제 실패 정보 저장
    @Transactional
    public Payment saveFailedPayment(Long orderId, Long userId, int totalAmount) {
        try {
            // 실패한 결제 객체 생성
            Payment payment = Payment.createFailedPayment(
                    orderId,
                    userId,
                    totalAmount
            );

            return paymentRepository.save(payment);

        } catch (Exception e) {
            throw new PaymentException("결제 실패 정보 저장 실패: " + e.getMessage());
        }
    }
}
