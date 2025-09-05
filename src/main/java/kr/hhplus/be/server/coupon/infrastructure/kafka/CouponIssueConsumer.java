package kr.hhplus.be.server.coupon.infrastructure.kafka;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.dto.message.CouponIssueMessage;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueConsumer {
    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_ISSUED = "coupon:issued:%d";
    private static final String COUPON_PROCESSING = "coupon:processing:%d";

    /**
     * 선착순 쿠폰 발급
     */
    @KafkaListener(
            topics = "coupon-issue",
            groupId = "coupon-issue-group",
            concurrency = "3"
    )
    @Transactional
    public void handleCouponIssue(CouponIssueMessage message) {
        Long policyId = message.getPolicyId();
        Long userId = message.getUserId();

        String issuedKey = String.format(COUPON_ISSUED, policyId);
        String processingKey = String.format(COUPON_PROCESSING, policyId);

        try {
            // 1. Redis 멱등성 체크 (이미 발급된 사용자인지 확인)
            if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(issuedKey, userId.toString()))) {
                log.info("이미 발급 완료된 사용자 - userId: {}, policyId: {}", userId, policyId);
                redisTemplate.opsForSet().remove(processingKey, userId.toString());
                return;
            }

            // 2. DB 중복 발급 체크
            if (couponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
                log.info("DB에서 중복 발급 확인 - userId: {}, policyId: {}", userId, policyId);
                redisTemplate.opsForSet().add(issuedKey, userId.toString());
                redisTemplate.opsForSet().remove(processingKey, userId.toString());
                return;
            }

            // 3. 정책 및 재고 확인
            CouponPolicy policy = couponPolicyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("쿠폰 정책을 찾을 수 없습니다."));

            long issuedCount = couponRepository.countByPolicyId(policyId);
            if (!policy.isAvailableForIssue(issuedCount)) {
                log.warn("재고 부족으로 발급 불가 - userId: {}, policyId: {}, 발급완료: {}, 총수량: {}",
                        userId, policyId, issuedCount, policy.getTotalQuantity());
                redisTemplate.opsForSet().remove(processingKey, userId.toString());
                return;
            }

            // 4. 쿠폰 발급
            Coupon coupon = Coupon.createFromPolicy(policy, userId);
            couponRepository.save(coupon);

            // 5. Redis 상태 업데이트
            redisTemplate.opsForSet().add(issuedKey, userId.toString());
            redisTemplate.opsForSet().remove(processingKey, userId.toString());

            log.info("쿠폰 발급 완료 - userId: {}, policyId: {}, couponId: {}",
                    userId, policyId, coupon.getCouponId());

        } catch (DataIntegrityViolationException e) {
            // DB 제약 조건 위반 (중복 발급 등)
            log.warn("DB 제약 조건 위반 - userId: {}, policyId: {}", userId, policyId);
            redisTemplate.opsForSet().add(issuedKey, userId.toString());
            redisTemplate.opsForSet().remove(processingKey, userId.toString());

        } catch (Exception e) {
            log.error("쿠폰 발급 실패 - userId: {}, policyId: {}, 오류: {}", userId, policyId, e.getMessage());
            redisTemplate.opsForSet().remove(processingKey, userId.toString());
            throw e; // 재시도를 위해 예외 전파
        }
    }
}
