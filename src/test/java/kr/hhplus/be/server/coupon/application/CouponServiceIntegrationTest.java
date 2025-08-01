package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.infrastructure.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.CouponPolicyJpaRepository;
import kr.hhplus.be.server.testdata.CouponTestDataLoader;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, CouponTestDataLoader.class})
@ActiveProfiles("test")
@Transactional
public class CouponServiceIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponPolicyJpaRepository couponPolicyRepository;

    @Autowired
    private CouponJpaRepository couponRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Test
    @DisplayName("선착순 쿠폰 발급 성공")
    void issueFirstComeCoupon_Success() {
        // given
        // 데이터 로더에서 생성된 정책들 중 수량이 충분한 정책 선택
        List<CouponPolicy> policies = couponPolicyRepository.findAll();
        CouponPolicy policyWithPlentyStock = policies.stream()
                .filter(p -> p.getTotalQuantity() > 10) // 수량이 충분한 정책 선택
                .findFirst()
                .orElseThrow(() -> new RuntimeException("충분한 수량의 쿠폰 정책이 없습니다"));

        // 테스트용 사용자 찾기
        List<User> users = userRepository.findAll();
        User testUser = users.get(0);

        // when
        Coupon coupon = couponService.issueFirstComeCoupon(policyWithPlentyStock.getPolicyId(), testUser.getUserId());

        // then
        assertThat(coupon).isNotNull();
        assertThat(coupon.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(coupon.getPolicyId()).isEqualTo(policyWithPlentyStock.getPolicyId());
        assertThat(coupon.getCouponStatus()).isEqualTo("AVAILABLE");
        assertThat(coupon.getExpiredAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 정책으로 발급 시도 시 예외 발생")
    void issueFirstComeCoupon_PolicyNotFound() {
        // given
        List<User> users = userRepository.findAll();
        User testUser = users.get(0);

        // 존재하지 않는 정책 ID 생성 (최대 ID + 100)
        Long maxPolicyId = couponPolicyRepository.findAll().stream()
                .mapToLong(CouponPolicy::getPolicyId)
                .max()
                .orElse(0);
        Long nonExistentPolicyId = maxPolicyId + 100;

        // when & then
        assertThatThrownBy(() -> couponService.issueFirstComeCoupon(nonExistentPolicyId, testUser.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("쿠폰 정책을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰 중복 발급 시도 시 예외 발생")
    void issueFirstComeCoupon_AlreadyIssued() {
        // given
        // 데이터 로더에서 생성된 정책들 중 수량이 충분한 정책 선택
        List<CouponPolicy> policies = couponPolicyRepository.findAll();
        CouponPolicy policy = policies.stream()
                .filter(p -> p.getTotalQuantity() > 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("테스트에 적합한 쿠폰 정책이 없습니다"));

        List<User> users = userRepository.findAll();
        User testUser = users.get(0);

        // 테스트 전에 쿠폰 발급
        couponService.issueFirstComeCoupon(policy.getPolicyId(), testUser.getUserId());

        // 발급된 쿠폰 확인
        boolean couponExists = couponRepository.existsByUserIdAndPolicyId(testUser.getUserId(), policy.getPolicyId());
        assertThat(couponExists).isTrue();

        // when & then
        // 같은 사용자가 같은 정책의 쿠폰을 다시 발급 시도
        assertThatThrownBy(() -> couponService.issueFirstComeCoupon(policy.getPolicyId(), testUser.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 발급받은 쿠폰입니다");
    }

    @Test
    @DisplayName("쿠폰 소진 시 발급 실패")
    void issueFirstComeCoupon_PolicyExhausted() {
        // given
        // 테스트용 제한된 쿠폰 정책 직접 생성 (테스트 데이터에 의존하지 않음)
        CouponPolicy veryLimitedPolicy = new CouponPolicy();
        veryLimitedPolicy.setDiscountAmount(3000); // 기존 정책과 다른 값
        veryLimitedPolicy.setExpiredDays(7);
        veryLimitedPolicy.setTotalQuantity(1); // 딱 한 개만 발급 가능
        veryLimitedPolicy.setCreatedAt(LocalDateTime.now());
        veryLimitedPolicy.setUpdatedAt(LocalDateTime.now());
        couponPolicyRepository.save(veryLimitedPolicy);

        List<User> users = userRepository.findAll();
        User user1 = users.get(0);
        User user2 = users.size() > 1 ? users.get(1) : createNewUser();

        // 첫 번째 사용자가 쿠폰 발급하여 소진
        Coupon issuedCoupon = couponService.issueFirstComeCoupon(veryLimitedPolicy.getPolicyId(), user1.getUserId());
        assertThat(issuedCoupon).isNotNull();

        // 발급된 쿠폰 수량 확인
        long issuedCount = couponRepository.countByPolicyId(veryLimitedPolicy.getPolicyId());
        assertThat(issuedCount).isEqualTo(veryLimitedPolicy.getTotalQuantity().longValue());

        // when & then
        // 두 번째 사용자가 발급 시도 시 실패
        assertThatThrownBy(() -> couponService.issueFirstComeCoupon(veryLimitedPolicy.getPolicyId(), user2.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("쿠폰이 모두 소진되었습니다");
    }

    // 필요시 새 사용자 생성 헬퍼 메소드
    private User createNewUser() {
        User newUser = new User();
        newUser.setPointBalance(5000);
        return userRepository.save(newUser);
    }
}