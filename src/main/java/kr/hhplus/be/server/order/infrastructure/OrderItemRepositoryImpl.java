package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.repository.OrderItemRepository;
import kr.hhplus.be.server.order.infrastructure.repository.OrderItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {
    public final OrderItemJpaRepository orderItemJpaRepository;
    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }

    @Override
    public void save(OrderItem item) {
        orderItemJpaRepository.save(item);
    }

    @Override
    public List<OrderItem> findAll() {
        return orderItemJpaRepository.findAll();
    }
}
