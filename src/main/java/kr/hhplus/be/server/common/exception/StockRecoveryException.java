package kr.hhplus.be.server.common.exception;

public class StockRecoveryException extends RuntimeException {
    private final Long productId;
    // 기본 생성자
    public StockRecoveryException() {
        super("재고 복구에 실패했습니다");
        this.productId = null;
    }

    // 상품 ID만 받는 생성자
    public StockRecoveryException(Long productId) {
        super("상품 ID " + productId + "의 재고 복구에 실패했습니다");
        this.productId = productId;
    }

    // 상품 ID와 메시지를 받는 생성자
    public StockRecoveryException(Long productId, String message) {
        super(message);
        this.productId = productId;
    }

    // 메시지만 받는 생성자
    public StockRecoveryException(String message) {
        super(message);
        this.productId = null;
    }
}
