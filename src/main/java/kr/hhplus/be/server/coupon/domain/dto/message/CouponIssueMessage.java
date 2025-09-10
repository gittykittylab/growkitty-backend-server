package kr.hhplus.be.server.coupon.domain.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueMessage {
    private Long policyId;
    private Long userId;
}