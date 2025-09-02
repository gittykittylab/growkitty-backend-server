package kr.hhplus.be.server.order.domain.event;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

public class OrderEvents {

    /**
     * 주문 생성 완료 이벤트
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderCompleted {
        private final Long orderId;
        private final Long userId;
        private final Integer totalAmount;
        private final Integer finalAmount;
        private final List<OrderItem> orderItems;  // OrderItem 직접 사용
        private final LocalDateTime completedAt;

        public static OrderCompleted from(Order order) {
            return OrderCompleted.builder()
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .completedAt(LocalDateTime.now())
                    .build();
        }
    }
}
