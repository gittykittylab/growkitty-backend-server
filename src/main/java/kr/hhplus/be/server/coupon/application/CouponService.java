package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.infrastructure.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.infrastructure.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRepository couponRepository;

    /**
     * 선착순 쿠폰 발급 기능
     * @return 생성된 쿠폰 엔티티
     */
    @Transactional
    public Coupon issueFirstComeCoupon(Long policyId, Long userId) {
        // 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("쿠폰 정책을 찾을 수 없습니다."));

        // 발급 가능 수량 확인 (도메인 메서드 사용)
        long issuedCount = couponRepository.countByPolicyId(policyId);
        if (!policy.isAvailableForIssue(issuedCount)) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }

        // 중복 발급 확인
        boolean alreadyIssued = couponRepository.existsByUserIdAndPolicyId(userId, policyId);
        if (alreadyIssued) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        // 쿠폰 생성 (도메인 메서드 사용)
        Coupon coupon = Coupon.createFromPolicy(policy, userId);
        return couponRepository.save(coupon);
    }
}