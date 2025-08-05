package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_policies")
@Getter @Setter
public class CouponPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @Column(name = "expired_days")
    private Integer expiredDays;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 도메인 메서드: 쿠폰 발급 가능 여부 확인
    public boolean isAvailableForIssue(long issuedCount) {
        return issuedCount < this.totalQuantity;
    }

    // 도메인 메서드: 만료일 계산
    public LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusDays(this.expiredDays);
    }
}