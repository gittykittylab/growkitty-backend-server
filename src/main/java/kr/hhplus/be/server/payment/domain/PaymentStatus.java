package kr.hhplus.be.server.payment.domain;

public enum PaymentStatus {
    PAID,       // 결제 완료
    FAILED,     // 결제 실패
    CANCELLED   // 결제 취소
}