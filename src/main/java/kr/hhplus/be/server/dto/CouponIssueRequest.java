package kr.hhplus.be.server.dto;

public record CouponIssueRequest(
        Long userId,
        String couponCode
) {}
