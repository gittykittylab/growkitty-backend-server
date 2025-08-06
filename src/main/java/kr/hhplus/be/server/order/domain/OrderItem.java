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
    private Long orderItemId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "ordered_product_name", nullable = false, length = 100)
    private String orderedProductName;

    @Column(name = "ordered_product_price", nullable = false)
    private Integer orderedProductPrice;

    @Column(name = "order_item_price", nullable = false)
    private Integer orderItemPrice;

    @Column(name = "order_item_qty", nullable = false)
    private Integer orderItemQty;

    // 팩토리 메서드 - Product 객체로부터 생성
    public static OrderItem createOrderItem(Product product, int orderQty, Long orderId) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setProductId(product.getProductId());
        orderItem.setOrderedProductName(product.getProductName());
        orderItem.setOrderedProductPrice(product.getProductPrice());
        orderItem.setOrderItemPrice(product.getProductPrice());
        orderItem.setOrderItemQty(orderQty);
        return orderItem;
    }
}