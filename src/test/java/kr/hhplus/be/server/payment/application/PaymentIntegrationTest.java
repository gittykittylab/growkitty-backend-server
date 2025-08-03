package kr.hhplus.be.server.payment.application;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.infrastructure.repository.PaymentJpaRepository;
import kr.hhplus.be.server.testdata.PaymentTestDataLoader;
import kr.hhplus.be.server.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({TestcontainersConfiguration.class, PaymentTestDataLoader.class})
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("결제 처리 성공 - 순수 결제")
    void processPayment_Success() {
        // given
        Long orderId = PaymentTestDataLoader.ORDER_ID_PURE_PAYMENT;
        Long userId = PaymentTestDataLoader.USER_ID_2;
        int totalAmount = PaymentTestDataLoader.AMOUNT_3;
        int pointAmount = 0;

        // when
        Payment payment = paymentService.processPayment(orderId, userId, totalAmount, pointAmount);

        // then
        assertNotNull(payment);
        assertEquals(orderId, payment.getOrderId());
        assertEquals(userId, payment.getUserId());
        assertEquals(totalAmount, payment.getPaidAmount());
        assertEquals(pointAmount, payment.getPointUsedAmount());
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
    }

    @Test
    @DisplayName("결제 실패 정보 저장 성공")
    void saveFailedPayment_Success() {
        // given
        Long orderId = PaymentTestDataLoader.ORDER_ID_CANCELLED; // 고유한 테스트 전용 ID
        Long userId = PaymentTestDataLoader.USER_ID_1;
        int totalAmount = 25000;

        // when
        Payment payment = paymentService.saveFailedPayment(orderId, userId, totalAmount);

        // then
        assertNotNull(payment);
        assertEquals(orderId, payment.getOrderId());
        assertEquals(userId, payment.getUserId());
        assertEquals(totalAmount, payment.getPaidAmount());
        assertEquals(0, payment.getPointUsedAmount());
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
    }

    @Test
    @DisplayName("결제 실패 처리")
    void handlePaymentFailure_Success() {
        // given
        Long orderId = PaymentTestDataLoader.ORDER_ID_FAILED;
        Long userId = PaymentTestDataLoader.USER_ID_1;
        int totalAmount = PaymentTestDataLoader.AMOUNT_1;

        // when
        paymentFacade.handlePaymentFailure(orderId, userId, totalAmount);

        // then
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        assertNotNull(payment);
        assertEquals(orderId, payment.getOrderId());
        assertEquals(userId, payment.getUserId());
        assertEquals(totalAmount, payment.getPaidAmount());
        assertEquals(0, payment.getPointUsedAmount());
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
    }

    @Test
    @DisplayName("기존 결제 정보 조회 성공")
    void getExistingPayment_Success() {
        // when
        Payment payment = paymentService.getPaymentByOrderId(PaymentTestDataLoader.ORDER_ID_COUPON_USED);

        // then
        assertNotNull(payment);
        assertEquals(PaymentTestDataLoader.ORDER_ID_COUPON_USED, payment.getOrderId());
        assertEquals(PaymentTestDataLoader.USER_ID_1, payment.getUserId());
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertEquals(PaymentTestDataLoader.COUPON_ID, payment.getCouponId());
        assertEquals(PaymentTestDataLoader.AMOUNT_1, payment.getPaidAmount());
        assertEquals(PaymentTestDataLoader.DISCOUNT_AMOUNT, payment.getAppliedDiscountAmount());
    }

}
