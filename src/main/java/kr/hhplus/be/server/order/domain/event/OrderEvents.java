package kr.hhplus.be.server.order.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

public class OrderEvents {

    /**
     * 주문 생성 완료 이벤트
     */
    @Getter
    @AllArgsConstructor
    public static class OrderCompleted {
        private final Long orderId;
        private final Long userId;
        private final Integer totalAmount;
        private final Integer finalAmount;
        private final List<OrderItemData> orderItems;
        private final LocalDateTime completedAt;

        @Getter
        @AllArgsConstructor
        public static class OrderItemData {
            private final Long productId;
            private final String productName;
            private final Integer quantity;
            private final Integer unitPrice;
            private final Integer totalPrice;
        }
    }

    /**
     * 주문 상태 변경 이벤트 (취소, 환불 등)
     */
    @Getter
    @AllArgsConstructor
    public static class OrderStatusChanged {
        private final Long orderId;
        private final String previousStatus;
        private final String currentStatus;
        private final String changeReason;
        private final LocalDateTime changedAt;
    }
}
