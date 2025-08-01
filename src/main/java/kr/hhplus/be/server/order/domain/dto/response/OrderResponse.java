package kr.hhplus.be.server.order.domain.dto.response;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private OrderStatus orderStatus;

    // 주문 일시
    private LocalDateTime orderedAt;

    // 주문 상품 목록
    private List<OrderItemResponse> orderItems = new ArrayList<>();

    // Order 엔티티만으로 기본 정보를 설정하는 생성자
    public OrderResponse(Order order) {
        this.orderId = order.getOrderId();
        this.userId = order.getUserId();
        this.couponId = order.getCouponId();
        this.totalAmount = order.getTotalAmount();
        this.couponDiscountAmount = order.getCouponDiscountAmount();
        this.finalAmount = order.calculateFinalAmount();
        this.orderStatus = order.getOrderStatus();
        this.orderedAt = order.getOrderedAt();
    }

    // OrderItems를 별도로 설정하는 메소드
    public void setOrderItemResponses(List<OrderItemResponse> orderItemResponses) {
        if (orderItemResponses != null) {
            this.orderItems = new ArrayList<>(orderItemResponses);
        }
    }

    // OrderItems를 포함하여 한번에 생성하는 정적 팩토리 메소드
    public static OrderResponse createWithItems(Order order, List<OrderItemResponse> orderItemResponses) {
        OrderResponse response = new OrderResponse(order);
        response.setOrderItemResponses(orderItemResponses);
        return response;
    }
}