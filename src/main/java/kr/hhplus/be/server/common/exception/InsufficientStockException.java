package kr.hhplus.be.server.common.exception;

public class InsufficientStockException extends RuntimeException {

    // 기본 생성자
    public InsufficientStockException() {
        super("재고가 부족합니다");
    }

    // 메시지를 받는 생성자
    public InsufficientStockException(String message) {
        super(message);
    }

    // 메시지와 원인 예외를 받는 생성자
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}