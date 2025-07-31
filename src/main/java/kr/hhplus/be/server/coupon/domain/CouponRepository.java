package kr.hhplus.be.server.coupon.domain;

public interface CouponRepository {
    long countByPolicyId(Long policyId);

    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    Coupon save(Coupon coupon);
}
