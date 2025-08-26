package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.dto.response.TopProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Testcontainers
@SqlGroup(@Sql(scripts = {"/sql/clear-data.sql", "/sql/insert-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD))
public class TopProductRedisIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 데이터 초기화
        clearRedisData();
    }

    @Test
    @DisplayName("Redis에 판매 데이터가 없을 때 빈 리스트 반환")
    void getTopSellingProducts_noSalesData() {
        // when
        List<TopProductResponse> topProducts = productService.getTopSellingProducts();

        // then
        assertThat(topProducts).isEmpty();
    }

    @Test
    @DisplayName("Redis에 판매 데이터 업데이트 후 상위 상품 조회")
    void getTopSellingProducts_withSalesData() {
        // given
        List<OrderItem> orderItems = createTestOrderItems();

        // when
        productService.updateSalesRank(orderItems);
        List<TopProductResponse> topProducts = productService.getTopSellingProducts();

        // then
        assertThat(topProducts).hasSize(2);

        // 판매량 순서 확인 (상품1: 10개, 상품2: 5개)
        assertThat(topProducts.get(0).getProductId()).isEqualTo(1L);
        assertThat(topProducts.get(0).getProductName()).isEqualTo("테스트 상품 1");

        assertThat(topProducts.get(1).getProductId()).isEqualTo(2L);
        assertThat(topProducts.get(1).getProductName()).isEqualTo("테스트 상품 2");
    }

    @Test
    @DisplayName("3일간의 데이터 합산하여 상위 상품 조회")
    void getTopSellingProducts_threeDaysData() {
        // given - 3일간의 판매 데이터 설정
        setupThreeDaysSalesData();

        // when
        List<TopProductResponse> topProducts = productService.getTopSellingProducts();

        // then
        assertThat(topProducts).hasSize(2);

        // 3일 누적 판매량: 상품2(35개) > 상품1(15개)
        assertThat(topProducts.get(0).getProductId()).isEqualTo(2L);
        assertThat(topProducts.get(1).getProductId()).isEqualTo(1L);

        // 개별 날짜별 데이터 확인으로 검증 방식 변경
        String todayKey = "product_sales:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String yesterdayKey = "product_sales:" + LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String twoDaysAgoKey = "product_sales:" + LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 각 날짜별 데이터가 올바르게 설정되었는지 확인
        assertThat(redisTemplate.opsForZSet().score(todayKey, "1")).isEqualTo(10.0);
        assertThat(redisTemplate.opsForZSet().score(todayKey, "2")).isEqualTo(8.0);
        assertThat(redisTemplate.opsForZSet().score(yesterdayKey, "1")).isEqualTo(5.0);
        assertThat(redisTemplate.opsForZSet().score(yesterdayKey, "2")).isEqualTo(12.0);
        assertThat(redisTemplate.opsForZSet().score(twoDaysAgoKey, "2")).isEqualTo(15.0);
    }

    @Test
    @DisplayName("동시 주문 발생 시 판매량 업데이트 정확성")
    void updateSalesRank_concurrentUpdates() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 동시에 여러 스레드에서 판매량 업데이트
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    List<OrderItem> orderItems = createOrderItemForThread(threadIndex);
                    productService.updateSalesRank(orderItems);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기 (타임아웃 확인)
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // 모든 스레드가 정상적으로 완료되었는지 확인
        assertThat(completed).isTrue();

        // then - 총 판매량이 정확히 집계되었는지 확인
        String todayKey = "product_sales:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 상품1: 스레드 0,2,4,6,8에서 각각 1,3,5,7,9개 = 25개
        // 상품2: 스레드 1,3,5,7,9에서 각각 2,4,6,8,10개 = 30개
        Double product1Score = redisTemplate.opsForZSet().score(todayKey, "1");
        Double product2Score = redisTemplate.opsForZSet().score(todayKey, "2");

        assertThat(product1Score).isEqualTo(25.0);
        assertThat(product2Score).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Redis 키 만료 확인")
    void updateSalesRank_keyExpiration() {
        // given
        List<OrderItem> orderItems = createTestOrderItems();

        // when
        productService.updateSalesRank(orderItems);

        // then
        String todayKey = "product_sales:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Long expiration = redisTemplate.getExpire(todayKey, TimeUnit.DAYS);

        assertThat(expiration).isGreaterThan(0);
        assertThat(expiration).isLessThanOrEqualTo(4);
    }

    @Test
    @DisplayName("상위 5개 제한 확인")
    void getTopSellingProducts_limitToTop5() {
        // given - 6개 상품에 대한 판매 데이터 생성
        List<OrderItem> orderItems = createOrderItemsForSixProducts();
        productService.updateSalesRank(orderItems);

        // when
        List<TopProductResponse> topProducts = productService.getTopSellingProducts();

        // then
        assertThat(topProducts).hasSizeLessThanOrEqualTo(5);

        // 판매량 순서대로 정렬되어 있는지 확인
        if (topProducts.size() > 1) {
            String todayKey = "product_sales:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            for (int i = 0; i < topProducts.size() - 1; i++) {
                Double currentScore = redisTemplate.opsForZSet().score(todayKey,
                        String.valueOf(topProducts.get(i).getProductId()));
                Double nextScore = redisTemplate.opsForZSet().score(todayKey,
                        String.valueOf(topProducts.get(i + 1).getProductId()));

                assertThat(currentScore).isGreaterThanOrEqualTo(nextScore);
            }
        }
    }

    // ---------- 헬퍼 메서드 ----------

    private void clearRedisData() {
        Set<String> keys = redisTemplate.keys("product_sales:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        Set<String> unionKeys = redisTemplate.keys("top_products:*");
        if (!unionKeys.isEmpty()) {
            redisTemplate.delete(unionKeys);
        }
    }

    private List<OrderItem> createTestOrderItems() {
        List<OrderItem> orderItems = new ArrayList<>();

        // 상품 1: 10개 판매
        OrderItem item1 = new OrderItem();
        item1.setProductId(1L);
        item1.setOrderItemQty(10);
        orderItems.add(item1);

        // 상품 2: 5개 판매
        OrderItem item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setOrderItemQty(5);
        orderItems.add(item2);

        return orderItems;
    }

    private List<OrderItem> createOrderItemForThread(int threadIndex) {
        List<OrderItem> orderItems = new ArrayList<>();

        // 홀수 스레드는 상품1, 짝수 스레드는 상품2
        long productId = (threadIndex % 2) + 1;
        int quantity = threadIndex + 1;

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setOrderItemQty(quantity);
        orderItems.add(item);

        return orderItems;
    }

    private List<OrderItem> createOrderItemsForSixProducts() {
        List<OrderItem> orderItems = new ArrayList<>();

        // 6개 상품에 대해 서로 다른 판매량 설정 (내림차순)
        for (long productId = 3; productId <= 8; productId++) {
            OrderItem item = new OrderItem();
            item.setProductId(productId);
            item.setOrderItemQty((int) (30 - (productId - 3) * 5)); // 30, 25, 20, 15, 10, 5
            orderItems.add(item);
        }

        return orderItems;
    }

    private void setupThreeDaysSalesData() {
        // 오늘 데이터
        String todayKey = "product_sales:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForZSet().add(todayKey, "1", 10.0);
        redisTemplate.opsForZSet().add(todayKey, "2", 8.0);
        redisTemplate.expire(todayKey, 4, TimeUnit.DAYS);

        // 어제 데이터
        String yesterdayKey = "product_sales:" + LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForZSet().add(yesterdayKey, "1", 5.0);
        redisTemplate.opsForZSet().add(yesterdayKey, "2", 12.0);
        redisTemplate.expire(yesterdayKey, 4, TimeUnit.DAYS);

        // 2일 전 데이터
        String twoDaysAgoKey = "product_sales:" + LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForZSet().add(twoDaysAgoKey, "2", 15.0);
        redisTemplate.expire(twoDaysAgoKey, 4, TimeUnit.DAYS);
    }
}