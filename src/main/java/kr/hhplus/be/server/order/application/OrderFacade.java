package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.common.exception.PaymentException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.dto.request.OrderItemRequest;
import kr.hhplus.be.server.order.dto.request.OrderRequest;
import kr.hhplus.be.server.order.dto.response.OrderResponse;
import kr.hhplus.be.server.payment.application.PaymentFacade;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * OrderFacade는 주문 관련 작업의 흐름을 조정하는 역할을 합니다.
 * 여러 서비스(OrderService, ProductService, PaymentFacade)를 조합하여
 * 클라이언트에게 단순화된 인터페이스를 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderService orderService;
    private final ProductService productService;
    private final PaymentFacade paymentFacade;

    /**
     * 주문 생성 프로세스 전체를 조정합니다.
     * 1. 상품 재고 확인 및 감소
     * 2. 주문 생성
     * 3. 결제 처리
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Long userId = request.getUserId();

        try {
            // 1. 주문 항목 준비 및 재고 확인/감소
            List<OrderItem> orderItems = prepareOrderItems(request.getOrderItems());

            // 2. 주문 생성
            Order order = orderService.createOrder(userId, orderItems);

            // 3. 결제 처리
            processPayment(order, userId, request.getUsedAmount());

            return new OrderResponse(order);
        } catch (Exception e) {
            // 주문 처리 중 오류 발생 시 처리
            log.error("주문 처리 중 오류 발생: {}", e.getMessage(), e);
            throw e; // 적절한 예외 변환 또는 처리
        }
    }

    /**
     * 주문 항목을 준비하고 재고를 확인 및 감소시킵니다.
     */
    private List<OrderItem> prepareOrderItems(List<OrderItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            Long productId = itemRequest.getProductId();
            int quantity = itemRequest.getQuantity();

            // 재고 확인
            if (!productService.checkStock(productId, quantity)) {
                throw new InsufficientStockException("재고가 부족합니다. 상품 ID: " + productId);
            }

            // 상품 정보 조회 및 주문 항목 생성
            Product product = productService.getProduct(productId);
            OrderItem orderItem = OrderItem.createOrderItem(product, quantity);
            orderItems.add(orderItem);

            // 재고 감소
            productService.decreaseStock(productId, quantity);
        }

        return orderItems;
    }

    /**
     * 결제를 처리합니다.
     */
    private void processPayment(Order order, Long userId, Integer usedPoints) {
        try {
            // 결제 처리
            int points = usedPoints != null ? usedPoints : 0;
            paymentFacade.processPayment(order.getId(), userId, order.getTotalAmount(), points);

            // 주문 상태 업데이트
            orderService.updateOrderStatus(order.getId(), "PAYMENT_COMPLETED");
        } catch (Exception e) {
            // 결제 실패 처리
            orderService.updateOrderStatus(order.getId(), "PAYMENT_FAILED");
            paymentFacade.handlePaymentFailure(order.getId(), userId, order.getTotalAmount());

            // 상품별 재고 복구 처리
            recoverInventory(order.getOrderItems());

            throw new PaymentException("결제 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 재고를 복구합니다.
     */
    private void recoverInventory(List<OrderItem> orderItems) {
        try {
            productService.recoverStocks(orderItems);
        } catch (Exception ex) {
            log.error("재고 복구 실패: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 주문을 조회합니다.
     */
    public OrderResponse getOrder(Long orderId) {
        Order order = orderService.getOrder(orderId);
        return new OrderResponse(order);
    }

    /**
     * 주문 상태를 업데이트합니다.
     */
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        orderService.updateOrderStatus(orderId, status);
    }
}