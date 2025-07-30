package kr.hhplus.be.server.product.domain.dto.response;
import kr.hhplus.be.server.product.domain.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponse {
    private Long productId;
    private String productName;
    private Integer productPrice;
    private Integer stockQty;

    // Product 엔티티로부터 DTO 생성
    public ProductResponse(Product product) {
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.productPrice = product.getProductPrice();
        this.stockQty = product.getStockQty();
    }
}
