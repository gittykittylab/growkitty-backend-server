package kr.hhplus.be.server.product.domain.dto.response;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.TopProductView;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
public class TopProductResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long productId;
    private final String productName;
    private final Integer productPrice;
    private final Integer stockQty;
    private final Long orderCount;
    private final Long totalQuantity;

    public static TopProductResponse from(TopProductView view) {
        return TopProductResponse.builder()
                .productId(view.getProductId())
                .productName(view.getProductName())
                .productPrice(view.getProductPrice())
                .stockQty(view.getStockQty())
                .orderCount(view.getOrderCount())
                .totalQuantity(view.getTotalQuantity())
                .build();
    }
    // TopProductResponse 클래스에 추가
    public static TopProductResponse from(Product product) {
        return TopProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productPrice(product.getProductPrice())
                .stockQty(product.getStockQty())
                .orderCount(null)        // Redis 방식에서는 없음
                .totalQuantity(null)     // Redis 방식에서는 없음
                .build();
    }
}