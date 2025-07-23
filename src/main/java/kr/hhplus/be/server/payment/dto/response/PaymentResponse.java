package kr.hhplus.be.server.payment.dto.response;

import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Integer paidAmount;
    private Integer pointUsedAmount;
    private Integer discountAmount;
    private Long couponId;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidDt;

    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paidAmount(payment.getPaidAmount())
                .pointUsedAmount(payment.getPointUsedAmount())
                .discountAmount(payment.getDiscountAmount())
                .couponId(payment.getCouponId())
                .paymentStatus(payment.getPaymentStatus())
                .paidDt(payment.getPaidDt())
                .build();
    }
}