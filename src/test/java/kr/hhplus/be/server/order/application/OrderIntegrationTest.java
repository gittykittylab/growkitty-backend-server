package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderStatus;
import kr.hhplus.be.server.order.domain.dto.request.OrderItemRequest;
import kr.hhplus.be.server.order.domain.dto.request.OrderRequest;
import kr.hhplus.be.server.order.domain.dto.response.OrderResponse;
import kr.hhplus.be.server.testdata.OrderTestDataLoader;
import kr.hhplus.be.server.testdata.ProductTestDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({TestcontainersConfiguration.class, ProductTestDataLoader.class})
@ActiveProfiles("order-test")
@Transactional
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderFacade orderFacade;

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_Success() {
        // when
        Order foundOrder = orderService.getOrder(OrderTestDataLoader.PENDING_ORDER_ID);

        // then
        assertNotNull(foundOrder);
        assertEquals(OrderTestDataLoader.PENDING_ORDER_ID, foundOrder.getOrderId());
        assertEquals(OrderTestDataLoader.USER_ID, foundOrder.getUserId());
        assertEquals(OrderStatus.PENDING, foundOrder.getOrderStatus());
    }

    @Test
    @DisplayName("존재하지 않는 주문 조회 실패")
    void getOrder_NotFound() {
        // when & then
        assertThrows(EntityNotFoundException.class, () ->
                orderService.getOrder(OrderTestDataLoader.NON_EXISTING_ORDER_ID));
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        OrderRequest request = createOrderRequest(
                OrderTestDataLoader.USER_ID,
                OrderTestDataLoader.PRODUCT_1_ID,
                5
        );

        // when
        OrderResponse response = orderFacade.createOrder(request);

        // then
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals(OrderTestDataLoader.PRODUCT_1_PRICE * 5, response.getTotalAmount());
        assertEquals(OrderStatus.PAID, response.getOrderStatus());

        // 실제 생성 확인
        Order createdOrder = orderService.getOrder(response.getOrderId());
        assertEquals(OrderTestDataLoader.USER_ID, createdOrder.getUserId());
        assertEquals(OrderTestDataLoader.PRODUCT_1_PRICE * 5, createdOrder.getTotalAmount());
    }

    @Test
    @DisplayName("재고 부족으로 주문 실패")
    void createOrder_InsufficientStock() {
        // given - 재고보다 많은 수량 주문
        OrderRequest request = createOrderRequest(
                OrderTestDataLoader.USER_ID,
                OrderTestDataLoader.PRODUCT_1_ID,
                101 // 충분히 큰 수량으로 재고 부족 유발
        );

        // when & then
        assertThrows(InsufficientStockException.class, () ->
                orderFacade.createOrder(request));
    }

    @Test
    @DisplayName("주문 상태 업데이트 성공")
    void updateOrderStatus_Success() {
        // given
        OrderStatus newStatus = OrderStatus.PAID;

        // when
        orderService.updateOrderStatus(OrderTestDataLoader.PENDING_ORDER_ID, newStatus);

        // then
        Order updatedOrder = orderService.getOrder(OrderTestDataLoader.PENDING_ORDER_ID);
        assertEquals(newStatus, updatedOrder.getOrderStatus());
    }

    @Test
    @DisplayName("주문 쿠폰 적용 성공")
    void applyCoupon_Success() {
        // given
        Long couponId = 1L;
        int discountAmount = 1000;
        Order originalOrder = orderService.getOrder(OrderTestDataLoader.PAID_ORDER_ID);
        int originalTotalAmount = originalOrder.getTotalAmount();

        // when
        orderService.applyCoupon(OrderTestDataLoader.PAID_ORDER_ID, couponId, discountAmount);

        // then
        Order updatedOrder = orderService.getOrder(OrderTestDataLoader.PAID_ORDER_ID);
        assertEquals(couponId, updatedOrder.getCouponId());
        assertEquals(discountAmount, updatedOrder.getCouponDiscountAmount());
        assertEquals(originalTotalAmount - discountAmount, updatedOrder.calculateFinalAmount());
    }

    private OrderRequest createOrderRequest(Long userId, Long productId, int quantity) {
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setUsedAmount(0);

        OrderItemRequest orderItem = new OrderItemRequest();
        orderItem.setProductId(productId);
        orderItem.setQuantity(quantity);

        request.setOrderItems(List.of(orderItem));
        return request;
    }
}