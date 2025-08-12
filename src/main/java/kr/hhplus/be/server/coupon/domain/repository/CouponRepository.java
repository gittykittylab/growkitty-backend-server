package kr.hhplus.be.server.coupon.domain.repository;

import kr.hhplus.be.server.coupon.domain.Coupon;

public interface CouponRepository {
    long countByPolicyId(Long policyId);

    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    long countByUserIdAndPolicyId(Long userId, Long policyId);

    Coupon save(Coupon coupon);

    // 비관적 락이 적용된 메서드 추가
    long countByPolicyIdWithLock(Long policyId);

    boolean existsByUserIdAndPolicyIdWithLock(Long userId, Long policyId);
}
