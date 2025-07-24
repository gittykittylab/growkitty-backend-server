package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.dto.response.ProductResponse;
import kr.hhplus.be.server.product.infrastructure.ProductRepository;
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

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Long productId;
    private Product testProduct;
    private List<Product> productList;

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

}
