package kr.hhplus.be.server.order.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemRequest {
    // 상품 ID
    private Long productId;

    // 주문 수량
    private Integer quantity;
}