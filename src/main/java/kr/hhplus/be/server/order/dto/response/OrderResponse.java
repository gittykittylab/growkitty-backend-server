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
    private Long orderId;

    // 주문자 ID
    private Long userId;

    // 쿠폰 ID
    private Long couponId;

    // 주문 금액 정보
    private Integer totalAmount;
    private Integer couponDiscountAmount;

    // 최종 결제 금액
    private Integer finalAmount;

    // 주문 상태
    private String orderStatus;

    // 주문 일시
    private LocalDateTime orderedAt;

    // 주문 상품 목록
    private List<OrderItemResponse> orderItems;

    // Order 엔티티로부터 DTO 생성하는 생성자
    public OrderResponse(Order order) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.couponId = order.getCouponId();
        this.totalAmount = order.getTotalAmount();
        this.couponDiscountAmount = order.getCouponDiscountAmount();
        this.finalAmount = order.calculateFinalAmount();
        this.orderStatus = order.getOrderStatus();
        this.orderedAt = order.getOrderedAt();

        // 주문 항목 변환
        if (order.getOrderItems() != null) {
            this.orderItems = order.getOrderItems().stream()
                    .map(OrderItemResponse::new)
                    .collect(Collectors.toList());
        }
    }
}