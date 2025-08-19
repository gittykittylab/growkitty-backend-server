package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.common.exception.StockRecoveryException;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.domain.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.product.domain.dto.response.ProductResponse;
import kr.hhplus.be.server.product.domain.dto.response.TopProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
//    private final TopProductRepository topProductRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 상품 조회
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. id=" + productId));
    }

    // 상품 목록 조회
    public List<ProductResponse> getProducts(){
        List<Product> products = productRepository.findAll();
        return  products.stream()
                .map(ProductResponse::new)
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
        // 3일간의 날짜 기반 키 생성
        List<String> dataKeys = List.of(
                "product_sales:" +LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "product_sales" +LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                "product_sales" +LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
        // 데이터 합산 임시 키
        String unionKey = "top_products:3days";

        // 3일치 데이터 합산
        redisTemplate.opsForZSet().unionAndStore(dataKeys.get(0), dataKeys.subList(1,dataKeys.size()), unionKey);

        // 임시 키 1시간 후 만료
        redisTemplate.expire(unionKey, 1, TimeUnit.HOURS);

        // top 5 상품 id 조회
        Set<String> topProductIdsString = redisTemplate.opsForZSet().reverseRange(unionKey,0,4);

        if(topProductIdsString == null || topProductIdsString.isEmpty()){
            return Collections.emptyList();
        }
        // DB에서 top 5 상품 상세 조회
        List<Long> topProductsIdsLong =  topProductIdsString.stream()
                .map(Long::valueOf)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllById(topProductsIdsLong)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        return topProductsIdsLong.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .map(TopProductResponse::from)
                .collect(Collectors.toList());
    }

    // 주문 성공 시 판매량 redis 업데이트
    @Transactional
    public void updateSalesRank(List<OrderItem> orderItems) {
        try {
            String todayKey = "product_sales:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            for (OrderItem item : orderItems) {
                redisTemplate.opsForZSet().incrementScore(
                        todayKey, // key
                        String.valueOf(item.getProductId()), // member(productId)
                        item.getOrderItemQty() // score
                );
            }

            //만료 기한 설정
            redisTemplate.expire(todayKey, 4, TimeUnit.DAYS);

        } catch (Exception e) {
            log.error("Failed to update sales rank", e);
        }
    }

//    // 최근 3일간 가장 많이 팔린 상위 5개 상품 조회 － 캐시 적용
//    @Cacheable(value = "topProducts", key = "'last3days'")
//    public List<TopProductResponse> getTopSellingProductsWithCache() {
//        List<TopProductView> topProducts = topProductRepository.findAll();
//        return topProducts.stream()
//                .map(TopProductResponse::from)
//                .collect(Collectors.toList());
//    }
}
