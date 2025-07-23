package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
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
}