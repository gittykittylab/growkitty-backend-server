package kr.hhplus.be.server.order.domain.repository;

import kr.hhplus.be.server.order.domain.Order;

import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(Long orderId);

    Order save(Order order);
}
