package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponPolicyRepositoryImpl implements CouponPolicyRepository {
    private final CouponPolicyJpaRepository couponPolicyJpaRepository;
    @Override
    public Optional<CouponPolicy> findById(Long policyId) {
        return couponPolicyJpaRepository.findById(policyId);
    }
}
