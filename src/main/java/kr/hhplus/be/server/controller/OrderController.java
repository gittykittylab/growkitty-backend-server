package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/order")
@Tag(name = "Order", description = "주문 및 결제 API")
public class OrderController {

    // Mock 데이터
    private final Map<Long, Integer> productStock = new ConcurrentHashMap<>();
    private final Map<Long, Long> userBalances = new ConcurrentHashMap<>();
    private final Map<Long, String> userCoupons = new ConcurrentHashMap<>();

    public OrderController() {
        // 초기 데이터
        productStock.put(1001L, 10);        // 상품 ID -> 재고 수량
        userBalances.put(1L, 10000L);       // 유저 ID -> 포인트 잔액
        userCoupons.put(1L, "DISCOUNT50");  // 유저 ID -> 보유 쿠폰 코드
    }

    @Operation(summary = "주문 요청", description = "상품 주문 및 결제를 처리합니다.")
    @PostMapping
    public ResponseEntity<OrderResponse> order(@RequestBody OrderRequest request) {
        Long userId = request.userId();
        Long productId = request.productId();
        int quantity = request.quantity();
        long totalAmount = request.totalAmount();
        String couponCode = request.couponCode();

        // 1. 재고 확인
        int stock = productStock.getOrDefault(productId, 0);
        if (stock < quantity) {
            return ResponseEntity.badRequest().body(
                    new OrderResponse(null, "재고 부족", 0)
            );
        }

        // 2. 쿠폰 유효성 확인
        if (couponCode != null && !couponCode.equals(userCoupons.get(userId))) {
            return ResponseEntity.badRequest().body(
                    new OrderResponse(null, "유효하지 않은 쿠폰", 0)
            );
        }

        // 3. 잔액 확인
        long balance = userBalances.getOrDefault(userId, 0L);
        if (balance < totalAmount) {
            // 재고 복원
            productStock.put(productId, stock + quantity);
            return ResponseEntity.badRequest().body(
                    new OrderResponse(null, "잔액 부족", 0)
            );
        }

        // 4. 결제 처리
        userBalances.put(userId, balance - totalAmount);
        productStock.put(productId, stock - quantity);

        // 가상 orderId 생성
        Long mockOrderId = 1L;

        return ResponseEntity.ok(
                new OrderResponse(mockOrderId, "주문 성공", (int) totalAmount)
        );
    }

}
