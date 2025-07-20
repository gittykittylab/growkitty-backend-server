package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderRequest(
        @Schema(description = "유저 ID", example = "1")
        Long userId,

        @Schema(description = "상품 ID", example = "1001")
        Long productId,

        @Schema(description = "주문 수량", example = "2")
        int quantity,

        @Schema(description = "총 결제 금액", example = "9000")
        long totalAmount,

        @Schema(description = "쿠폰 코드", example = "DISCOUNT50")
        String couponCode
) {}
