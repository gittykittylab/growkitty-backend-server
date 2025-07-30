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

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "coupon_status", nullable = false)
    private String couponStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 도메인 메서드: 쿠폰 생성
    public static Coupon createFromPolicy(CouponPolicy policy, Long userId) {
        Coupon coupon = new Coupon();
        coupon.setUserId(userId);
        coupon.setPolicyId(policy.getPolicyId());
        coupon.setExpiredAt(policy.calculateExpiryDate());
        coupon.setCouponStatus("AVAILABLE");
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        return coupon;
    }

    // 도메인 메서드: 쿠폰 상태 복원
    public void restore() {
        this.couponStatus = "AVAILABLE";
        this.updatedAt = LocalDateTime.now();
    }
}