package kr.hhplus.be.server.order.dto.response;

import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemResponse {
    // 주문 항목 ID
    private Long id;

    // 주문 ID
    private Long orderId;

    // 상품 정보
    private Long productId;
    private String productName;
    private Integer productPrice;

    // 주문 정보
    private Integer orderPrice;
    private Integer orderQty;
    private Integer itemDiscountAmount;

    // 적용된 쿠폰 ID
    private Long appliedCouponId;

    // 항목 소계 (orderPrice * orderQty - itemDiscountAmount)
    private Integer subtotal;

    // OrderItem 엔티티로부터 DTO 생성하는 생성자
    public OrderItemResponse(OrderItem orderItem) {
        this.id = orderItem.getId();
        this.orderId = orderItem.getOrderId();
        this.productId = orderItem.getOrderedProductId();
        this.productName = orderItem.getOrderedProductName();
        this.productPrice = orderItem.getOrderedProductPrice();
        this.orderPrice = orderItem.getOrderItemPrice();
        this.orderQty = orderItem.getOrderItemQty();

    }
}
