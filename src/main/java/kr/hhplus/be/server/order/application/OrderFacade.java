package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.common.exception.PaymentException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.dto.request.OrderItemRequest;
import kr.hhplus.be.server.order.dto.request.OrderRequest;
import kr.hhplus.be.server.order.dto.response.OrderResponse;
import kr.hhplus.be.server.payment.application.PaymentService;
import kr.hhplus.be.server.product.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.user.application.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final PaymentService paymentService;

    // 주문 생성 및 재고 처리
    @Transactional
    public OrderResponse createOrder(OrderRequest request){
        Long userId = request.getUserId();
        List<OrderItemRequest> itemRequests = request.getOrderItems();
        List<OrderItem> orderItems = new ArrayList<>();

        // 상품 재고 확인 및 감소
        for (OrderItemRequest itemRequest : itemRequests){
            Long productId = itemRequest.getProductId();
            int quantity = itemRequest.getQuantity();

            //재고 확인
            if(!productService.checkStock(productId, quantity)){
                throw  new InsufficientStockException("재고가 부족합니다.");
            }

            //상품 정보 조회
            ProductDetailResponse product = productService.getProductById(productId);

            // 주문 항목
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(productId);
            orderItem.setProductName(product.getProductName());
            orderItem.setProductPrice(product.getProductPrice());
            orderItem.setOrderPrice(product.getProductPrice());
            orderItem.setOrderQty(quantity);
            orderItem.setItemDiscountAmount(0);

            orderItems.add(orderItem);

            //재고 감소
            productService.decreaseStock(productId, quantity);
        }
        // 주문 생성
        Order order = orderService.createOrder(userId, orderItems);

        try {
            // 포인트 차감 (있는 경우)
            int usedPoints = request.getUsedAmount() != null ? request.getUsedAmount() : 0;
            if (usedPoints > 0) {
                userService.usePoint(userId, usedPoints);
            }

            // 결제 정보 저장
            paymentService.processPayment(
                    order.getId(),
                    userId,
                    order.getTotalAmount(),
                    usedPoints
            );

            // 주문 상태 업데이트
            orderService.updateOrderStatus(order.getId(), "PAYMENT_COMPLETED");

            return new OrderResponse(order);

        } catch (Exception e) {
            // 결제 실패 시 결제 실패 정보 저장
            paymentService.saveFailedPayment(
                    order.getId(),
                    userId,
                    order.getTotalAmount()
            );

            // 주문 상태 업데이트
            orderService.updateOrderStatus(order.getId(), "PAYMENT_FAILED");

            // 예외 발생 (트랜잭션 롤백으로 포인트 차감도 취소됨)
            throw new PaymentException("결제 처리 실패: " + e.getMessage());
        }
    }
}
