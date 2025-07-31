package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.PaymentException;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "paid_amount", nullable = false)
    private Integer paidAmount;

    @Column(name = "applied_discount_amount")
    private Integer appliedDiscountAmount;

    @Column(name = "point_used_amount", nullable = false)
    private Integer pointUsedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

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

    // 결제 성공 객체 생성
    public static Payment createSuccessedPayment(Long orderId, Long userId, Integer paidAmount,
                                        Integer pointUsedAmount, Integer appliedDiscountAmount,
                                        Long couponId) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setPaidAmount(paidAmount);
        payment.setPointUsedAmount(pointUsedAmount);
        payment.setAppliedDiscountAmount(appliedDiscountAmount);
        payment.setCouponId(couponId);
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        return payment;
    }

    // 결제 실패 객체 생성
    public static Payment createFailedPayment(Long orderId, Long userId, Integer paidAmount) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setPaidAmount(paidAmount);
        payment.setPointUsedAmount(0); // 실패 시 포인트는 사용되지 않음
        payment.setAppliedDiscountAmount(0);
        payment.setCouponId(null);
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaidAt(LocalDateTime.now());

        return payment;
    }
}