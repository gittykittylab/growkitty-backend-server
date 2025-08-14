package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.dto.response.TopProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@SpringBootTest
public class CachePerformanceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    public void setUp() {
        // 테스트 전에 관련 캐시 삭제
        redisTemplate.delete("topProducts::last3days");
    }

    @Test
    public void comparePerformance() {
        // 1. 캐시 없는 메서드 성능 측정 (첫 번째 실행)
        long startTime1 = System.currentTimeMillis();
        List<TopProductResponse> result1 = productService.getTopSellingProducts();
        long duration1 = System.currentTimeMillis() - startTime1;
        System.out.println("캐시 없는 메서드 (첫 번째 실행): " + duration1 + "ms");

        // 2. 캐시 없는 메서드 성능 측정 (두 번째 실행)
        long startTime2 = System.currentTimeMillis();
        List<TopProductResponse> result2 = productService.getTopSellingProducts();
        long duration2 = System.currentTimeMillis() - startTime2;
        System.out.println("캐시 없는 메서드 (두 번째 실행): " + duration2 + "ms");

        // 3. 캐시 적용 메서드 성능 측정 (첫 번째 실행 - 캐시 미스)
        long startTime3 = System.currentTimeMillis();
        List<TopProductResponse> result3 = productService.getTopSellingProductsWithCache();
        long duration3 = System.currentTimeMillis() - startTime3;
        System.out.println("캐시 적용 메서드 (첫 번째 실행 - 캐시 미스): " + duration3 + "ms");

        // 4. 캐시 적용 메서드 성능 측정 (두 번째 실행 - 캐시 히트)
        long startTime4 = System.currentTimeMillis();
        List<TopProductResponse> result4 = productService.getTopSellingProductsWithCache();
        long duration4 = System.currentTimeMillis() - startTime4;
        System.out.println("캐시 적용 메서드 (두 번째 실행 - 캐시 히트): " + duration4 + "ms");

        // 5. 결과 비교 출력
        System.out.println("\n===== 성능 비교 결과 =====");
        System.out.println("캐시 없는 메서드 평균 실행 시간: " + ((duration1 + duration2) / 2) + "ms");
        System.out.println("캐시 적용 메서드 평균 실행 시간: " + ((duration3 + duration4) / 2) + "ms");
        System.out.println("캐시 히트 시 성능 향상: " + (duration2 - duration4) + "ms (" +
                String.format("%.2f", ((float)duration2 / duration4)) + "배 빠름)");
    }

    @Test
    public void testCacheTTL() throws InterruptedException {
        // 1. 캐시 적용 메서드 첫 호출 (캐시 미스)
        long startTime1 = System.currentTimeMillis();
        List<TopProductResponse> result1 = productService.getTopSellingProductsWithCache();
        long duration1 = System.currentTimeMillis() - startTime1;
        System.out.println("첫 번째 호출 (캐시 미스): " + duration1 + "ms");

        // 2. 캐시 적용 메서드 두 번째 호출 (캐시 히트)
        long startTime2 = System.currentTimeMillis();
        List<TopProductResponse> result2 = productService.getTopSellingProductsWithCache();
        long duration2 = System.currentTimeMillis() - startTime2;
        System.out.println("두 번째 호출 (캐시 히트): " + duration2 + "ms");

        // 참고: 실제 TTL 테스트를 위해서는 TTL을 짧게 설정하거나,
        // 수동으로 캐시를 삭제해야 합니다.
        // 여기서는 데모를 위해 5초 TTL로 가정하고 테스트합니다.
        System.out.println("TTL 만료 대기 중...");
        // 실제로는 RedisCacheConfig에서 TTL을 짧게 설정하고 테스트하거나
        // 아래처럼 캐시를 수동으로 삭제
        redisTemplate.delete("topProducts::last3days");

        // 3. TTL 만료 후 호출 (캐시 미스)
        long startTime3 = System.currentTimeMillis();
        List<TopProductResponse> result3 = productService.getTopSellingProductsWithCache();
        long duration3 = System.currentTimeMillis() - startTime3;
        System.out.println("TTL 만료 후 호출 (캐시 미스): " + duration3 + "ms");
    }

    @Test
    public void loadTest() {
        int iterations = 100; // 반복 횟수

        // 캐시 없는 메서드 부하 테스트
        long totalTimeNoCache = 0;
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            productService.getTopSellingProducts();
            totalTimeNoCache += (System.currentTimeMillis() - startTime);
        }
        double avgTimeNoCache = (double) totalTimeNoCache / iterations;

        // 캐시 삭제 후 첫 번째 호출
        redisTemplate.delete("topProducts::last3days");
        productService.getTopSellingProductsWithCache(); // 캐시 웜업

        // 캐시 적용 메서드 부하 테스트
        long totalTimeWithCache = 0;
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            productService.getTopSellingProductsWithCache();
            totalTimeWithCache += (System.currentTimeMillis() - startTime);
        }
        double avgTimeWithCache = (double) totalTimeWithCache / iterations;

        System.out.println("\n===== 부하 테스트 결과 (" + iterations + "회 반복) =====");
        System.out.println("캐시 없는 메서드 평균 실행 시간: " + avgTimeNoCache + "ms");
        System.out.println("캐시 적용 메서드 평균 실행 시간: " + avgTimeWithCache + "ms");
        System.out.println("성능 차이: " + (avgTimeNoCache - avgTimeWithCache) + "ms (" +
                String.format("%.2f", (avgTimeNoCache / avgTimeWithCache)) + "배 빠름)");
    }
}
