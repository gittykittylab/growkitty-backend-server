package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.infrastructure.repository.CouponPolicyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponPolicyRepositoryImpl implements CouponPolicyRepository {
    private final CouponPolicyJpaRepository couponPolicyJpaRepository;
    @Override
    public Optional<CouponPolicy> findById(Long policyId) {
        return couponPolicyJpaRepository.findById(policyId);
    }

    @Override
    public Optional<CouponPolicy> findByIdWithLock(Long policyId) {
        return couponPolicyJpaRepository.findByIdWithLock(policyId);
    }

    @Override
    public List<CouponPolicy> findAll() {
        return couponPolicyJpaRepository.findAll();
    }
}
