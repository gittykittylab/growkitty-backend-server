package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.dto.reponse.WaitingQueueResponse;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<List> couponIssueScript;

    // Redis 키 패턴
    private static final String COUPON_QUEUE = "coupon:queue:%d";         // List - 대기열
    private static final String COUPON_ISSUED = "coupon:issued:%d";       // Set - 발급받은 사용자
    private static final String COUPON_STOCK = "coupon:stock:%d";         // List - 재고
    /**
     * 쿠폰 발급 요청 - 대기열 진입
     */
    public WaitingQueueResponse requestCoupon(Long policyId, Long userId) {
        String queueKey = String.format(COUPON_QUEUE, policyId);
        String issuedKey = String.format(COUPON_ISSUED, policyId);

        // 1. 중복 발급 체크 (SISMEMBER)
        if (redisTemplate.opsForSet().isMember(issuedKey, userId.toString())) {
            return WaitingQueueResponse.completed("이미 발급받은 쿠폰입니다.");
        }

        // 2. 대기열에 이미 있는지 체크
        List<String> queue = redisTemplate.opsForList().range(queueKey, 0, -1);
        if (queue != null && queue.contains(userId.toString())) {
            int position = queue.indexOf(userId.toString()) + 1;
            return WaitingQueueResponse.waiting(position, queue.size());
        }

        // 3. 대기열 진입 (LPUSH - 순서 보장)
        redisTemplate.opsForList().leftPush(queueKey, userId.toString());

        // 4. 현재 위치 반환
        Long totalWaiting = redisTemplate.opsForList().size(queueKey);
        return WaitingQueueResponse.waiting(totalWaiting.intValue(), totalWaiting);
    }

    /**
     * 대기열 상태 조회
     */
    public WaitingQueueResponse getQueueStatus(Long policyId, Long userId) {
        String queueKey = String.format(COUPON_QUEUE, policyId);
        String issuedKey = String.format(COUPON_ISSUED, policyId);

        // 1. 발급 완료 체크 (SISMEMBER)
        if (redisTemplate.opsForSet().isMember(issuedKey, userId.toString())) {
            return WaitingQueueResponse.completed("쿠폰이 발급되었습니다.");
        }

        // 2. 대기열 위치 확인
        List<String> queue = redisTemplate.opsForList().range(queueKey, 0, -1);
        if (queue == null || !queue.contains(userId.toString())) {
            return WaitingQueueResponse.active();
        }

        // LPUSH로 넣었으므로 인덱스 0이 가장 최근, 끝이 가장 오래된 사용자
        int position = queue.size() - queue.indexOf(userId.toString());
        return WaitingQueueResponse.waiting(position, queue.size());
    }

    /**
     * 대기열 처리 스케줄러 (2초마다)
     */
    @Scheduled(fixedDelay = 2000)
    public void processWaitingQueues() {
        // 활성화된 모든 쿠폰 정책 조회
        List<CouponPolicy> activePolicies = couponPolicyRepository.findAll();

        for (CouponPolicy policy : activePolicies) {
            try {
                processQueue(policy.getPolicyId());
            } catch (Exception e) {
                log.error("대기열 처리 실패 - 정책: {}, 오류: {}", policy.getPolicyId(), e.getMessage());
            }
        }
    }

    /**
     * 대기열 처리 (Lua 스크립트 사용)
     */
    private void processQueue(Long policyId) {
        String queueKey = String.format(COUPON_QUEUE, policyId);
        String issuedKey = String.format(COUPON_ISSUED, policyId);
        String stockKey = String.format(COUPON_STOCK, policyId);

        // 재고 초기화
        initializeStockIfNeeded(policyId, stockKey);

        // 한 번에 최대 10명 처리
        int processCount = 0;
        while (processCount < 10) {
            try {
                // Lua 스크립트를 사용하여 원자적으로 재고와 대기열에서 처리
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) redisTemplate.execute(
                        couponIssueScript,
                        List.of(stockKey, queueKey) // KEYS[1] = stockKey, KEYS[2] = queueKey
                );

                // 스크립트 실행 결과가 null이면 재고가 없거나 대기열이 비어있음
                if (result == null || result.isEmpty()) {
                    log.info("처리할 사용자가 없거나 재고가 소진되었습니다 - 정책: {}", policyId);
                    break;
                }

                // Lua 스크립트에서 반환된 결과: [stock, userId]
                String stock = result.get(0);
                String userId = result.get(1);

                try {
                    // 실제 쿠폰 발급 (DB 저장)
                    issueCoupon(policyId, Long.valueOf(userId));

                    // 발급 완료 기록 (SADD)
                    redisTemplate.opsForSet().add(issuedKey, userId);

                    log.info("쿠폰 발급 완료 - 사용자: {}, 정책: {}", userId, policyId);
                    processCount++;

                } catch (Exception e) {
                    log.error("쿠폰 발급 실패 - 사용자: {}, 정책: {}, 오류: {}", userId, policyId, e.getMessage());

                    // DB 발급 실패 시 Redis에서 꺼낸 재고와 사용자를 복구
                    // 재고 복구 (다시 앞쪽에 추가)
                    redisTemplate.opsForList().leftPush(stockKey, stock);
                    // 사용자를 대기열 뒤쪽에 다시 추가 (재시도 기회 제공)
                    redisTemplate.opsForList().rightPush(queueKey, userId);
                }

            } catch (Exception e) {
                log.error("Lua 스크립트 실행 실패 - 정책: {}, 오류: {}", policyId, e.getMessage());
                break; // 스크립트 실행 자체가 실패하면 루프 중단
            }
        }
    }

    /**
     * 재고 초기화 - 실제 동작 로직
     */
    private void initializeStockIfNeeded(Long policyId, String stockKey) {
        // 이미 재고가 있으면 건너뛰기
        if (redisTemplate.hasKey(stockKey)) {
            return;
        }

        try {
            CouponPolicy policy = couponPolicyRepository.findById(policyId).orElse(null);
            if (policy == null) {
                log.warn("쿠폰 정책을 찾을 수 없음: {}", policyId);
                return;
            }

            // 현재 발급된 수량 조회
            long issuedCount = couponRepository.countByPolicyId(policyId);

            // 남은 재고 계산
            int totalQuantity = policy.getTotalQuantity();
            int remainingStock = totalQuantity - (int) issuedCount;

            if (remainingStock <= 0) {
                log.info("재고 없음 - 정책: {}, 총수량: {}, 발급완료: {}",
                        policyId, totalQuantity, issuedCount);
                return;
            }

            // Redis에 재고 토큰 추가 (배치로 처리)
            String[] stockTokens = new String[remainingStock];
            for (int i = 0; i < remainingStock; i++) {
                stockTokens[i] = "STOCK";
            }
            redisTemplate.opsForList().leftPushAll(stockKey, stockTokens);

            // 24시간 TTL 설정
            redisTemplate.expire(stockKey, 24, TimeUnit.HOURS);

            log.info("재고 초기화 완료 - 정책: {}, 총수량: {}, 발급완료: {}, 재고추가: {}",
                    policyId, totalQuantity, issuedCount, remainingStock);

        } catch (Exception e) {
            log.error("재고 초기화 실패 - 정책: {}, 오류: {}", policyId, e.getMessage());
        }
    }

    /**
     * 실제 쿠폰 발급 (DB 저장)
     */
    @Transactional
    private void issueCoupon(Long policyId, Long userId) {
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("쿠폰 정책을 찾을 수 없습니다."));

        // DB 중복 체크만 (재고는 Redis에서 이미 체크됨)
        if (couponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        Coupon coupon = Coupon.createFromPolicy(policy, userId);
        couponRepository.save(coupon);
    }

    /**
     * 선착순 쿠폰 발급 기능(비관적 락 적용)
     */
    @Transactional
    public Coupon issueFirstComeCouponWithLock(Long policyId, Long userId) {
        // 비관적 락을 적용하여 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(policyId)
                .orElseThrow(() -> new RuntimeException("쿠폰 정책을 찾을 수 없습니다."));

        // 락이 적용된 발급 가능 수량 확인
        long issuedCount = couponRepository.countByPolicyIdWithLock(policyId);
        if (!policy.isAvailableForIssue(issuedCount)) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }

        // 락이 적용된 중복 발급 확인
        boolean alreadyIssued = couponRepository.existsByUserIdAndPolicyIdWithLock(userId, policyId);
        if (alreadyIssued) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        Coupon coupon = Coupon.createFromPolicy(policy, userId);
        return couponRepository.save(coupon);
    }

    /**
     * 선착순 쿠폰 발급 기능(기존)
     */
    public Coupon issueFirstComeCoupon(Long policyId, Long userId) {
        // 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("쿠폰 정책을 찾을 수 없습니다."));

        // 발급 가능 수량 확인 (도메인 메서드 사용)
        long issuedCount = couponRepository.countByPolicyId(policyId);
        if (!policy.isAvailableForIssue(issuedCount)) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }

        // 중복 발급 확인
        boolean alreadyIssued = couponRepository.existsByUserIdAndPolicyId(userId, policyId);
        if (alreadyIssued) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        Coupon coupon = Coupon.createFromPolicy(policy, userId);
        return couponRepository.save(coupon);
    }
}