package kr.hhplus.be.server.order.infrastructure.kafka;

import kr.hhplus.be.server.order.domain.dto.message.OrderCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 주문 관련 Kafka 메시지를 발행하는 Producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, OrderCompletedMessage> kafkaTemplate;

    @Value("${topics.order-completed}")
    private String orderCompletedTopic;

    /**
     * 주문 완료 메시지를 Kafka 토픽으로 발행
     */
    public void sendOrderCompletedMessage(OrderCompletedMessage message) {
        String key = String.valueOf(message.getOrderId()); // 파티셔닝용 키

        log.info("=== Kafka 메시지 발행 시작 ===");
        log.info("Topic: {}, OrderId: {}, UserId: {}, FinalAmount: {}",
                orderCompletedTopic, message.getOrderId(), message.getUserId(), message.getFinalAmount());

        try {
            // 비동기 메시지 발행
            CompletableFuture<SendResult<String, OrderCompletedMessage>> future =
                    kafkaTemplate.send(orderCompletedTopic, key, message);

            // 결과 처리 콜백
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handleSendFailure(message, throwable);
                } else {
                    handleSendSuccess(message, result);
                }
            });

        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 예외 발생: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("카프카 메시지 발행 실패", e);
        }
    }

    /**
     * 메시지 발행 성공 처리
     */
    private void handleSendSuccess(OrderCompletedMessage message, SendResult<String, OrderCompletedMessage> result) {
        log.info("=== Kafka 메시지 발행 성공 ===");
        log.info("OrderId: {}, Partition: {}, Offset: {}",
                message.getOrderId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    /**
     * 메시지 발행 실패 처리
     */
    private void handleSendFailure(OrderCompletedMessage message, Throwable throwable) {
        log.error("=== Kafka 메시지 발행 실패 ===");
        log.error("OrderId: {}, Error: {}", message.getOrderId(), throwable.getMessage(), throwable);
    }
}