package kr.hhplus.be.server.coupon.infrastructure.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
     long countByPolicyId(Long policyId);

     boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    long countByUserIdAndPolicyId(Long userId, Long policyId);

    // 비관적 락이 적용된 새로운 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.policyId = :policyId")
    long countByPolicyIdWithLock(@Param("policyId") Long policyId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Coupon c WHERE c.userId = :userId AND c.policyId = :policyId")
    boolean existsByUserIdAndPolicyIdWithLock(@Param("userId") Long userId, @Param("policyId") Long policyId);
}
