package kr.hhplus.be.server.order.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
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
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount = 0;

    @Column(name = "coupon_discount_amount", nullable = false)
    private Integer couponDiscountAmount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    // 주문 생성 팩토리 메서드
    public static Order createOrder(Long userId) {
        Order order = new Order();
        order.setUserId(userId);
//        order.setCouponId(couponId);
        order.setTotalAmount(0);
        order.setCouponDiscountAmount(0);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderedAt(LocalDateTime.now());
        return order;
    }

    // 최종 결제 금액 계산
    public int calculateFinalAmount() {
        return totalAmount - couponDiscountAmount;
    }

    // 주문 상태 변경
    public void updateStatus(OrderStatus status) {
        this.orderStatus = status;
    }

}