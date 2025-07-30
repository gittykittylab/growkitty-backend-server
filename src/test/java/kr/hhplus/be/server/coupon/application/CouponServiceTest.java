package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Long policyId;
    private Long userId;
    private CouponPolicy policy;
    private Coupon expectedCoupon;

    @BeforeEach
    void setUp() {
        // 공통 테스트 데이터 초기화
        policyId = 1L;
        userId = 1L;

        policy = new CouponPolicy();
        policy.setPolicyId(policyId);
        policy.setTotalQuantity(100);

        expectedCoupon = new Coupon();
        expectedCoupon.setCouponId(1L);

        policy.setExpiredDays(30); // expiredDays 값 설정 추가
        policy.setDiscountAmount(1000);

        // 기본 모킹 설정
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void coupon_issued_success() {
        // given
        when(couponRepository.countByPolicyId(policyId)).thenReturn(0L);
        when(couponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(expectedCoupon);

        // when
        Coupon result = couponService.issueFirstComeCoupon(policyId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCouponId()).isEqualTo(expectedCoupon.getCouponId());
    }

    @Test
    @DisplayName("쿠폰 소진 시 예외발생")
    void coupon_sold_out() {
        // given
        when(couponRepository.countByPolicyId(policyId)).thenReturn(100L);

        // when & then
        assertThatThrownBy(() -> couponService.issueFirstComeCoupon(policyId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("쿠폰이 모두 소진되었습니다.");
    }
    @Test
    @DisplayName("중복 발급 시 예외발생")
    void coupon_already_issued() {
        // given
        when(couponRepository.countByPolicyId(policyId)).thenReturn(50L);
        when(couponRepository.existsByUserIdAndPolicyId(userId, policyId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> couponService.issueFirstComeCoupon(policyId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 발급받은 쿠폰입니다.");
    }
}