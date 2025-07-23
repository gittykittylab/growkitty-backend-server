package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
