package kr.hhplus.be.server.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "total_discount_amount")
    private Integer totalDiscountAmount = 0;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "order_type", nullable = false)
    private String orderType = "NORMAL";

    @Column(name = "ordered_dt", nullable = false)
    private LocalDateTime orderedDt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 주문 항목 추가
    //양방향관계
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
}