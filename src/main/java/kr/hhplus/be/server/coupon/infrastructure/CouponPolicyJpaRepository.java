package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicy, Long> {
}
