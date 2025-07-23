package kr.hhplus.be.server.user;

public class PointBalanceResponse {
    private final int pointBalance;

    public PointBalanceResponse(int pointBalance) {
        this.pointBalance = pointBalance;
    }

    public int getPointBalance() { return pointBalance; }
}
