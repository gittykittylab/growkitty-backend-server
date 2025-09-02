package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.order.domain.event.OrderEvents;
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
     * 주문 완료 후 데이터 플랫폼으로 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOrderData(OrderEvents.OrderCompleted event) {

        log.info("데이터 플랫폼 전송: 주문ID={}, 사용자ID={}, 금액={}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount());

        // Mock API 호출
        callDataPlatformAPI(event);

        log.info("데이터 플랫폼 전송 완료: 주문ID={}", event.getOrderId());
    }

    /**
     * Mock API 호출
     */
    private void callDataPlatformAPI(OrderEvents.OrderCompleted event) {
        try {
            log.info(">>> Mock API 호출: POST /api/v1/orders");
            log.info(">>> 주문ID: {}, 사용자ID: {}, 금액: {}",
                    event.getOrderId(), event.getUserId(), event.getTotalAmount());

            // API 호출 시뮬레이션 (200ms)
            Thread.sleep(200);

            log.info(">>> Mock API 응답: 200 OK");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("API 호출 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: 주문ID={}", event.getOrderId(), e);
            throw e;
        }
    }

}