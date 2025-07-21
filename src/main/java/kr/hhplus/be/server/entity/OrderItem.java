package kr.hhplus.be.server.entity;

import jakarta.persistence.*;
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
}