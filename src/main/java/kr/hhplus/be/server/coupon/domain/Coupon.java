package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "discount_rate")
    private Integer discountRate;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @Column(name = "expired_dt")
    private LocalDateTime expiredDt;

    @Column(name = "coupon_status", nullable = false)
    private String couponStatus;

    @Column(name = "created_dt")
    private LocalDateTime createdDt;

    @Column(name = "updated_dt")
    private LocalDateTime updatedDt;

    // 도메인 메서드: 쿠폰 생성
    public static Coupon createFromPolicy(CouponPolicy policy, Long userId) {
        Coupon coupon = new Coupon();
        coupon.setUserId(userId);
        coupon.setPolicyId(policy.getPolicyId());
        coupon.setDiscountRate(policy.getDiscountRate());
        coupon.setDiscountAmount(policy.getDiscountAmount());
        coupon.setExpiredDt(policy.calculateExpiryDate());
        coupon.setCouponStatus("AVAILABLE");
        coupon.setCreatedDt(LocalDateTime.now());
        coupon.setUpdatedDt(LocalDateTime.now());
        return coupon;
    }

    // 도메인 메서드: 쿠폰 상태 복원
    public void restore() {
        this.couponStatus = "AVAILABLE";
        this.updatedDt = LocalDateTime.now();
    }
}