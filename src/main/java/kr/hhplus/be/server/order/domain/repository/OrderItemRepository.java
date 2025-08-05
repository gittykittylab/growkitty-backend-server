package kr.hhplus.be.server.order.domain.repository;

import kr.hhplus.be.server.order.domain.OrderItem;

import java.util.List;

public interface OrderItemRepository {
    List<OrderItem> findByOrderId(Long orderId);

    void save(OrderItem item);
}
