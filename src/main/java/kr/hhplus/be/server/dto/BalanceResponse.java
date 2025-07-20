package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class BalanceResponse {
    @Schema(description = "유저 ID", example = "1")
    private Long userId;

    @Schema(description = "현재 잔액", example = "5000")
    private Long balance;

    @Schema(description = "처리 결과 메시지", example = "충전 성공")
    private String message;

    public BalanceResponse(Long userId, Long balance, String message) {
        this.userId = userId;
        this.balance = balance;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public Long getBalance() { return balance; }
    public String getMessage() { return message; }
}
