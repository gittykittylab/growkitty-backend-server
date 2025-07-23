package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.PaymentException;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_payments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "paid_amount", nullable = false)
    private Integer paidAmount;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "point_used_amount", nullable = false)
    private Integer pointUsedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "paid_dt", nullable = false)
    private LocalDateTime paidDt = LocalDateTime.now();

    // 결제 취소 메서드
    public void cancel() {
        if (this.paymentStatus != PaymentStatus.PAID) {
            throw new PaymentException("이미 취소되었거나 실패한 결제입니다.");
        }
        this.paymentStatus = PaymentStatus.CANCELLED;
    }

    // 결제 실패 처리 메서드
    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    // 결제 객체 생성
    public static Payment createPayment(Long orderId, Long userId, Integer paidAmount,
                                        Integer pointUsedAmount, Integer discountAmount,
                                        Long couponId) {
        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .paidAmount(paidAmount)
                .pointUsedAmount(pointUsedAmount)
                .discountAmount(discountAmount)
                .couponId(couponId)
                .paymentStatus(PaymentStatus.PAID)
                .paidDt(LocalDateTime.now())
                .build();
    }
}