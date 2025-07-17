package kr.hhplus.be.server.dto;

public record OrderResponse(
        Long orderId,
        String status,     // 예: "결제 성공", "잔액 부족", "쿠폰 무효", ...
        int finalAmount
) {}
