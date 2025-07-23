package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.dto.request.OrderItemRequest;
import kr.hhplus.be.server.order.dto.request.OrderRequest;
import kr.hhplus.be.server.order.dto.response.OrderResponse;
import kr.hhplus.be.server.product.ProductDetailResponse;
import kr.hhplus.be.server.product.ProductService;
import kr.hhplus.be.server.user.UserService;
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

        //포인트 결제
        if(request.getUsedAmount() > 0){
            userService.usePoint(userId, request.getUsedAmount());
        }
        return new OrderResponse(order);
    }
}
