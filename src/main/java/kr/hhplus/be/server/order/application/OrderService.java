package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    // 주문 조회
    public Order getOrder(Long orderId){
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
    }

    // 주문 생성
    @Transactional
    public Order createOrder(Long userId, List<OrderItem> orderItems){
        // Order 엔티티 메서드 활용
        Order order = Order.createOrder(userId, orderItems);
        return  orderRepository.save(order);
    }

    // 주문 상태 업데이트
    public void updateOrderStatus(Long orderId, String status){
        Order order = getOrder(orderId);
        order.setOrderStatus(status);
    }

}
