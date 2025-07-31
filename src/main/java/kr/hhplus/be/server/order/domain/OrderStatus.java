package kr.hhplus.be.server.order.domain;

public enum OrderStatus {
    PENDING,     // 주문 접수
    PAID,        // 결제 완료
    PREPARING,   // 상품 준비 중
    SHIPPING,    // 배송 중
    DELIVERED,   // 배송 완료
    CANCELED     // 주문 취소
}