package kr.hhplus.be.server.dto;

public record OrderRequest(
        Long userId,
        String couponCode,  // optional (null 가능)
        int orderAmount
) {}
