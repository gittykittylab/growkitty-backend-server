package kr.hhplus.be.server.coupon.domain.repository;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;

import java.util.Optional;

public interface CouponPolicyRepository {
    Optional<CouponPolicy> findById(Long policyId);
}
