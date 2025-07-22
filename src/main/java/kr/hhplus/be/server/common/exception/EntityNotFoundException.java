package kr.hhplus.be.server.common.exception;

public class EntityNotFoundException extends RuntimeException {
    // 기본 생성자
    public EntityNotFoundException() {
        super("상품을 찾을 수 없습니다.");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }

    // 메시지와 원인 예외를 받는 생성자
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
