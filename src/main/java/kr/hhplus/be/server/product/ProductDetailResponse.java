package kr.hhplus.be.server.product;

import kr.hhplus.be.server.product.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductDetailResponse {
    private Long productId;
    private String productName;
    private Integer productPrice;
    private Integer stockQty;

    // Product 엔티티로부터 DTO 생성
    public ProductDetailResponse(Product product) {
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.productPrice = product.getProductPrice();
        this.stockQty = product.getStockQty();
    }
}
