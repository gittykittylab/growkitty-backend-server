package kr.hhplus.be.server.testdata;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.TopProductView;
import kr.hhplus.be.server.product.infrastructure.ProductJpaRepository;
import kr.hhplus.be.server.product.infrastructure.TopProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("test")
@RequiredArgsConstructor
public class ProductTestDataLoader implements ApplicationRunner {

    private final ProductJpaRepository productJpaRepository;
    private final TopProductJpaRepository topProductJpaRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 이미 데이터가 있는지 확인
        if (productJpaRepository.count() >= 10) {
            System.out.println("Product 테스트 데이터가 이미 존재합니다. 건너뜁니다.");
            return;
        }

        // 상품 데이터 생성
        List<Product> products = new ArrayList<>();

        // 일반 상품 생성 (10개)
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setProductName("테스트 상품 " + i);
            product.setProductPrice(i * 1000); // 1000, 2000, ..., 10000
            product.setStockQty(i * 10);      // 10, 20, ..., 100
            product.setCreatedAt(LocalDateTime.now());
            products.add(product);
        }

        // 특수 케이스 상품 추가
        // 재고 없는 상품
        Product outOfStockProduct = new Product();
        outOfStockProduct.setProductName("품절 상품");
        outOfStockProduct.setProductPrice(5000);
        outOfStockProduct.setStockQty(0);
        outOfStockProduct.setCreatedAt(LocalDateTime.now());
        products.add(outOfStockProduct);

        // 재고 적은 상품
        Product lowStockProduct = new Product();
        lowStockProduct.setProductName("재고 적은 상품");
        lowStockProduct.setProductPrice(3000);
        lowStockProduct.setStockQty(1);
        lowStockProduct.setCreatedAt(LocalDateTime.now());
        products.add(lowStockProduct);

        // 상품 저장
        productJpaRepository.saveAll(products);
        System.out.println("Product 테스트 데이터 " + products.size() + "건 생성 완료");

        // TopProductView 데이터 생성 (상위 5개 상품)
        if (topProductJpaRepository.count() == 0) {
            List<TopProductView> topProducts = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                Product product = products.get(i);

                TopProductView topProduct = new TopProductView();
                topProduct.setProductId(product.getProductId());
                topProduct.setProductName(product.getProductName());
                topProduct.setProductPrice(product.getProductPrice());
                topProduct.setStockQty(product.getStockQty());
                topProduct.setOrderCount((long)(50 - i * 10)); // 50, 40, 30, 20, 10 (내림차순)
                topProduct.setTotalQuantity((long)(100 - i * 15)); // 100, 85, 70, 55, 40 (내림차순)

                topProducts.add(topProduct);
            }

            topProductJpaRepository.saveAll(topProducts);
            System.out.println("TopProductView 테스트 데이터 " + topProducts.size() + "건 생성 완료");
        }
    }
}