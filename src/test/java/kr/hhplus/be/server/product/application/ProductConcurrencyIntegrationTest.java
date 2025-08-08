package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderStatus;
import kr.hhplus.be.server.order.infrastructure.repository.OrderItemJpaRepository;
import kr.hhplus.be.server.order.infrastructure.repository.OrderJpaRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.infrastructure.repository.ProductJpaRepository;
import kr.hhplus.be.server.testdata.ProductTestDataLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, ProductTestDataLoader.class})
@ActiveProfiles("product-test")
public class ProductConcurrencyIntegrationTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderItemJpaRepository orderItemJpaRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final int THREAD_COUNT  = 5; // 동시 접속 사용자 수
    private static final int DECREASE_AMOUNT = 5; // 차감할 수량
    private static final int INCREASE_AMOUNT = 5; // 복원 시 증가할 수량
    private static final int TEST_STOCK = 100; // 테스트에 사용할 재고량

    private Long productId; // 테스트에 사용할 상품 ID

    @BeforeEach
    void setUp() {
        // ProductTestDataLoader에서 생성한 상품 중 하나 선택
        List<Product> products = productJpaRepository.findAll();
        Product testProduct = products.get(0);
        productId = testProduct.getProductId();

        // 테스트를 위해 재고를 항상 일정한 값으로 설정
        testProduct.setStockQty(TEST_STOCK);
        productJpaRepository.save(testProduct);

        // 테스트 시작 시간
        LocalDateTime testStartTime = LocalDateTime.now();
        System.out.println("테스트 시작 - 상품 ID: " + productId + ", 상품명: " + testProduct.getProductName() +
                ", 설정된 재고: " + TEST_STOCK + ", 테스트 시작 시간: " + testStartTime);
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
//        jdbcTemplate.execute("TRUNCATE TABLE products");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE order_items");
    }

    @Test
    @DisplayName("동시에 여러 사용자가 동일한 상품의 재고 차감을 요청할 때 동시성 테스트 - 락 미적용")
    void concurrentStockDecreaseTest() throws InterruptedException{
        // given
        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        try {
            // when
            // 재고 차감 요청
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        // 재고 확인 후 차감 시도
                        if (productService.checkStock(productId, DECREASE_AMOUNT)) {
                            System.out.println(Thread.currentThread().getName() + " 차감 요청 시작");
                            productService.decreaseStock(productId, DECREASE_AMOUNT);
                            System.out.println(Thread.currentThread().getName() + " 차감 요청 완료");
                        } else {
                            System.out.println(Thread.currentThread().getName() + " 재고 부족으로 차감 실패");
                        }
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 차감 중 예외 발생: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 모든 스레드가 완료될 때까지 대기
            latch.await();
            System.out.println("모든 차감 요청 완료");

            // then
            // 차감 후 데이터
            Product product = productService.getProduct(productId);
            int finalStock = product.getStockQty();
            // 실제 차감 수량
            int actualDecrease = TEST_STOCK - finalStock;
            // 예상 차감 수량
            int expectedDecrease = DECREASE_AMOUNT * THREAD_COUNT;

            System.out.println("테스트 결과 =================");
            System.out.println("초기 재고 수량: " + TEST_STOCK);
            System.out.println("예상 재고 수량: " + (TEST_STOCK - expectedDecrease));
            System.out.println("실제 재고 수량: " + finalStock);

            // 테스트 후 차감된 재고의 수량이 일치하는지 확인
            assertThat(finalStock).isEqualTo(TEST_STOCK - expectedDecrease);

            // 차감 수량이 일치하는지 확인
            assertThat(actualDecrease).isEqualTo(expectedDecrease);

        } finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("동시에 여러 동일한 상품의 재고 복원을 요청할 때 동시성 테스트 - 락 미적용")
    void concurrentStockIncreaseTest() throws InterruptedException {
        // given
        // 초기 재고를 먼저 감소시킨다 (테스트를 위해)
        int initialDecrease = THREAD_COUNT * INCREASE_AMOUNT;
        Product testProduct = productService.getProduct(productId);
        testProduct.decreaseStock(initialDecrease);
        productJpaRepository.save(testProduct);

        // 테스트 주문 아이템 생성
        List<OrderItem> allOrderItems = new ArrayList<>();

        // THREAD_COUNT 개수만큼 주문 생성
        for (int i = 0; i < THREAD_COUNT; i++) {
            Order order = Order.createOrder(1L); // 테스트용 사용자 ID
            order.setOrderStatus(OrderStatus.CANCELED);
            // 생성된 주문 저장
            orderJpaRepository.save(order);

            // 주문 아이템 생성
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getOrderId());
            orderItem.setProductId(productId);
            orderItem.setOrderedProductName(testProduct.getProductName());
            orderItem.setOrderedProductPrice(testProduct.getProductPrice());
            orderItem.setOrderItemPrice(testProduct.getProductPrice());
            orderItem.setOrderItemQty(INCREASE_AMOUNT);
            // 주문 아이템 저장
            orderItemJpaRepository.save(orderItem);

            // 주문 아이템 리스트에 추가
            allOrderItems.add(orderItem);
        }

        // 현재 재고 확인
        int stockAfterDecrease = productService.getProduct(productId).getStockQty();
        System.out.println("재고 감소 후 수량: " + stockAfterDecrease);

        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        try {
            // when
            // 각 스레드에서 재고 복원 요청
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + " 복원 요청 시작");

                        // 재고 복원 시도
                        List<OrderItem> itemsForThread = List.of(allOrderItems.get(index));
                        productService.recoverStocks(itemsForThread);

                        System.out.println(Thread.currentThread().getName() + " 복원 요청 완료");
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 복원 중 예외 발생: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 모든 스레드가 완료될 때까지 대기
            latch.await();
            System.out.println("모든 복원 요청 완료");

            // then
            // 복원 후 데이터
            Product product = productService.getProduct(productId);
            int finalStock = product.getStockQty();

            // 실제 증가 수량
            int actualIncrease = finalStock - stockAfterDecrease;
            // 예상 증가 수량
            int expectedIncrease = INCREASE_AMOUNT * THREAD_COUNT;

            System.out.println("테스트 결과 =================");
            System.out.println("초기 재고 수량: " + TEST_STOCK);
            System.out.println("감소 후 재고 수량: " + stockAfterDecrease);
            System.out.println("예상 복원 후 재고 수량: " + (stockAfterDecrease + expectedIncrease));
            System.out.println("실제 복원 후 재고 수량: " + finalStock);

            // 테스트 후 복원 후 수량이 일치하는지 확인
            assertThat(finalStock).isEqualTo(stockAfterDecrease + expectedIncrease);

            // 증가 수량이 일치하는지 확인
            assertThat(actualIncrease).isEqualTo(expectedIncrease);

        } finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }
    @Test
    @DisplayName("동시에 여러 사용자가 동일한 상품의 재고 차감을 요청할 때 동시성 테스트 - 비관적 락 적용")
    void concurrentStockDecreaseWithPessimisticLock() throws InterruptedException{
        // given
        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // 성능 측정을 위한 시작 시간 기록
        long startTime = System.currentTimeMillis();

        try {
            // when
            // 재고 차감 요청
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        // 재고 확인 후 차감 시도
                        if (productService.checkStock(productId, DECREASE_AMOUNT)) {
                            System.out.println(Thread.currentThread().getName() + " 차감 요청 시작");
                            productService.decreaseStockWithPessimisticLock(productId, DECREASE_AMOUNT);
                            System.out.println(Thread.currentThread().getName() + " 차감 요청 완료");
                        } else {
                            System.out.println(Thread.currentThread().getName() + " 재고 부족으로 차감 실패");
                        }
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 차감 중 예외 발생: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 모든 스레드가 완료될 때까지 대기
            latch.await();
            System.out.println("모든 차감 요청 완료");

            // then
            // 차감 후 데이터
            Product product = productService.getProduct(productId);
            int finalStock = product.getStockQty();
            // 실제 차감 수량
            int actualDecrease = TEST_STOCK - finalStock;
            // 예상 차감 수량
            int expectedDecrease = DECREASE_AMOUNT * THREAD_COUNT;

            System.out.println("테스트 결과 =================");
            System.out.println("초기 재고 수량: " + TEST_STOCK);
            System.out.println("예상 재고 수량: " + (TEST_STOCK - expectedDecrease));
            System.out.println("실제 재고 수량: " + finalStock);

            // 테스트 후 차감된 재고의 수량이 일치하는지 확인
            assertThat(finalStock).isEqualTo(TEST_STOCK - expectedDecrease);

            // 차감 수량이 일치하는지 확인
            assertThat(actualDecrease).isEqualTo(expectedDecrease);

        } finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("동시에 여러 동일한 상품의 재고 복원을 요청할 때 동시성 테스트 - 비관적 락 적용")
    void concurrentStockIncreaseTestWithPessimisticLock() throws InterruptedException {
        // given
        // 초기 재고를 먼저 감소시킨다 (테스트를 위해)
        int initialDecrease = THREAD_COUNT * INCREASE_AMOUNT;
        Product testProduct = productService.getProduct(productId);
        testProduct.decreaseStock(initialDecrease);
        productJpaRepository.save(testProduct);

        // 테스트 주문 아이템 생성
        List<OrderItem> allOrderItems = new ArrayList<>();

        // THREAD_COUNT 개수만큼 주문 생성
        for (int i = 0; i < THREAD_COUNT; i++) {
            Order order = Order.createOrder(1L); // 테스트용 사용자 ID
            order.setOrderStatus(OrderStatus.CANCELED);
            // 생성된 주문 저장
            orderJpaRepository.save(order);

            // 주문 아이템 생성
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getOrderId());
            orderItem.setProductId(productId);
            orderItem.setOrderedProductName(testProduct.getProductName());
            orderItem.setOrderedProductPrice(testProduct.getProductPrice());
            orderItem.setOrderItemPrice(testProduct.getProductPrice());
            orderItem.setOrderItemQty(INCREASE_AMOUNT);
            // 주문 아이템 저장
            orderItemJpaRepository.save(orderItem);

            // 주문 아이템 리스트에 추가
            allOrderItems.add(orderItem);
        }

        // 현재 재고 확인
        int stockAfterDecrease = productService.getProduct(productId).getStockQty();
        System.out.println("재고 감소 후 수량: " + stockAfterDecrease);

        // 쓰레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // 성능 측정을 위한 시작 시간 기록
        long startTime = System.currentTimeMillis();

        try {
            // when
            // 각 스레드에서 재고 복원 요청
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + " 복원 요청 시작");

                        // 재고 복원 시도
                        List<OrderItem> itemsForThread = List.of(allOrderItems.get(index));
                        productService.recoverStocksWithPessimisticLock(itemsForThread);

                        System.out.println(Thread.currentThread().getName() + " 복원 요청 완료");
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 복원 중 예외 발생: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 모든 스레드가 완료될 때까지 대기
            latch.await();
            System.out.println("모든 복원 요청 완료");

            // then
            // 복원 후 데이터
            Product product = productService.getProduct(productId);
            int finalStock = product.getStockQty();

            // 실제 증가 수량
            int actualIncrease = finalStock - stockAfterDecrease;
            // 예상 증가 수량
            int expectedIncrease = INCREASE_AMOUNT * THREAD_COUNT;

            System.out.println("테스트 결과 =================");
            System.out.println("초기 재고 수량: " + TEST_STOCK);
            System.out.println("감소 후 재고 수량: " + stockAfterDecrease);
            System.out.println("예상 복원 후 재고 수량: " + (stockAfterDecrease + expectedIncrease));
            System.out.println("실제 복원 후 재고 수량: " + finalStock);

            // 테스트 후 복원 후 수량이 일치하는지 확인
            assertThat(finalStock).isEqualTo(stockAfterDecrease + expectedIncrease);

            // 증가 수량이 일치하는지 확인
            assertThat(actualIncrease).isEqualTo(expectedIncrease);

        } finally {
            // 쓰레드 풀 종료
            executor.shutdown();
        }
    }
}
