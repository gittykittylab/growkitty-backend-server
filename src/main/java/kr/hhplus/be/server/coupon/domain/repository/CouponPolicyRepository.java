package kr.hhplus.be.server.coupon.domain.repository;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;

import java.util.List;
import java.util.Optional;

public interface CouponPolicyRepository {
    Optional<CouponPolicy> findById(Long policyId);

    // 비관적 락이 적용된 메서드 추가
    Optional<CouponPolicy> findByIdWithLock(Long policyId);

    List<CouponPolicy> findAll();
}
