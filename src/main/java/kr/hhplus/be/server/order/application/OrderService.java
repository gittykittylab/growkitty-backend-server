package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderItemRepository;
import kr.hhplus.be.server.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // 주문 조회
    public Order getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));

        // 주문 항목 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        return order;
    }

    // 주문 생성
    @Transactional
    public Order createOrder(Long userId, List<OrderItem> orderItems) {
        // 주문 생성
        Order order = Order.createOrder(userId);
        order = orderRepository.save(order);  // ID 생성을 위해 먼저 저장

        // 주문 항목 처리 및 총액 계산
        int totalAmount = 0;

        for (OrderItem item : orderItems) {
            // 주문 ID 설정
            item.setOrderId(order.getOrderId());

            // 주문 항목 저장
            orderItemRepository.save(item);

            // 총액 계산
            totalAmount += item.getOrderItemPrice() * item.getOrderItemQty();
        }

        // 주문 총액 설정
        order.setTotalAmount(totalAmount);

        // 업데이트된 주문 저장
        return orderRepository.save(order);
    }

    // 쿠폰 적용
    @Transactional
    public void applyCoupon(Long orderId, Long couponId, int discountAmount) {
        Order order = getOrder(orderId);
        order.setCouponId(couponId);
        order.setCouponDiscountAmount(discountAmount);
    }

    // 주문 상태 업데이트
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = getOrder(orderId);
        order.updateStatus(status);
    }
}