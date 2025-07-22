package kr.hhplus.be.server.common.exception;

public class InsufficientBalanceException extends RuntimeException{
    // 기본 생성자
    public InsufficientBalanceException() {
        super("잔액이 부족합니다");
    }

    // 메시지를 받는 생성자
    public InsufficientBalanceException(String message) {
        super(message);
    }

    // 메시지와 원인 예외를 받는 생성자
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
