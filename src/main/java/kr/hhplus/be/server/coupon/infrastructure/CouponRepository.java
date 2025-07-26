package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    //해당 정책으로 발급된 쿠폰 수
    long countByPolicyId(Long policyId);
    //유저의 쿠폰 보유 여부(해당 정책으로 발급된 쿠폰)
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);
}
