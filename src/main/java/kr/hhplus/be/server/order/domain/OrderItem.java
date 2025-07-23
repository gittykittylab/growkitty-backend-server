package kr.hhplus.be.server.order.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.product.domain.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_price", nullable = false)
    private Integer productPrice;

    @Column(name = "order_price", nullable = false)
    private Integer orderPrice;

    @Column(name = "order_qty", nullable = false)
    private Integer orderQty;

    @Column(name = "item_discount_amount", nullable = false)
    private Integer itemDiscountAmount = 0;

    @Column(name = "applied_coupon_id")
    private Long appliedCouponId;

    // 주문 상품
    public static OrderItem createOrderItem(Product product, int orderQuantity) {

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(product.getProductId());  // 주문 당시의 상품 상태를 그대로 보존
        orderItem.setProductName(product.getProductName());
        orderItem.setProductPrice(product.getProductPrice());
        orderItem.setOrderPrice(product.getProductPrice());
        orderItem.setOrderQty(orderQuantity);
        orderItem.setItemDiscountAmount(0);  // 할인 없음

        return orderItem;
    }
}