package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.product.domain.dto.response.ProductResponse;
import kr.hhplus.be.server.product.domain.dto.response.TopProductResponse;
import kr.hhplus.be.server.product.infrastructure.repository.ProductJpaRepository;
import kr.hhplus.be.server.testdata.ProductTestDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, ProductTestDataLoader.class})
@ActiveProfiles("test")
@Transactional
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("상품 목록 조회")
    void getProducts() {
        // when
        List<ProductResponse> products = productService.getProducts();

        // then
        assertThat(products).isNotEmpty();
        assertThat(products.size()).isGreaterThanOrEqualTo(10); // 최소 10개 이상

        // 첫 번째 상품 확인
        ProductResponse firstProduct = products.get(0);
        assertThat(firstProduct.getProductId()).isNotNull();
        assertThat(firstProduct.getProductName()).isNotEmpty();
        assertThat(firstProduct.getProductPrice()).isPositive();
    }

    @Test
    @DisplayName("상품 상세 조회")
    void getProductById() {
        // given
        List<Product> allProducts = productJpaRepository.findAll();
        Product testProduct = allProducts.get(0);
        Long productId = testProduct.getProductId();

        // when
        ProductDetailResponse response = productService.getProductById(productId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getProductName()).isEqualTo(testProduct.getProductName());
        assertThat(response.getProductPrice()).isEqualTo(testProduct.getProductPrice());
        assertThat(response.getStockQty()).isEqualTo(testProduct.getStockQty());
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
    void getProductById_NotFound() {
        // given
        Long nonExistentProductId = 9999L;

        // when & then
        assertThatThrownBy(() -> productService.getProductById(nonExistentProductId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("상품 조회")
    void getProduct() {
        // given
        List<Product> allProducts = productJpaRepository.findAll();
        Product testProduct = allProducts.get(0);
        Long productId = testProduct.getProductId();

        // when
        Product product = productService.getProduct(productId);

        // then
        assertThat(product).isNotNull();
        assertThat(product.getProductId()).isEqualTo(productId);
        assertThat(product.getProductName()).isEqualTo(testProduct.getProductName());
        assertThat(product.getProductPrice()).isEqualTo(testProduct.getProductPrice());
        assertThat(product.getStockQty()).isEqualTo(testProduct.getStockQty());
    }

    @Test
    @DisplayName("재고 확인 - 충분한 재고")
    void checkStock_Sufficient() {
        // given
        List<Product> allProducts = productJpaRepository.findAll();
        Product testProduct = allProducts.stream()
                .filter(p -> p.getStockQty() >= 50)
                .findFirst()
                .orElseThrow();
        Long productId = testProduct.getProductId();
        int quantity = testProduct.getStockQty() / 2; // 재고의 절반만 사용

        // when
        boolean isStockSufficient = productService.checkStock(productId, quantity);

        // then
        assertThat(isStockSufficient).isTrue();
    }

    @Test
    @DisplayName("재고 확인 - 부족한 재고")
    void checkStock_Insufficient() {
        // given
        List<Product> allProducts = productJpaRepository.findAll();
        Product testProduct = allProducts.get(0);
        Long productId = testProduct.getProductId();
        int quantity = testProduct.getStockQty() + 1; // 재고보다 1개 더 많이 요청

        // when
        boolean isStockSufficient = productService.checkStock(productId, quantity);

        // then
        assertThat(isStockSufficient).isFalse();
    }

    @Test
    @DisplayName("재고 감소")
    void decreaseStock() {
        // given
        List<Product> allProducts = productJpaRepository.findAll();
        Product testProduct = allProducts.stream()
                .filter(p -> p.getStockQty() >= 30)
                .findFirst()
                .orElseThrow();
        Long productId = testProduct.getProductId();
        int initialStock = testProduct.getStockQty();
        int decreaseAmount = 10;

        // when
        productService.decreaseStock(productId, decreaseAmount);

        // then
        Product updatedProduct = productJpaRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQty()).isEqualTo(initialStock - decreaseAmount);
    }

    @Test
    @DisplayName("재고보다 많은 수량 감소 시 예외 발생")
    void decreaseStock_InsufficientStock() {
        // given
        List<Product> allProducts = productJpaRepository.findAll();
        Product testProduct = allProducts.get(0);
        Long productId = testProduct.getProductId();
        int excessiveAmount = testProduct.getStockQty() + 1; // 재고보다 1개 더 많이 요청

        // when & then
        assertThatThrownBy(() -> productService.decreaseStock(productId, excessiveAmount))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("재고 복구")
    void recoverStocks() {
        // given
        List<Product> allProducts = productJpaRepository.findAll();
        Product testProduct = allProducts.get(0);
        Long productId = testProduct.getProductId();
        int initialStock = testProduct.getStockQty();
        int decreaseAmount = 5;

        // 먼저 재고 감소
        productService.decreaseStock(productId, decreaseAmount);

        // 주문 항목 생성
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderedProductId(productId);
        orderItem.setOrderItemQty(decreaseAmount);
        orderItems.add(orderItem);

        // when
        productService.recoverStocks(orderItems);

        // then
        Product updatedProduct = productJpaRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQty()).isEqualTo(initialStock); // 원래 재고로 복구됨
    }

    @Test
    @DisplayName("최근 3일간 가장 많이 팔린 상위 상품 조회")
    void getTopSellingProducts() {
        // when
        List<TopProductResponse> topProducts = productService.getTopSellingProducts();

        // then
        assertThat(topProducts).isNotEmpty();
        assertThat(topProducts.size()).isLessThanOrEqualTo(5); // 최대 5개

        // 판매량 내림차순 정렬 확인 (첫 번째 상품이 가장 많이 팔린 상품)
        if (topProducts.size() >= 2) {
            assertThat(topProducts.get(0).getTotalQuantity())
                    .isGreaterThanOrEqualTo(topProducts.get(1).getTotalQuantity());
        }

        // 상품 정보 확인
        TopProductResponse firstTopProduct = topProducts.get(0);
        assertThat(firstTopProduct.getProductId()).isNotNull();
        assertThat(firstTopProduct.getProductName()).isNotEmpty();
        assertThat(firstTopProduct.getProductPrice()).isPositive();
        assertThat(firstTopProduct.getOrderCount()).isPositive();
        assertThat(firstTopProduct.getTotalQuantity()).isPositive();
    }
}