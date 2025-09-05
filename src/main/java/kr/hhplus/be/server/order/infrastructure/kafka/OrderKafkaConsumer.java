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
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("=== 주문 완료 메시지 수신 ===");
        log.info("Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        log.info("OrderId: {}, UserId: {}, FinalAmount: {}",
                message.getOrderId(), message.getUserId(), message.getFinalAmount());

        try {
            // 데이터 플랫폼으로 전송
            sendToDataPlatform(message);

            log.info("주문 데이터 플랫폼 전송 완료: orderId={}", message.getOrderId());

        } catch (Exception e) {
            log.error("주문 데이터 플랫폼 전송 실패: orderId={}, error={}",
                    message.getOrderId(), e.getMessage(), e);

            // 자동 커밋 방식에서는 예외 발생해도 설정된 시간에 커밋 심각한 오류의 경우 예외를 다시 던져서 Spring Kafka의 에러 핸들러가 처리하도록 함
            throw new RuntimeException("데이터 플랫폼 전송 실패", e);
        }
    }

    /**
     * 데이터 플랫폼으로 주문 정보 전송
     */
    private void sendToDataPlatform(OrderCompletedMessage message) {
        log.info("데이터 플랫폼 전송 시작: 주문ID={}, 사용자ID={}, 총금액={}, 최종금액={}",
                message.getOrderId(), message.getUserId(),
                message.getTotalAmount(), message.getFinalAmount());

        // Mock API 호출
        callDataPlatformAPI(message);

        log.info("데이터 플랫폼 전송 완료: 주문ID={}", message.getOrderId());
    }

    /**
     * Mock API 호출
     */
    private void callDataPlatformAPI(OrderCompletedMessage message) {
        try {
            log.info(">>> Mock API 호출: POST /api/v1/orders");
            log.info(">>> 주문 상세정보:");
            log.info("    - 주문ID: {}, 사용자ID: {}", message.getOrderId(), message.getUserId());
            log.info("    - 쿠폰ID: {}, 총금액: {}, 쿠폰할인: {}, 최종금액: {}",
                    message.getCouponId(), message.getTotalAmount(),
                    message.getCouponDiscountAmount(), message.getFinalAmount());
            log.info("    - 주문상태: {}, 주문시간: {}, 완료시간: {}",
                    message.getOrderStatus(), message.getOrderedAt(), message.getCompletedAt());

            // 주문 아이템 정보 출력
            if (message.getOrderItems() != null && !message.getOrderItems().isEmpty()) {
                log.info(">>> 주문 아이템 수: {}", message.getOrderItems().size());
                message.getOrderItems().forEach(item ->
                        log.info("    - [아이템] ID: {}, 상품명: {}, 상품ID: {}, 수량: {}, 가격: {}",
                                item.getOrderItemId(), item.getOrderedProductName(),
                                item.getProductId(), item.getOrderItemQty(), item.getOrderItemPrice())
                );
            }

            // API 호출 시뮬레이션 (200ms)
            Thread.sleep(200);

            log.info(">>> Mock API 응답: 200 OK");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("API 호출 중 인터럽트 발생", e);
            throw new RuntimeException("API 호출 인터럽트", e);
        } catch (Exception e) {
            log.error("데이터 플랫폼 API 호출 실패: 주문ID={}", message.getOrderId(), e);
            throw new RuntimeException("데이터 플랫폼 API 호출 실패", e);
        }
    }
}