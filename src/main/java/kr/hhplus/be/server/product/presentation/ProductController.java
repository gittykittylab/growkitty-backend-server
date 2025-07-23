package kr.hhplus.be.server.product.presentation;

import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.product.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    /**
     * 상품 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> products = productService.getProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long productId) {
        ProductDetailResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    /**
     * 재고 확인
     */
    @GetMapping("/{productId}/stock")
    public ResponseEntity<Boolean> checkStock(
            @PathVariable Long productId,
            @RequestParam int quantity) {
        boolean hasStock = productService.checkStock(productId, quantity);
        return ResponseEntity.ok(hasStock);
    }
}
