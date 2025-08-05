package kr.hhplus.be.server.order.domain.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {
    // 주문자 ID
    private Long userId;

    // 주문 상품 목록
    private List<OrderItemRequest> orderItems;

    // 사용 포인트
    private Integer usedAmount = 0;

    // 적용할 쿠폰 ID
    private Long couponId;
}