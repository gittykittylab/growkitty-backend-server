package kr.hhplus.be.server.product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private Integer price;
    private Integer stockQuantity;

    // Product 엔티티로부터 DTO 생성
    public ProductResponse(Product product) {
        this.id = product.getProductId();
        this.name = product.getProductName();
        this.price = product.getProductPrice();
        this.stockQuantity = product.getStockQty();
    }
}
