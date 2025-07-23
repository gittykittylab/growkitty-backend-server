package kr.hhplus.be.server.order.presentation;

import jakarta.validation.Valid;
import kr.hhplus.be.server.order.application.OrderFacade;
import kr.hhplus.be.server.order.application.OrderService;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.dto.request.OrderRequest;
import kr.hhplus.be.server.order.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderFacade orderFacade;
    private final OrderService orderService;
    /**
     * 주문 생성
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request) {
        log.info("주문 생성 요청 - userId: {}, orderItems: {}",
                request.getUserId(), request.getOrderItems().size());

        OrderResponse orderResponse = orderFacade.createOrder(request);

        log.info("주문 생성 완료 - orderId: {}, totalAmount: {}",
                orderResponse.getId(), orderResponse.getTotalAmount());

        return ResponseEntity.ok(orderResponse);
    }

}
