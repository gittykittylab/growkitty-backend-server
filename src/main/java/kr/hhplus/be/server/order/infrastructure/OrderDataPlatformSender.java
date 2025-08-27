package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.order.domain.event.OrderEvents;
import kr.hhplus.be.server.order.infrastructure.dto.OrderPlatformPayload;
import kr.hhplus.be.server.order.infrastructure.dto.StatusChangePlatformPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 데이터를 외부 데이터 플랫폼으로 전송하는 컴포넌트
 */
@Slf4j
@Component
public class OrderDataPlatformSender {

    /**
     * 주문 완료 후 데이터 플랫폼으로 주문 정보 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendOrderData(OrderEvents.OrderCompleted event) {
        try {
            log.info("=== 데이터 플랫폼 주문 정보 전송 시작 ===");
            log.info("주문ID: {}, 사용자ID: {}, 총액: {}",
                    event.getOrderId(), event.getUserId(), event.getTotalAmount());

            // Mock API 호출 - 실제로는 HTTP Client 사용
            callDataPlatformAPI(event);

            log.info("데이터 플랫폼 전송 성공 - 주문ID: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - 주문ID: {}, 오류: {}",
                    event.getOrderId(), e.getMessage());
            // Retry 없이 수동 재시도 로직이나 Dead Letter Queue 처리 가능
            handleTransmissionFailure(event, e);
        }
    }

    /**
     * 주문 상태 변경 시 데이터 플랫폼으로 상태 변경 정보 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendOrderStatusChange(OrderEvents.OrderStatusChanged event) {
        try {
            log.info("주문 상태 변경 정보 전송 - 주문ID: {}, {} -> {}",
                    event.getOrderId(), event.getPreviousStatus(), event.getCurrentStatus());

            callStatusChangeAPI(buildStatusPayload(event));

        } catch (Exception e) {
            log.error("상태 변경 정보 전송 실패 - 주문ID: {}", event.getOrderId());
            handleStatusChangeFailure(event, e);
        }
    }

    private boolean isPlatformApiCompatible() {
        // 플랫폼 API 스펙과 호환되는지 확인
        // 실제로는 설정값이나 환경변수로 관리
        return true; // 대부분의 경우 true
    }

    private StatusChangePlatformPayload buildStatusPayload(OrderEvents.OrderStatusChanged event) {
        return StatusChangePlatformPayload.builder()
                .orderId(event.getOrderId())
                .previousStatus(event.getPreviousStatus())
                .currentStatus(event.getCurrentStatus())
                .changeReason(event.getChangeReason())
                .changedAt(event.getChangedAt())
                .build();
    }

    /**
     * 데이터 플랫폼 API 호출 (Mock)
     */
    private void callDataPlatformAPI(Object payload) {
        // 실제 환경에서는 RestTemplate, WebClient, 또는 HTTP Client 사용
        log.info(">>> Mock API 호출: POST /api/v1/orders");

        if (payload instanceof OrderEvents.OrderCompleted) {
            OrderEvents.OrderCompleted event = (OrderEvents.OrderCompleted) payload;
            log.info(">>> Payload: 주문ID={}, 사용자ID={}, 상품수={}",
                    event.getOrderId(), event.getUserId(), event.getOrderItems().size());
        } else if (payload instanceof OrderPlatformPayload) {
            OrderPlatformPayload converted = (OrderPlatformPayload) payload;
            log.info(">>> Converted Payload: order_id={}, customer_id={}",
                    converted.getOrder_id(), converted.getCustomer_id());
        }

        // Mock 응답 시뮬레이션 (200ms 지연)
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info(">>> Mock API 응답: 200 OK");
    }

    private void callStatusChangeAPI(StatusChangePlatformPayload payload) {
        log.info(">>> Mock API 호출: PUT /api/v1/orders/{}/status", payload.getOrderId());
        log.info(">>> Status: {} -> {}", payload.getPreviousStatus(), payload.getCurrentStatus());

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info(">>> Mock API 응답: 200 OK");
    }

    /**
     * 전송 실패 처리
     */
    private void handleTransmissionFailure(OrderEvents.OrderCompleted event, Exception e) {
        // 실패 시 처리 로직
        // 1. Dead Letter Queue에 저장
        // 2. 별도 재시도 스케줄링
        // 3. 알림 발송
        log.warn("주문 {} 데이터 전송 실패 처리 - 재시도 또는 수동 확인 필요", event.getOrderId());
    }

    private void handleStatusChangeFailure(OrderEvents.OrderStatusChanged event, Exception e) {
        log.warn("주문 {} 상태 변경 전송 실패 처리", event.getOrderId());
    }
}