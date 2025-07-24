package kr.hhplus.be.server.payment.presentation;

import kr.hhplus.be.server.payment.application.PaymentService;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.dto.request.PaymentRequest;
import kr.hhplus.be.server.payment.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * 결제 처리
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(
                request.getOrderId(),
                // userId는 실제로는 인증된 사용자 정보에서 가져와야 함
                1L, // 임시로 하드코딩, 실제로는 SecurityContext 등에서 추출
                request.getAmount(),
                request.getPointAmount()
        );

        return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
    }
//    /**
//     * 결제 실패 처리
//     */
//    @PostMapping("/failed")
//    public ResponseEntity<PaymentResponse> saveFailedPayment(@RequestBody PaymentRequest request) {
//        Payment payment = paymentService.saveFailedPayment(
//                request.getOrderId(),
//                1L, // 임시로 하드코딩
//                request.getAmount()
//        );
//
//        return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
//    }
}
