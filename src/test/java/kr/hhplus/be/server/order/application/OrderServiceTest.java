package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.infrastructure.OrderRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

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
        item1.setProductId(1L);
        item1.setProductName("테스트 상품 1");
        item1.setProductPrice(10000);
        item1.setOrderPrice(10000);
        item1.setOrderQty(2);
        item1.setItemDiscountAmount(0);

        OrderItem item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setProductName("테스트 상품 2");
        item2.setProductPrice(20000);
        item2.setOrderPrice(20000);
        item2.setOrderQty(1);
        item2.setItemDiscountAmount(0);

        testOrderItems.add(item1);
        testOrderItems.add(item2);

        // 테스트용 Order 생성
        testOrder = Order.createOrder(userId, testOrderItems);
        testOrder.setId(orderId);
    }

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_Success() {
        // given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));

        // when
        Order result = orderService.getOrder(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getOrderItems().size()).isEqualTo(2);
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        Order result = orderService.createOrder(userId, testOrderItems);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getOrderItems().size()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualTo(40000); // 10000*2 + 20000*1
        assertThat(result.getOrderStatus()).isEqualTo("PENDING");
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}
