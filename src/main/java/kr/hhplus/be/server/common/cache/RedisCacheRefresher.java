package kr.hhplus.be.server.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RedisCacheRefresher {
    private final Logger log = LoggerFactory.getLogger(RedisCacheRefresher.class);

    @CacheEvict(value = "topProducts", allEntries = true)
    @Scheduled(cron = "0 0 */1 * * *")  // 매 시간마다 실행 (초 분 시 일 월 요일)
    public void refreshTopProductsCache() {
        log.info("Redis 캐시 갱신: topProducts 캐시가 비워졌습니다. - {}", LocalDateTime.now());
    }
}
