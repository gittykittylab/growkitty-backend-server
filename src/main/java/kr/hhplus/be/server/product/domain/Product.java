package kr.hhplus.be.server.product.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.InsufficientStockException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_price", nullable = false)
    private Integer productPrice;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty;

    // 재고 감소
    public void decreaseStock(int quantity) {
        int restStock = this.stockQty - quantity;
        if(restStock < 0){
            throw new InsufficientStockException("재고가 부족합니다.");
        }
        this.stockQty = restStock;
    }
    // 재고 복원
    public void increaseStock(int quantity){
        this.stockQty += quantity;
    }

}