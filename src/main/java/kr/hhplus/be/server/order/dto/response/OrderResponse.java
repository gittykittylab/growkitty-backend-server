package kr.hhplus.be.server.order.dto.response;

import kr.hhplus.be.server.order.domain.Order;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class OrderResponse {
    // 주문 ID
    private Long id;

    // 주문자 ID
    private Long userId;

    // 주문 금액 정보
    private Integer totalAmount;
    private Integer totalDiscountAmount;

    // 주문 상태
    private String orderStatus;

    // 주문 유형
    private String orderType;

    // 주문 일시
    private LocalDateTime orderedDt;

    // 주문 상품 목록
    private List<OrderItemResponse> orderItems;

    // 결제 정보
//    private PaymentResponse payment;

    // Order 엔티티로부터 DTO 생성하는 생성자
    public OrderResponse(Order order) {
        this.id = order.getId();
        this.userId = order.getUserId();
        this.totalAmount = order.getTotalAmount();
        this.totalDiscountAmount = order.getTotalDiscountAmount();
        this.orderStatus = order.getOrderStatus();
        this.orderType = order.getOrderType();
        this.orderedDt = order.getOrderedDt();

        // 주문 항목 변환
        if (order.getOrderItems() != null) {
            this.orderItems = order.getOrderItems().stream()
                    .map(item -> new OrderItemResponse(item))
                    .collect(Collectors.toList());
        }
    }
}