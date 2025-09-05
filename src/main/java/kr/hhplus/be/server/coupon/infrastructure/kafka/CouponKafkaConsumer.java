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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쿠폰 발급 메시지를 수신하여 실제 쿠폰을 발급하는 Kafka Consumer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponKafkaConsumer {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_ISSUED = "coupon:issued:%d";

    /**
     * 쿠폰 발급 메시지 수신 및 처리
     */
    @KafkaListener(
            topics = "${kafka.topics.coupon-issue:coupon-issue}",
            groupId = "coupon-issue-group"
    )
    @Transactional
    public void handleCouponIssue(
            @Payload CouponIssueMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        Long policyId = message.getPolicyId();
        Long userId = message.getUserId();

        log.info("쿠폰 발급 메시지 수신: policyId={}, userId={}, partition={}, offset={}",
                policyId, userId, partition, offset);

        try {
            issueCoupon(policyId, userId);
            log.info("쿠폰 발급 완료: policyId={}, userId={}", policyId, userId);

        } catch (Exception e) {
            log.error("쿠폰 발급 실패: policyId={}, userId={}", policyId, userId, e);
            throw new RuntimeException("쿠폰 발급 실패", e);
        }
    }

    /**
     * 실제 쿠폰 발급 처리
     */
    private void issueCoupon(Long policyId, Long userId) {
        String issuedKey = String.format(COUPON_ISSUED, policyId);

        // 1. Redis 멱등성 체크
        if (isAlreadyIssued(issuedKey, userId)) {
            log.info("이미 발급 완료된 사용자 (Redis): policyId={}, userId={}", policyId, userId);
            return;
        }

        // 2. DB 중복 발급 체크
        if (couponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            log.info("DB에서 중복 발급 확인: policyId={}, userId={}", policyId, userId);
            markAsIssued(issuedKey, userId); // Redis 상태를 DB와 동기화
            return;
        }

        try {
            // 3. 쿠폰 발급
            CouponPolicy policy = getCouponPolicy(policyId);
            Coupon coupon = Coupon.createFromPolicy(policy, userId);
            couponRepository.save(coupon);

            // 4. Redis 상태 '발급 완료'로 업데이트
            markAsIssued(issuedKey, userId);

            log.info("쿠폰 발급 성공: policyId={}, userId={}, couponId={}",
                    policyId, userId, coupon.getCouponId());

        } catch (DataIntegrityViolationException e) {
            log.warn("DB 제약 조건 위반 (중복 발급): policyId={}, userId={}", policyId, userId);
            markAsIssued(issuedKey, userId); // 발급완료 처리 - 요청 완료로 간주

        } catch (Exception e) {
            log.error("쿠폰 발급 처리 중 예외 발생: policyId={}, userId={}", policyId, userId, e);
            throw e;
        }
    }

    private boolean isAlreadyIssued(String issuedKey, Long userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(issuedKey, userId.toString()));
    }

    private void markAsIssued(String issuedKey, Long userId) {
        redisTemplate.opsForSet().add(issuedKey, userId.toString());
    }

    private CouponPolicy getCouponPolicy(Long policyId) {
        return couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("쿠폰 정책을 찾을 수 없습니다."));
    }
}