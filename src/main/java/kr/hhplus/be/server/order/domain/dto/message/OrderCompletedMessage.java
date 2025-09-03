package kr.hhplus.be.server.order.domain.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka로 전송할 주문 완료 메시지 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedMessage {

    private Long orderId;
    private Long userId;
    private Long couponId;
    private Integer totalAmount;
    private Integer couponDiscountAmount;
    private Integer finalAmount;  // 실제 결제 금액
    private OrderStatus orderStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    private List<OrderItemInfo> orderItems;

    /**
     * Order 도메인 객체에서 Kafka 메시지로 변환
     * OrderItem 리스트를 별도로 받아서 처리
     */
    public static OrderCompletedMessage from(Order order, List<OrderItem> orderItems) {
        return OrderCompletedMessage.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .couponId(order.getCouponId())
                .totalAmount(order.getTotalAmount())
                .couponDiscountAmount(order.getCouponDiscountAmount())
                .finalAmount(order.calculateFinalAmount())
                .orderStatus(order.getOrderStatus())
                .orderedAt(order.getOrderedAt())
                .completedAt(LocalDateTime.now())
                .orderItems(orderItems.stream()
                        .map(OrderItemInfo::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 주문 아이템 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private Long orderItemId;
        private Long orderId;
        private Long productId;
        private String orderedProductName;
        private Integer orderedProductPrice;
        private Integer orderItemPrice;
        private Integer orderItemQty;

        public static OrderItemInfo from(OrderItem orderItem) {
            return OrderItemInfo.builder()
                    .orderItemId(orderItem.getOrderItemId())
                    .orderId(orderItem.getOrderId())
                    .productId(orderItem.getProductId())
                    .orderedProductName(orderItem.getOrderedProductName())
                    .orderedProductPrice(orderItem.getOrderedProductPrice())
                    .orderItemPrice(orderItem.getOrderItemPrice())
                    .orderItemQty(orderItem.getOrderItemQty())
                    .build();
        }
    }
}