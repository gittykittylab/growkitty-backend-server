package kr.hhplus.be.server.testdata;

import kr.hhplus.be.server.coupon.infrastructure.repository.CouponJpaRepository;
import kr.hhplus.be.server.order.infrastructure.repository.OrderJpaRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.infrastructure.repository.PaymentJpaRepository;
import kr.hhplus.be.server.product.infrastructure.repository.ProductJpaRepository;
import kr.hhplus.be.server.user.infrastructure.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("payment-test")
@RequiredArgsConstructor
public class PaymentTestDataLoader implements ApplicationRunner {
    private final PaymentJpaRepository paymentJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final OrderJpaRepository orderJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final CouponJpaRepository couponJpaRepository;

    public static final Long USER_ID_1 = 2001L;
    public static final Long USER_ID_2 = 2002L;

    public static final Long ORDER_ID_POINT_USED = 1001L;
    public static final Long ORDER_ID_CANCELLED = 1002L;
    public static final Long ORDER_ID_COUPON_USED = 1003L;
    public static final Long ORDER_ID_FAILED = 1004L;
    public static final Long ORDER_ID_PURE_PAYMENT = 1005L;

    public static final Long COUPON_ID = 1L;

    public static final int AMOUNT_1 = 10000;
    public static final int AMOUNT_2 = 15000;
    public static final int AMOUNT_3 = 5000;

    public static final int POINT_USED_AMOUNT = 3000;
    public static final int DISCOUNT_AMOUNT = 2000;

    @Override
    public void run(ApplicationArguments args) {
        createAllTestPayments();
    }

    private void createAllTestPayments() {

        // 취소된 결제
        createPayment(
                USER_ID_2,
                ORDER_ID_CANCELLED,
                null,
                AMOUNT_2,
                0,
                0,
                PaymentStatus.CANCELLED,
                LocalDateTime.now().minusDays(2)
        );

        // 쿠폰 사용 결제
        createPayment(
                USER_ID_1,
                ORDER_ID_COUPON_USED,
                COUPON_ID,
                AMOUNT_1,
                0,
                2000,
                PaymentStatus.PAID,
                LocalDateTime.now().minusDays(3)
        );


        // 순수 결제 (할인/포인트 없음)
        createPayment(
                USER_ID_2,
                ORDER_ID_PURE_PAYMENT,
                null,
                AMOUNT_3,
                0,
                0,
                PaymentStatus.PAID,
                LocalDateTime.now().minusDays(5)
        );
    }

    private void createPayment(Long userId, Long orderId, Long couponId,
                               int paidAmount, int pointUsedAmount, int discountAmount,
                               PaymentStatus status, LocalDateTime paidAt) {

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId(orderId);
        payment.setCouponId(couponId);
        payment.setPaidAmount(paidAmount);
        payment.setPointUsedAmount(pointUsedAmount);
        payment.setAppliedDiscountAmount(discountAmount);
        payment.setPaymentStatus(status);
        payment.setPaidAt(paidAt);

        paymentJpaRepository.save(payment);
    }
}