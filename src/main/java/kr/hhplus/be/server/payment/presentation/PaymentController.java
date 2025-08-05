package kr.hhplus.be.server.payment.presentation;

import kr.hhplus.be.server.order.domain.OrderStatus;
import kr.hhplus.be.server.payment.application.PaymentFacade;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.domain.dto.request.PaymentRequest;
import kr.hhplus.be.server.payment.domain.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentFacade paymentFacade;

    /**
     * 결제 처리
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest request) {
        log.info("결제 처리 요청 - orderId: {}, userId: {}, amount: {}, pointAmount: {}",
                request.getOrderId(), request.getUserId(), request.getAmount(), request.getPointAmount());

        // 개별 파라미터로 전달 (Facade 패턴에 맞춤)
        paymentFacade.processPayment(
                request.getOrderId(),
                request.getUserId(),
                request.getAmount(),
                request.getPointAmount() != null ? request.getPointAmount() : 0
        );

        log.info("결제 처리 완료 - orderId: {}", request.getOrderId());

        return ResponseEntity.ok().build();
    }

    /**
     * 결제 실패 처리
     * POST /api/payments/failed
     */
    @PostMapping("/failed")
    public ResponseEntity<Void> saveFailedPayment(@RequestBody PaymentRequest request) {
        log.info("결제 실패 처리 요청 - orderId: {}, userId: {}, amount: {}",
                request.getOrderId(), request.getUserId(), request.getAmount());

        // 개별 파라미터로 전달 (Facade 패턴에 맞춤)
        paymentFacade.handlePaymentFailure(
                request.getOrderId(),
                request.getUserId(),
                request.getAmount()
        );

        log.info("결제 실패 처리 완료 - orderId: {}", request.getOrderId());

        return ResponseEntity.ok().build();
    }

    /**
     * 결제 정보 조회
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        log.info("결제 정보 조회 요청 - paymentId: {}", paymentId);

        PaymentResponse response = paymentFacade.getPayment(paymentId);

        log.info("결제 정보 조회 완료 - paymentId: {}", paymentId);

        return ResponseEntity.ok(response);
    }

    /**
     * 주문 ID로 결제 조회
     * GET /api/payments/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("주문 ID로 결제 정보 조회 요청 - orderId: {}", orderId);

        PaymentResponse response = paymentFacade.getPaymentByOrderId(orderId);

        log.info("주문 ID로 결제 정보 조회 완료 - orderId: {}", orderId);

        return ResponseEntity.ok(response);
    }

    /**
     * 결제 상태 업데이트
     * PATCH /api/payments/{paymentId}/status
     */
    @PatchMapping("/{paymentId}/status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable Long paymentId,
            @RequestParam PaymentStatus status) {
        log.info("결제 상태 업데이트 요청 - paymentId: {}, status: {}", paymentId, status);

        paymentFacade.updatePaymentStatus(paymentId, status);

        log.info("결제 상태 업데이트 완료 - paymentId: {}, status: {}", paymentId, status);

        return ResponseEntity.ok().build();
    }
}