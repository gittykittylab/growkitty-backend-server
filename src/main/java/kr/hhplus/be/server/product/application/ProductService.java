package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.common.exception.StockRecoveryException;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.product.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    //상품 목록 조회
    public List<ProductResponse> getProducts(){
        List<Product> products = productRepository.findAll();
        return  products.stream()
                .map(product -> new ProductResponse(product))
                .collect(Collectors.toList());
    }

    // 상품 상세 조회
    public ProductDetailResponse getProductById(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("상품을 찾을 수 없습니다. id=" + productId));
        return new ProductDetailResponse(product);
    }

    // 재고 확인
    public boolean checkStock(Long productId, int quantity){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품이 없습니다. id=" + productId));
        return product.getStockQty() >= quantity;
    }

    // 재고 감소
    public void decreaseStock(Long productId, int quantity){
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("상품을 찾을 수 없습니다. id=" + productId));
        product.decreaseStock(quantity);
    }
    // 재고 복구
    @Transactional
    public void recoverStocks(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            try {
                productRepository.findById(item.getProductId())
                        .ifPresent(product -> {
                            product.increaseStock(item.getOrderQty());
                            productRepository.save(product);
                        });
            } catch (Exception e) {
                throw new StockRecoveryException(item.getProductId(), e.getMessage());
            }
        }
    }
}
