package kr.hhplus.be.server.coupon.infrastructure.kafka;

import kr.hhplus.be.server.coupon.domain.dto.message.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 발급 관련 Kafka 메시지를 발행하는 Producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponKafkaProducer {

    private final KafkaTemplate<String, CouponIssueMessage> kafkaTemplate;

    @Value("${kafka.topics.coupon-issue:coupon-issue}")
    private String couponIssueTopic;

    /**
     * 쿠폰 발급 메시지를 Kafka 토픽으로 발행
     */
    public void sendCouponIssueMessage(CouponIssueMessage message) {
        String key = message.getPolicyId().toString();

        log.info("쿠폰 발급 메시지 발행: policyId={}, userId={}",
                message.getPolicyId(), message.getUserId());

        try {
            kafkaTemplate.send(couponIssueTopic, key, message);
            log.debug("쿠폰 발급 메시지 발행 완료: policyId={}", message.getPolicyId());
        } catch (Exception e) {
            log.error("쿠폰 발급 메시지 발행 실패: policyId={}, userId={}",
                    message.getPolicyId(), message.getUserId(), e);
            throw new RuntimeException("쿠폰 발급 메시지 발행 실패", e);
        }
    }
}