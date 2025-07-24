package kr.hhplus.be.server.coupon.presentation;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;

import kr.hhplus.be.server.coupon.dto.reponse.CouponIssueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 선착순 쿠폰 발급 API
     */
    @PostMapping("/issue/{policyId}")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
            @PathVariable Long policyId,
            @RequestHeader("X-USER-ID") Long userId) {

        try {
            // 서비스 호출 (도메인 엔티티 반환)
            Coupon coupon = couponService.issueFirstComeCoupon(policyId, userId);

            // 컨트롤러에서 DTO로 변환
            return ResponseEntity.ok(CouponIssueResponse.success(coupon.getCouponId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CouponIssueResponse.fail(e.getMessage()));
        }
    }
}