package kr.hhplus.be.server.coupon.infrastructure.repository;

import jakarta.annotation.Nonnull;
import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicy, Long> {
    // 비관적 락이 적용된 새로운 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cp FROM CouponPolicy cp WHERE cp.policyId = :id")
    Optional<CouponPolicy> findByIdWithLock(@Param("id") Long id);
}
