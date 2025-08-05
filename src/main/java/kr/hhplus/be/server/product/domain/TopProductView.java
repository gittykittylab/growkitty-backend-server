package kr.hhplus.be.server.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "top_products_3days")
@Getter @Setter
@NoArgsConstructor
public class TopProductView {
    @Id
    private Long productId;

    private String productName;
    private Integer productPrice;
    private Integer stockQty;

    // 통계 정보
    private Long orderCount;  // 주문 건수 (COUNT)
    private Long totalQuantity;  // 총 판매 수량 (SUM)

}