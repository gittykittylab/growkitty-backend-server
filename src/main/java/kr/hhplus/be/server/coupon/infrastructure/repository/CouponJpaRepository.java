package kr.hhplus.be.server.coupon.infrastructure.repository;

import kr.hhplus.be.server.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    long countByPolicyId(Long policyId);

    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    long countByUserIdAndPolicyId(Long userId, Long policyId);
}
