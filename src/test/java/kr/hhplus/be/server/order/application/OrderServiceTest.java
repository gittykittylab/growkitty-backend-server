package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.order.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private List<OrderItem> testOrderItems;
    private Long userId = 1L;
    private Long orderId = 1L;

    @BeforeEach
    void setUp() {
        // 테스트용 OrderItem 생성
        testOrderItems = new ArrayList<>();
        OrderItem item1 = new OrderItem();
        item1.setOrderId(orderId);
        item1.setOrderedProductId(1L);
        item1.setOrderedProductName("테스트 상품 1");
        item1.setOrderedProductPrice(10000);
        item1.setOrderItemPrice(10000);
        item1.setOrderItemQty(2);

        OrderItem item2 = new OrderItem();
        item2.setOrderId(orderId);
        item2.setOrderedProductId(2L);
        item2.setOrderedProductName("테스트 상품 2");
        item2.setOrderedProductPrice(20000);
        item2.setOrderItemPrice(20000);
        item2.setOrderItemQty(1);

        testOrderItems.add(item1);
        testOrderItems.add(item2);

        // 테스트용 Order 생성
        testOrder = Order.createOrder(userId);
        testOrder.setOrderId(orderId);
        testOrder.setTotalAmount(40000); // 10000*2 + 20000*1
    }

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_Success() {
        // given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(testOrderItems);

        // when
        Order result = orderService.getOrder(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderItemRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("주문 조회 실패 - 존재하지 않는 주문")
    void getOrder_NotFound() {
        // given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThrows(EntityNotFoundException.class, () -> {
            orderService.getOrder(orderId);
        });
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderItemRepository, never()).findByOrderId(anyLong());
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        Order newOrder = Order.createOrder(userId);
        when(orderRepository.save(any(Order.class)))
                .thenReturn(newOrder)  // 첫 번째 호출에서는 빈 주문 반환
                .thenReturn(testOrder); // 두 번째 호출에서는 총액이 설정된 주문 반환

        // when
        Order result = orderService.createOrder(userId, testOrderItems);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(orderRepository, times(2)).save(any(Order.class)); // 2번 저장됨
        verify(orderItemRepository, times(2)).save(any(OrderItem.class)); // 각 주문 항목 저장
    }

    @Test
    @DisplayName("주문 상태 업데이트 성공")
    void updateOrderStatus_Success() {
        // given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(testOrderItems);

        // when
        orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);

        // then
        assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("쿠폰 적용 성공")
    void applyCoupon_Success() {
        // given
        Long couponId = 100L;
        int discountAmount = 5000;

        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(testOrderItems);

        // when
        orderService.applyCoupon(orderId, couponId, discountAmount);

        // then
        assertThat(testOrder.getCouponId()).isEqualTo(couponId);
        assertThat(testOrder.getCouponDiscountAmount()).isEqualTo(discountAmount);
        verify(orderRepository, times(1)).findById(orderId);
    }
}