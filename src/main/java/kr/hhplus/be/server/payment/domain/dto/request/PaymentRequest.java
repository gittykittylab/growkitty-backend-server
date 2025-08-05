package kr.hhplus.be.server.payment.domain.dto.request;

import kr.hhplus.be.server.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Long orderId;
    private Long userId;
    private Integer amount;        // 최종 결제 금액
    private Integer pointAmount;   // 포인트 사용 금액
    private Integer discountAmount; // 할인 금액
    private Long couponId;         // 사용한 쿠폰 ID (있는 경우)
    private PaymentStatus paymentStatus;
}