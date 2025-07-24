package kr.hhplus.be.server.coupon.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponIssueResponse {
    private boolean success;
    private Long couponId;
    private String message;

    public static CouponIssueResponse success(Long couponId) {
        return new CouponIssueResponse(true, couponId, "쿠폰이 성공적으로 발급되었습니다.");
    }

    public static CouponIssueResponse fail(String message) {
        return new CouponIssueResponse(false, null, message);
    }
}