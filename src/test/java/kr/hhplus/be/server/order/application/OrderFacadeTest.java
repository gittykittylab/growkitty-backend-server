package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.common.exception.PaymentException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.dto.request.OrderItemRequest;
import kr.hhplus.be.server.order.dto.request.OrderRequest;
import kr.hhplus.be.server.order.dto.response.OrderResponse;
import kr.hhplus.be.server.payment.application.PaymentFacade;
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
    private PaymentFacade paymentFacade;

    @InjectMocks
    private OrderFacade orderFacade;

    private OrderRequest orderRequest;
    private ProductDetailResponse product;
    private Order order;

    private final Long userId = 1L;
    private final Long productId = 100L;
    private final Long orderId = 1L;
    private final int quantity = 2;
    private final int price = 10000;
    private final int totalAmount = price * quantity;

    @BeforeEach
    void setUp() {
        // 기본 주문 요청 설정
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(quantity);

        orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setOrderItems(List.of(itemRequest));
        orderRequest.setUsedAmount(0); // 기본값은 포인트 미사용

        // 상품 정보 설정
        product = new ProductDetailResponse();
        product.setProductId(productId);
        product.setProductName("테스트 상품");
        product.setProductPrice(price);

        // 주문 객체 설정
        order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setOrderStatus("PENDING");
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_BasicFlowSuccess() {
        // given
        when(productService.checkStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.getProductById(anyLong())).thenReturn(product);
        when(orderService.createOrder(anyLong(), anyList())).thenReturn(order);

        doAnswer(inv -> {
            order.setOrderStatus(inv.getArgument(1));
            return null;
        }).when(orderService).updateOrderStatus(anyLong(), anyString());

        // when
        OrderResponse response = orderFacade.createOrder(orderRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo("PAYMENT_COMPLETED");

        // 비즈니스 흐름 검증
        verify(productService).checkStock(eq(productId), eq(quantity));
        verify(productService).getProductById(eq(productId));
        verify(productService).decreaseStock(eq(productId), eq(quantity));
        verify(orderService).createOrder(eq(userId), anyList());
        verify(paymentFacade).processPayment(eq(orderId), eq(userId), eq(totalAmount), eq(0));
        verify(orderService).updateOrderStatus(eq(orderId), eq("PAYMENT_COMPLETED"));

    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_InsufficientStockFails() {
        // given
        when(productService.checkStock(anyLong(), anyInt())).thenReturn(false);

        // when & then
        assertThrows(InsufficientStockException.class, () ->
                orderFacade.createOrder(orderRequest)
        );

        // 재고 부족 시 흐름 검증
        verify(productService).checkStock(eq(productId), eq(quantity));
        verify(productService, never()).decreaseStock(anyLong(), anyInt());
        verify(orderService, never()).createOrder(anyLong(), anyList());
        verify(paymentFacade, never()).processPayment(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("주문 생성 실패 - 결제 처리 오류")
    void createOrder_PaymentFailureRollsBack() {
        // given
        when(productService.checkStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.getProductById(anyLong())).thenReturn(product);
        when(orderService.createOrder(anyLong(), anyList())).thenReturn(order);

        // 결제 처리 실패 설정
        doThrow(new RuntimeException("결제 처리 오류"))
                .when(paymentFacade).processPayment(anyLong(), anyLong(), anyInt(), anyInt());

        doAnswer(inv -> {
            order.setOrderStatus(inv.getArgument(1));
            return null;
        }).when(orderService).updateOrderStatus(anyLong(), anyString());

        // when & then
        assertThrows(PaymentException.class, () ->
                orderFacade.createOrder(orderRequest)
        );

        // 결제 실패 시 흐름 검증
        verify(productService).checkStock(eq(productId), eq(quantity));
        verify(productService).decreaseStock(eq(productId), eq(quantity));
        verify(orderService).createOrder(eq(userId), anyList());
        verify(orderService).updateOrderStatus(eq(orderId), eq("PAYMENT_FAILED"));

        // 트랜잭션 롤백 검증을 위한 부분은 단위 테스트에서 검증하기 어려움
        // 실제 롤백은 통합 테스트에서 검증
    }

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_Success() {
        // given
        when(orderService.getOrder(orderId)).thenReturn(order);

        // when
        OrderResponse response = orderFacade.getOrder(orderId);

        // then
        // OrderService.getOrder가 올바른 파라미터(orderId)로 한 번 호출되었는지 검증
        verify(orderService).getOrder(eq(orderId));

        // 반환된 응답이 예상대로인지 확인 (선택적)
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("주문 상태 업데이트 성공")
    void updateOrderStatus_Success() {
        // given
        String newStatus = "DELIVERED";

        // when
        orderFacade.updateOrderStatus(orderId, newStatus);

        // then
        // OrderService.updateOrderStatus가 올바른 파라미터(orderId, newStatus)로 한 번 호출되었는지 검증
        verify(orderService).updateOrderStatus(eq(orderId), eq(newStatus));
    }

    @Test
    @DisplayName("결제 실패 시 재고 복구 처리 검증")
    void createOrder_PaymentFailureTriggersStockRecovery() {
        // given
        when(productService.checkStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.getProductById(anyLong())).thenReturn(product);
        when(orderService.createOrder(anyLong(), anyList())).thenReturn(order);

        // 결제 처리 실패 설정
        doThrow(new RuntimeException("결제 처리 오류"))
                .when(paymentFacade).processPayment(anyLong(), anyLong(), anyInt(), anyInt());

        // when & then
        assertThrows(PaymentException.class, () -> orderFacade.createOrder(orderRequest));

        // 결제 실패 시 흐름 검증
        verify(productService).checkStock(eq(productId), eq(quantity));
        verify(productService).decreaseStock(eq(productId), eq(quantity));
        verify(orderService).createOrder(eq(userId), anyList());
        verify(orderService).updateOrderStatus(eq(orderId), eq("PAYMENT_FAILED"));
        verify(paymentFacade).handlePaymentFailure(eq(orderId), eq(userId), eq(totalAmount));

        // 재고 복구 메서드 호출 검증 - 핵심 테스트 부분
        verify(productService).recoverStocks(anyList());
    }

    @Test
    @DisplayName("결제 실패 후 재고 복구 실패 시에도 PaymentException 발생")
    void createOrder_StockRecoveryFailureAfterPaymentFailure() {
        // given
        when(productService.checkStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.getProductById(anyLong())).thenReturn(product);
        when(orderService.createOrder(anyLong(), anyList())).thenReturn(order);

        // 결제 처리 실패 설정
        doThrow(new RuntimeException("결제 처리 오류"))
                .when(paymentFacade).processPayment(anyLong(), anyLong(), anyInt(), anyInt());

        // 재고 복구 실패 설정
        doThrow(new RuntimeException("재고 복구 오류"))
                .when(productService).recoverStocks(anyList());

        // when & then
        PaymentException exception = assertThrows(PaymentException.class,
                () -> orderFacade.createOrder(orderRequest));

        // 예외 메시지 검증
        assertThat(exception.getMessage()).contains("결제 처리 실패");

        // 메서드 호출 검증
        verify(orderService).updateOrderStatus(eq(orderId), eq("PAYMENT_FAILED"));
        verify(paymentFacade).handlePaymentFailure(eq(orderId), eq(userId), eq(totalAmount));
        verify(productService).recoverStocks(anyList());
    }
}
