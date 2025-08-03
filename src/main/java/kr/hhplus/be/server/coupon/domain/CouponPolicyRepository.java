package kr.hhplus.be.server.coupon.domain;

import java.util.Optional;

public interface CouponPolicyRepository {
    Optional<CouponPolicy> findById(Long policyId);
}
