package kr.hhplus.be.server.order.domain;

import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(Long orderId);

    Order save(Order order);
}
