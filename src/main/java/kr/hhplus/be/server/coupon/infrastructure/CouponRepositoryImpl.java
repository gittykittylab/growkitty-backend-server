package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.coupon.infrastructure.repository.CouponJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository couponJpaRepository;

    @Override
    public long countByPolicyId(Long policyId) {
        return couponJpaRepository.countByPolicyId(policyId);
    }

    @Override
    public boolean existsByUserIdAndPolicyId(Long userId, Long policyId) {
        return couponJpaRepository.existsByUserIdAndPolicyId(userId, policyId);
    }

    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }
}
