package kr.hhplus.be.server.user.domain.dto.response;

public class PointBalanceResponse {
    private final int pointBalance;

    public PointBalanceResponse(int pointBalance) {
        this.pointBalance = pointBalance;
    }

    public int getPointBalance() { return pointBalance; }
}
