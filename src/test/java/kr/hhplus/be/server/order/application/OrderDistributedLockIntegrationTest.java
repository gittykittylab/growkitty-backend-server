package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.exception.InsufficientStockException;
import kr.hhplus.be.server.order.domain.dto.request.OrderItemRequest;
import kr.hhplus.be.server.order.domain.dto.request.OrderRequest;
import kr.hhplus.be.server.order.domain.dto.response.OrderResponse;
import kr.hhplus.be.server.product.application.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Testcontainers
@SqlGroup(@Sql(scripts = {"/sql/clear-data.sql", "/sql/insert-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD))
public class OrderDistributedLockIntegrationTest {
    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductService productService;

    // ---------- Tests ----------
    @Test
    @DisplayName("단일 상품 - 동시 주문 테스트")
    void distributedLock_differentUsersConcurrentOrders() throws InterruptedException {
        // given
        int threadCount = 3;
        ConcurrencyTestContext ctx = new ConcurrencyTestContext(threadCount);
        long productId = 1L;
        int initialStock = getCurrentStock(productId);

        log.info("=== 단일 상품 - 동시 주문 테스트 설정 ===");
        log.info("초기 재고: {}", initialStock);
        log.info("스레드 수: {}", threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 10;
            ctx.executorService.submit(() -> {
                try {
                    OrderResponse response = orderFacade.createOrder(createOrderRequest(userId, productId, 1));
                    ctx.successCount.incrementAndGet();
                    log.info("사용자 {} 주문 성공, 주문 ID: {}", userId, response.getOrderId());
                } catch (InsufficientStockException e) {
                    ctx.insufficientStockCount.incrementAndGet();
                    log.info("사용자 {} 재고 부족 예외 발생", userId);
                } catch (Exception e) {
                    ctx.failCount.incrementAndGet();
                    log.error("사용자 {} 주문 실패, 오류: {}", userId, e.getMessage());
                } finally {
                    ctx.latch.countDown();
                }
            });
        }

        ctx.latch.await();
        ctx.shutdown();

        // then
        int remainingStock = getCurrentStock(productId);

        log.info("=== 단일 상품 - 동시 주문 테스트 결과 ===");
        log.info("성공: {}", ctx.successCount.get());
        log.info("재고 부족: {}", ctx.insufficientStockCount.get());
        log.info("기타 실패: {}", ctx.failCount.get());
        log.info("남은 재고: {}", remainingStock);

        // 서로 다른 사용자의 주문은 병렬로 처리되지만, 상품 재고 락에 의해 순차적으로 재고가 감소됨
        if (initialStock >= threadCount) {
            assertThat(ctx.successCount.get()).isEqualTo(threadCount);
            assertThat(ctx.failCount.get()).isEqualTo(0);
            assertThat(remainingStock).isEqualTo(initialStock - threadCount);
        } else { // 재고가 충분하지 않다면 일부만 성공
            assertThat(ctx.successCount.get()).isLessThanOrEqualTo(initialStock);
            assertThat(ctx.insufficientStockCount.get()).isGreaterThan(0);
            assertThat(remainingStock).isLessThanOrEqualTo(initialStock);
        }
    }

    @Test
    @DisplayName("단일 상품 - 재고 경쟁 테스트")
    void distributedLock_productStockCompetition() throws InterruptedException {
        // given
        int threadCount = 10;
        ConcurrencyTestContext ctx = new ConcurrencyTestContext(threadCount);
        long productId = 1L;
        int initialStock = getCurrentStock(productId);

        log.info("=== 단일 상품 - 재고 경쟁 테스트 설정 ===");
        log.info("초기 재고: {}", initialStock);
        log.info("스레드 수: {}", threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 20; // 서로 다른 사용자
            ctx.executorService.submit(() -> {
                try {
                    OrderResponse response = orderFacade.createOrder(createOrderRequest(userId, productId, 1));
                    ctx.successCount.incrementAndGet();
                    log.info("사용자 {} 주문 성공", userId);
                } catch (InsufficientStockException e) {
                    ctx.insufficientStockCount.incrementAndGet();
                    log.info("사용자 {} 재고 부족 예외 발생", userId);
                } catch (Exception e) {
                    ctx.failCount.incrementAndGet();
                    log.error("사용자 {} 예상치 못한 오류 발생: {}", userId, e.getMessage());
                } finally {
                    ctx.latch.countDown();
                }
            });
        }

        ctx.latch.await();
        ctx.shutdown();

        // then
        int remainingStock = getCurrentStock(productId);
        int totalOrdered = initialStock - remainingStock;

        log.info("=== 단일 상품 - 재고 경쟁 테스트 결과 ===");
        log.info("성공 횟수: {}", ctx.successCount.get());
        log.info("재고 부족 횟수: {}", ctx.insufficientStockCount.get());
        log.info("기타 실패 횟수: {}", ctx.failCount.get());
        log.info("총 주문 수량: {}", totalOrdered);
        log.info("남은 재고: {}", remainingStock);

        // 성공한 주문 수와 실제 차감된 재고가 일치해야 함
        assertThat(ctx.successCount.get()).isEqualTo(totalOrdered);
        assertThat(ctx.successCount.get() + ctx.insufficientStockCount.get() + ctx.failCount.get()).isEqualTo(threadCount);

        // 재고가 음수가 되지 않아야 함
        assertThat(remainingStock).isGreaterThanOrEqualTo(0);

        // 초기 재고보다 많이 주문할 수 없음
        if (threadCount > initialStock) {
            assertThat(ctx.successCount.get()).isEqualTo(initialStock);
            assertThat(ctx.insufficientStockCount.get()).isEqualTo(threadCount - initialStock);
            assertThat(remainingStock).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("다중 품목 - 동시 주문 테스트")
    void distributedLock_multipleProductsOrder() throws InterruptedException {
        // given
        int threadCount = 5;
        ConcurrencyTestContext ctx = new ConcurrencyTestContext(threadCount);

        List<Long> productIds = List.of(1L, 2L);

        // 초기 재고 상태 기록
        Map<Long, Integer> initialStocks = productIds.stream()
                .collect(Collectors.toMap(id -> id, this::getCurrentStock));

        log.info("=== 다중 품목 - 동시 주문 테스트 설정 ===");
        log.info("초기 재고: {}", initialStocks);
        log.info("스레드 수: {}", threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 30;
            ctx.executorService.submit(() -> {
                try {
                    OrderResponse response = orderFacade.createOrder(createMultiProductOrderRequest(userId, productIds));
                    ctx.successCount.incrementAndGet();
                    log.info("사용자 {} 주문 성공, 주문 ID: {}", userId, response.getOrderId());
                } catch (InsufficientStockException e) {
                    ctx.insufficientStockCount.incrementAndGet();
                    log.info("사용자 {} 재고 부족 예외 발생", userId);
                } catch (Exception e) {
                    ctx.failCount.incrementAndGet();
                    log.error("사용자 {} 주문 실패, 오류: {}", userId, e.getMessage());
                } finally {
                    ctx.latch.countDown();
                }
            });
        }

        ctx.latch.await();
        ctx.shutdown();

        // then
        Map<Long, Integer> remainingStocks = productIds.stream()
                .collect(Collectors.toMap(id -> id, this::getCurrentStock));

        log.info("=== 다중 품목 - 동시 주문 테스트 결과 ===");
        log.info("성공: {}", ctx.successCount.get());
        log.info("재고 부족: {}", ctx.insufficientStockCount.get());
        log.info("기타 실패: {}", ctx.failCount.get());
        log.info("남은 재고: {}", remainingStocks);

        // 성공한 주문이 있을 경우, 각 상품의 재고가 정확히 감소되었는지 확인
        for (Long productId : productIds) {
            int initialStock = initialStocks.get(productId);
            int currentStock = remainingStocks.get(productId);
            int expectedStock = Math.max(0, initialStock - ctx.successCount.get());

            log.info("상품 {}: 초기={}, 현재={}, 예상={}",
                    productId, initialStock, currentStock, expectedStock);

            assertThat(currentStock).isEqualTo(expectedStock);
        }

        // 모든 쓰레드 처리 검증
        assertThat(ctx.successCount.get() + ctx.insufficientStockCount.get() + ctx.failCount.get())
                .isEqualTo(threadCount);
    }

    @Test
    @DisplayName("다중 품목 - 재고 경쟁 테스트")
    void distributedLock_multipleProductsCompetition() throws InterruptedException {
        // given
        int threadCount = 8;
        ConcurrencyTestContext ctx = new ConcurrencyTestContext(threadCount);

        // 상품 2개를 모든 스레드가 동시에 주문
        List<Long> productIds = List.of(1L, 2L);

        // 초기 재고 상태 기록
        Map<Long, Integer> initialStocks = productIds.stream()
                .collect(Collectors.toMap(id -> id, this::getCurrentStock));

        // 제한 요소가 될 최소 재고 상품 찾기
        int minInitialStock = initialStocks.values().stream().mapToInt(Integer::intValue).min().orElse(0);

        log.info("=== 다중 품목 - 재고 경쟁 테스트 설정 ===");
        log.info("초기 재고: {}", initialStocks);
        log.info("최소 초기 재고: {}", minInitialStock);
        log.info("스레드 수: {}", threadCount);

        // when - 동시에 여러 상품 주문
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 40;
            ctx.executorService.submit(() -> {
                try {
                    OrderResponse response = orderFacade.createOrder(createMultiProductOrderRequest(userId, productIds));
                    ctx.successCount.incrementAndGet();
                    log.info("사용자 {} 주문 성공, 주문 ID: {}", userId, response.getOrderId());
                } catch (InsufficientStockException e) {
                    ctx.insufficientStockCount.incrementAndGet();
                    log.info("사용자 {} 재고 부족 예외 발생", userId);
                } catch (Exception e) {
                    ctx.failCount.incrementAndGet();
                    log.error("사용자 {} 주문 실패, 오류: {}", userId, e.getMessage());
                    ctx.exceptions.add(e);
                } finally {
                    ctx.latch.countDown();
                }
            });
        }

        ctx.latch.await();
        ctx.shutdown();

        // then
        Map<Long, Integer> remainingStocks = productIds.stream()
                .collect(Collectors.toMap(id -> id, this::getCurrentStock));

        log.info("=== 다중 품목 - 재고 경쟁 테스트 결과 ===");
        log.info("성공: {}", ctx.successCount.get());
        log.info("재고 부족: {}", ctx.insufficientStockCount.get());
        log.info("기타 실패: {}", ctx.failCount.get());
        log.info("예외 발생 수: {}", ctx.exceptions.size());
        log.info("남은 재고: {}", remainingStocks);

        // 성공한 주문 수는 최소 재고량을 초과할 수 없음
        assertThat(ctx.successCount.get()).isLessThanOrEqualTo(minInitialStock);

        // 모든 재고가 0 이상이어야 함 (음수 재고 없음)
        for (Integer stock : remainingStocks.values()) {
            assertThat(stock).isGreaterThanOrEqualTo(0);
        }

        // 모든 쓰레드 처리 검증
        assertThat(ctx.successCount.get() + ctx.insufficientStockCount.get() + ctx.failCount.get())
                .isEqualTo(threadCount);

        // 재고가 충분했다면 모두 성공해야 함
        if (minInitialStock >= threadCount) {
            assertThat(ctx.successCount.get()).isEqualTo(threadCount);
            assertThat(ctx.insufficientStockCount.get()).isZero();
        } else {
            // 재고가 부족했다면 일부만 성공
            assertThat(ctx.successCount.get()).isEqualTo(minInitialStock);
            assertThat(ctx.insufficientStockCount.get()).isEqualTo(threadCount - minInitialStock);
        }
    }

    // ---------- 테스트 헬퍼 메서드 ----------

    /**
     * OrderItemRequest 객체 생성 헬퍼 메서드
     */
    private OrderItemRequest createItemRequest(long productId, int quantity) {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(quantity);
        return itemRequest;
    }

    /**
     * 주문 요청 객체 생성 헬퍼 메서드
     */
    private OrderRequest createOrderRequest(long userId, long productId, int quantity) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setOrderItems(Collections.singletonList(createItemRequest(productId, quantity)));
        return orderRequest;
    }

    /**
     * 다중 상품 주문 요청 객체 생성 헬퍼 메서드
     */
    private OrderRequest createMultiProductOrderRequest(long userId, List<Long> productIds) {
        List<OrderItemRequest> orderItems = productIds.stream()
                .map(productId -> createItemRequest(productId, 1))
                .collect(Collectors.toList());

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setOrderItems(orderItems);
        return orderRequest;
    }

    /**
     * 상품 재고 조회 헬퍼 메서드
     */
    private int getCurrentStock(long productId) {
        return productService.getProduct(productId).getStockQty();
    }

    /**
     * 스레드 풀과 실행 결과 클래스
     */
    private static class ConcurrencyTestContext {
        final ExecutorService executorService;
        final CountDownLatch latch;
        final AtomicInteger successCount;
        final AtomicInteger failCount;
        final AtomicInteger insufficientStockCount;
        final List<Exception> exceptions;

        public ConcurrencyTestContext(int threadCount) {
            this.executorService = Executors.newFixedThreadPool(threadCount);
            this.latch = new CountDownLatch(threadCount);
            this.successCount = new AtomicInteger(0);
            this.failCount = new AtomicInteger(0);
            this.insufficientStockCount = new AtomicInteger(0);
            this.exceptions = Collections.synchronizedList(new ArrayList<>());
        }

        public void shutdown() throws InterruptedException {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }
}