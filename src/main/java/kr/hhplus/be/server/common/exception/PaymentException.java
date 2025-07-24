package kr.hhplus.be.server.common.exception;

public class PaymentException extends RuntimeException{
    // 기본
    public PaymentException() {
        super("결제 처리 중 오류가 발생했습니다.");
    }

    // 메시지
    public PaymentException(String message) {
        super(message);
    }

    // 예외 포함
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

}
