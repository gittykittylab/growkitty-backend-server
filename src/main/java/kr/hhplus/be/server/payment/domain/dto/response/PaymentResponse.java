package kr.hhplus.be.server.payment.domain.dto.response;

import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Integer paidAmount;           // amount -> paidAmount로 변경
    private Integer pointUsedAmount;      // pointAmount -> pointUsedAmount로 변경
    private Integer appliedDiscountAmount; // 할인 금액 추가
    private Long couponId;                // 쿠폰 ID 추가
    private PaymentStatus paymentStatus;  // status -> paymentStatus로 변경
    private LocalDateTime paidAt;         // 결제 시간 추가 (createdAt/updatedAt 대신)

    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paidAmount(payment.getPaidAmount())
                .pointUsedAmount(payment.getPointUsedAmount())
                .appliedDiscountAmount(payment.getAppliedDiscountAmount())
                .couponId(payment.getCouponId())
                .paymentStatus(payment.getPaymentStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }
}