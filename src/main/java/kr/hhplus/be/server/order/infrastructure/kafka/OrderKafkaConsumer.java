package kr.hhplus.be.server.order.infrastructure.kafka;

import kr.hhplus.be.server.order.domain.dto.message.OrderCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 주문 완료 메시지를 수신하여 외부 데이터 플랫폼으로 전송하는 Kafka Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {
    /**
     * 주문 완료 메시지 수신 및 데이터 플랫폼 전송
     */
    @KafkaListener(
            topics = "${kafka.topics.order-completed:order-completed}",
            groupId = "order-completed-group"
    )
    public void handleOrderCompleted(
            @Payload OrderCompletedMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("주문 완료 메시지 수신: orderId={}, partition={}, offset={}",
                message.getOrderId(), partition, offset);

        try {
            sendToDataPlatform(message);
            log.info("데이터 플랫폼 전송 완료: orderId={}", message.getOrderId());

        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("데이터 플랫폼 전송 실패", e);
        }
    }

    /**
     * 데이터 플랫폼으로 주문 정보 전송
     */
    private void sendToDataPlatform(OrderCompletedMessage message) {
        log.info("데이터 플랫폼 전송 시작: orderId={}, userId={}, finalAmount={}",
                message.getOrderId(), message.getUserId(), message.getFinalAmount());

        callDataPlatformAPI(message);
    }

    /**
     * 데이터 플랫폼 API 호출
     */
    private void callDataPlatformAPI(OrderCompletedMessage message) {
        try {
            log.info("데이터 플랫폼 API 호출: orderId={}", message.getOrderId());

            simulateApiCall();

            log.debug("데이터 플랫폼 API 응답 성공: orderId={}", message.getOrderId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API 호출 인터럽트", e);
        } catch (Exception e) {
            log.error("데이터 플랫폼 API 호출 실패: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("데이터 플랫폼 API 호출 실패", e);
        }
    }

    /**
     * Mock API 호출 시뮬레이션
     */
    private void simulateApiCall() throws InterruptedException {
        // API 호출 시뮬레이션 (200ms)
        Thread.sleep(200);
    }
}