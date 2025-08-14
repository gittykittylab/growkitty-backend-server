package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.common.exception.StockRecoveryException;
import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.domain.repository.TopProductRepository;
import kr.hhplus.be.server.product.domain.TopProductView;
import kr.hhplus.be.server.product.domain.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.product.domain.dto.response.ProductResponse;
import kr.hhplus.be.server.product.domain.dto.response.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final TopProductRepository topProductRepository;

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    // 상품 조회
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. id=" + productId));
    }
    
    // 상품 목록 조회
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
    @Transactional
    public void decreaseStock(Long productId, int quantity){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. id=" + productId));

        // 재고 확인 및 감소를 하나의 락 안에서 처리
        if (product.getStockQty() < quantity) {
            throw new InsufficientStockException("재고가 부족합니다.");
        }

        product.decreaseStock(quantity);
        productRepository.save(product);
    }

    // 재고 감소 (비관적 락 적용)
    @Transactional
    public void decreaseStockWithPessimisticLock(Long productId, int quantity){
        Product product = productRepository.findByIdWithPessimisticLock(productId)
                .orElseThrow(()-> new EntityNotFoundException("상품을 찾을 수 없습니다. id=" + productId));
        product.decreaseStock(quantity);
        productRepository.save(product);
    }

    // 재고 복구
    @Transactional
    public void recoverStocks(List<OrderItem> orderItems) {
        // 상품 ID 기준 정렬 - 데드락 방지
        List<OrderItem> sortedItems = orderItems.stream()
                .sorted(Comparator.comparing(OrderItem::getProductId))
                .toList();

        for (OrderItem item : sortedItems) {
            try {
                productRepository.findById(item.getProductId())
                        .ifPresent(product -> {
                            product.increaseStock(item.getOrderItemQty());
                            productRepository.save(product);
                        });
            } catch (Exception e) {
                throw new StockRecoveryException(item.getProductId(), e.getMessage());
            }
        }
    }

    // 재고 복구 (비관적 락 적용)
    @Transactional
    public void recoverStocksWithPessimisticLock(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            try {
                productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .ifPresent(product -> {
                            product.increaseStock(item.getOrderItemQty());
                            productRepository.save(product);
                        });
            } catch (Exception e) {
                throw new StockRecoveryException(item.getProductId(), e.getMessage());
            }
        }
    }
    // 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
    public List<TopProductResponse> getTopSellingProducts() {
        List<TopProductView> topProducts = topProductRepository.findAll();
        return topProducts.stream()
                .map(TopProductResponse::from)
                .collect(Collectors.toList());
    }
    // 최근 3일간 가장 많이 팔린 상위 5개 상품 조회 － 캐시 적용
    @Cacheable(value = "topProducts", key = "'last3days'")
    public List<TopProductResponse> getTopSellingProductsWithCache() {
        List<TopProductView> topProducts = topProductRepository.findAll();
        return topProducts.stream()
                .map(TopProductResponse::from)
                .collect(Collectors.toList());
    }
}
