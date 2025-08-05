package kr.hhplus.be.server.testdata;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderStatus;
import kr.hhplus.be.server.order.infrastructure.repository.OrderItemJpaRepository;
import kr.hhplus.be.server.order.infrastructure.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("order-test")
@RequiredArgsConstructor
public class OrderTestDataLoader implements ApplicationRunner {

    // 테스트 상수들
    public static final Long USER_ID = 1L;
    public static final Long USER_ID_2 = 2L;
    public static final Long PENDING_ORDER_ID = 1L;
    public static final Long PAID_ORDER_ID = 2L;
    public static final Long CANCELED_ORDER_ID = 3L;
    public static final Long NON_EXISTING_ORDER_ID = 9999L;

    // 상품 관련 상수
    public static final Long PRODUCT_1_ID = 1L;
    public static final String PRODUCT_1_NAME = "테스트 상품 1";
    public static final int PRODUCT_1_PRICE = 1000; // 실제 사용된 가격

    public static final Long PRODUCT_2_ID = 2L;
    public static final String PRODUCT_2_NAME = "테스트 상품 2";
    public static final int PRODUCT_2_PRICE = 2000;

    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (orderRepository.count() > 0) {
            return;
        }

        createTestOrders();
    }

    private void createTestOrders() {
        // PENDING 주문
        Order pendingOrder = createOrder(
                USER_ID,
                10000,
                0,
                OrderStatus.PENDING,
                LocalDateTime.now()
        );

        createOrderItem(
                pendingOrder.getOrderId(),
                PRODUCT_1_ID,
                PRODUCT_1_NAME,
                PRODUCT_1_PRICE,
                1
        );

        // PAID 주문 (다중 상품)
        Order paidOrder = createOrder(
                USER_ID,
                PRODUCT_1_PRICE + PRODUCT_2_PRICE,
                500,
                OrderStatus.PAID,
                LocalDateTime.now().minusDays(1)
        );

        createOrderItem(
                paidOrder.getOrderId(),
                PRODUCT_1_ID,
                PRODUCT_1_NAME,
                PRODUCT_1_PRICE,
                1
        );

        createOrderItem(
                paidOrder.getOrderId(),
                PRODUCT_2_ID,
                PRODUCT_2_NAME,
                PRODUCT_2_PRICE,
                1
        );

        // CANCELED 주문
        Order canceledOrder = createOrder(
                USER_ID_2,
                PRODUCT_2_PRICE,
                0,
                OrderStatus.CANCELED,
                LocalDateTime.now().minusDays(2)
        );

        createOrderItem(
                canceledOrder.getOrderId(),
                PRODUCT_2_ID,
                PRODUCT_2_NAME,
                PRODUCT_2_PRICE,
                1
        );
    }

    private Order createOrder(Long userId, int totalAmount, int couponDiscount,
                              OrderStatus status, LocalDateTime orderedAt) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setCouponDiscountAmount(couponDiscount);
        order.setOrderStatus(status);
        order.setOrderedAt(orderedAt);
        return orderRepository.save(order);
    }

    private void createOrderItem(Long orderId, Long productId, String productName,
                                 int productPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setOrderedProductId(productId);
        item.setOrderedProductName(productName);
        item.setOrderedProductPrice(productPrice);
        item.setOrderItemPrice(productPrice);
        item.setOrderItemQty(quantity);
        orderItemRepository.save(item);
    }
}