package kr.hhplus.be.server.dto;

public record CouponIssueResponse(
        Long userId,
        String couponCode,
        String message
) {}