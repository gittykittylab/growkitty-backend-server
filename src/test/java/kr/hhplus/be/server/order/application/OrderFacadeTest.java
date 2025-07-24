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

    private OrderRequest orderRequest;
    private Order mockOrder;
    private ProductDetailResponse product1;
    private ProductDetailResponse product2;
    private Long userId = 1L;
    private Long orderId = 1L;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 정보 설정
        product1 = new ProductDetailResponse();
        product1.setProductId(1L);
        product1.setProductName("테스트 상품 1");
        product1.setProductPrice(10000);

        product2 = new ProductDetailResponse();
        product2.setProductId(2L);
        product2.setProductName("테스트 상품 2");
        product2.setProductPrice(20000);

        // 테스트용 주문 요청 객체 생성
        orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setUsedAmount(1000); // 1,000 포인트 사용

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(1);

        orderRequest.setOrderItems(Arrays.asList(item1, item2));

        // 테스트용 주문 엔티티 생성
        mockOrder = new Order();
        mockOrder.setId(orderId);
        mockOrder.setUserId(userId);
        mockOrder.setTotalAmount(40000); // 10000*2 + 20000*1
        mockOrder.setTotalDiscountAmount(0);
        mockOrder.setOrderStatus("PAYMENT_COMPLETED");
        mockOrder.setOrderType("NORMAL");
        mockOrder.setOrderedDt(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setId(1L);
        orderItem1.setProductId(1L);
        orderItem1.setProductName("테스트 상품 1");
        orderItem1.setProductPrice(10000);
        orderItem1.setOrderPrice(10000);
        orderItem1.setOrderQty(2);
        orderItem1.setItemDiscountAmount(0);
        orderItem1.setOrder(mockOrder);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setId(2L);
        orderItem2.setProductId(2L);
        orderItem2.setProductName("테스트 상품 2");
        orderItem2.setProductPrice(20000);
        orderItem2.setOrderPrice(20000);
        orderItem2.setOrderQty(1);
        orderItem2.setItemDiscountAmount(0);
        orderItem2.setOrder(mockOrder);

        orderItems.add(orderItem1);
        orderItems.add(orderItem2);
        mockOrder.setOrderItems(orderItems);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given
        when(productService.checkStock(eq(1L), eq(2))).thenReturn(true);
        when(productService.checkStock(eq(2L), eq(1))).thenReturn(true);
        when(productService.getProductById(eq(1L))).thenReturn(product1);
        when(productService.getProductById(eq(2L))).thenReturn(product2);
        when(orderService.createOrder(eq(userId), anyList())).thenReturn(mockOrder);

        // void 메서드는 따로 모킹하지 않아도 됨 (기본적으로 아무것도 하지 않음)
        // 필요한 경우 doThrow()만 사용하여 예외 상황을 모킹

        // when
        OrderResponse response = orderFacade.createOrder(orderRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalAmount()).isEqualTo(40000);
        assertThat(response.getOrderStatus()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(response.getOrderItems().size()).isEqualTo(2);

        // 검증: 재고 확인 및 감소
        verify(productService, times(1)).checkStock(eq(1L), eq(2));
        verify(productService, times(1)).checkStock(eq(2L), eq(1));
        verify(productService, times(1)).decreaseStock(eq(1L), eq(2));
        verify(productService, times(1)).decreaseStock(eq(2L), eq(1));

        // 검증: 포인트 사용
        verify(userService, times(1)).usePoint(eq(userId), eq(1000));

        // 검증: 결제 처리
        verify(paymentService, times(1)).processPayment(eq(orderId), eq(userId), eq(40000), eq(1000));

        // 검증: 주문 상태 업데이트
        verify(orderService, times(1)).updateOrderStatus(eq(orderId), eq("PAYMENT_COMPLETED"));
    }

}
