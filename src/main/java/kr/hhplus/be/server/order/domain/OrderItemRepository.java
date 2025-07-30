package kr.hhplus.be.server.order.domain;

import java.util.List;

public interface OrderItemRepository {
    List<OrderItem> findByOrderId(Long orderId);

    void save(OrderItem item);
}
