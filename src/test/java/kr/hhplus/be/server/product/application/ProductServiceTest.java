package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.domain.dto.response.ProductDetailResponse;
import kr.hhplus.be.server.product.domain.dto.response.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Long productId;
    private Product testProduct;
    private List<Product> productList;

    // 재고 복구 테스트
    private OrderItem orderItem1;
    private OrderItem orderItem2;
    private List<OrderItem> orderItems;
    private Product product2;

    @BeforeEach
    void setUp() {
        // 기본 테스트 데이터 설정
        productId = 1L;

        // 테스트용 상품 객체 생성
        testProduct = new Product();
        testProduct.setProductId(productId);
        testProduct.setProductName("테스트 상품");
        testProduct.setProductPrice(10000);
        testProduct.setStockQty(100);

        // 두 번째 상품 설정
        product2 = new Product();
        product2.setProductId(2L);
        product2.setProductName("상품2");
        product2.setProductPrice(20000);
        product2.setStockQty(200);

        // 주문 항목 설정
        orderItem1 = new OrderItem();
        orderItem1.setProductId(1L);
        orderItem1.setOrderItemQty(10);

        orderItem2 = new OrderItem();
        orderItem2.setProductId(2L);
        orderItem2.setOrderItemQty(20);

        orderItems = Arrays.asList(orderItem1, orderItem2);

        // 기본 모킹 설정
//        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
//        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
    }


    @Test
    @DisplayName("상품 목록 조회")
    void getProducts() {
        // given
        Product product2 = new Product();
        product2.setProductId(2L);
        product2.setProductName("상품2");
        product2.setProductPrice(20000);
        product2.setStockQty(200);

        List<Product> productList = Arrays.asList(testProduct, product2);
        when(productRepository.findAll()).thenReturn(productList);

        // when
        List<ProductResponse> result = productService.getProducts();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(productId);
        assertThat(result.get(0).getProductName()).isEqualTo("테스트 상품");
        assertThat(result.get(0).getProductPrice()).isEqualTo(10000);
        assertThat(result.get(0).getStockQty()).isEqualTo(100);

        assertThat(result.get(1).getProductId()).isEqualTo(2L);
        assertThat(result.get(1).getProductName()).isEqualTo("상품2");
        assertThat(result.get(1).getProductPrice()).isEqualTo(20000);
        assertThat(result.get(1).getStockQty()).isEqualTo(200);

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("상품 상세 정보 조회")
    void getProductById() {
        // given
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // when
        ProductDetailResponse result = productService.getProductById(productId);

        // then
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("테스트 상품");
        assertThat(result.getProductPrice()).isEqualTo(10000);
        assertThat(result.getStockQty()).isEqualTo(100);

        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("재고가 충분할 경우 true를 반환한다")
    void checkStockWithSufficientStock() {
        // given
        int requestQuantity = 50;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // when
        boolean result = productService.checkStock(productId, requestQuantity);

        // then
        assertTrue(result);
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("재고가 부족할 경우 false를 반환한다")
    void checkStockWithInsufficientStock() {
        // given
        int requestQuantity = 150;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // when
        boolean result = productService.checkStock(productId, requestQuantity);

        // then
        assertFalse(result);
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("재고를 성공적으로 감소시킨다")
    void decreaseStock() {
        // given
        int decreaseQuantity = 50;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // when
        productService.decreaseStock(productId, decreaseQuantity);

        // then
        assertThat(testProduct.getStockQty()).isEqualTo(50);
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 감소시키려 할 경우 예외가 발생한다")
    void decreaseStockWithInsufficientStock() {
        // given
        int decreaseQuantity = 150;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // when & then
        assertThrows(InsufficientStockException.class, () -> {
            productService.decreaseStock(productId, decreaseQuantity);
        });

        // 재고는 변경되지 않아야 함
        assertThat(testProduct.getStockQty()).isEqualTo(100);
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("주문 상품 재고 복구 성공 테스트")
    void recoverStocks_Success() {
        // when
        productService.recoverStocks(orderItems);

        // then
        assertThat(testProduct.getStockQty()).isEqualTo(100); // 100 + 10
        assertThat(product2.getStockQty()).isEqualTo(200); // 200 + 20

    }
}
