package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class BalanceRequest {
    @Schema(description = "유저 ID", example = "1")
    private Long userId;

    @Schema(description = "충전 금액", example = "1000")
    private Long amount;

    // 생성자, getter, setter
    public BalanceRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}
