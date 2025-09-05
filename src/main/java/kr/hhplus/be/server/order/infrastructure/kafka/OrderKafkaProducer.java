package kr.hhplus.be.server.order.infrastructure.kafka;

import kr.hhplus.be.server.order.domain.dto.message.OrderCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


/**
 * 주문 관련 Kafka 메시지를 발행하는 Producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, OrderCompletedMessage> kafkaTemplate;

    @Value("${kafka.topics.order-completed:order-completed}")
    private String orderCompletedTopic;

    /**
     * 주문 완료 메시지를 Kafka 토픽으로 발행
     */
    public void sendOrderCompletedMessage(OrderCompletedMessage message) {
        String key = String.valueOf(message.getOrderId());

        log.info("Kafka 메시지 발행 시작: orderId={}", message.getOrderId());

        try {
            kafkaTemplate.send(orderCompletedTopic, key, message);
            log.debug("Kafka 메시지 발행 요청 완료: orderId={}", message.getOrderId());
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 실패: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("카프카 메시지 발행 실패", e);
        }
    }
}