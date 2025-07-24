package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.common.exception.PaymentException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.dto.request.OrderItemRequest;
import kr.hhplus.be.server.order.dto.request.OrderRequest;
import kr.hhplus.be.server.order.dto.response.OrderResponse;
import kr.hhplus.be.server.payment.application.PaymentService;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.user.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderFacadeTest {
    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderFacade orderFacade;

    // 테스트에 사용할 공통 객체
    private OrderRequest orderRequest;
    private ProductDetailResponse product;
    private Order order;

    // 테스트 데이터
    private final Long userId = 1L;
    private final Long productId = 100L;
    private final Long orderId = 1L;
    private final int quantity = 2;
    private final int price = 10000;
    private final int totalAmount = price * quantity;
    private final int usedPoints = 1000;

    @BeforeEach
    void setUp() {
        // OrderRequest 생성
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(quantity);

        orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setOrderItems(List.of(itemRequest));
        orderRequest.setUsedAmount(usedPoints);

        // 상품 정보 생성
        product = new ProductDetailResponse();
        product.setProductId(productId);
        product.setProductName("테스트 상품");
        product.setProductPrice(price);

        // 주문 생성
        order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setOrderStatus("PENDING");
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        when(productService.checkStock(eq(productId), eq(quantity))).thenReturn(true);
        when(productService.getProductById(eq(productId))).thenReturn(product);
        // 중요: anyList() 사용으로 실제 생성되는 OrderItem 리스트와 일치하도록 변경
        when(orderService.createOrder(eq(userId), anyList())).thenReturn(order);

        // updateOrderStatus 호출 시 order 상태 변경
        doAnswer(inv -> {
            order.setOrderStatus(inv.getArgument(1));
            return null;
        }).when(orderService).updateOrderStatus(eq(orderId), eq("PAYMENT_COMPLETED"));

        // when
        OrderResponse response = orderFacade.createOrder(orderRequest);

        // then
        assertThat(response.getOrderStatus()).isEqualTo("PAYMENT_COMPLETED");
        verify(productService).decreaseStock(eq(productId), eq(quantity));
        verify(paymentService).processPayment(eq(orderId), eq(userId), eq(totalAmount), eq(usedPoints));
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void createOrder_ThrowsWhenInsufficientStock() {
        // given
        when(productService.checkStock(eq(productId), eq(quantity))).thenReturn(false);

        // when & then
        assertThrows(InsufficientStockException.class, () ->
                orderFacade.createOrder(orderRequest)
        );

        verify(productService, never()).decreaseStock(anyLong(), anyInt());
        verify(orderService, never()).createOrder(anyLong(), anyList());
    }
}
