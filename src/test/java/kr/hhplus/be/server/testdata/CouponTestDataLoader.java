package kr.hhplus.be.server.testdata;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.infrastructure.repository.CouponPolicyJpaRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("test")
@RequiredArgsConstructor
public class CouponTestDataLoader implements ApplicationRunner {

    private final CouponPolicyJpaRepository couponPolicyRepository;
    private final UserJpaRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 이미 데이터가 있는지 확인
        if (couponPolicyRepository.count() > 0) {
            return;
        }

        // 테스트용 쿠폰 정책 생성 (일반 정책)
        CouponPolicy normalPolicy = new CouponPolicy();
        normalPolicy.setDiscountAmount(1000);
        normalPolicy.setExpiredDays(30);
        normalPolicy.setTotalQuantity(100);
        normalPolicy.setCreatedAt(LocalDateTime.now());
        normalPolicy.setUpdatedAt(LocalDateTime.now());
        couponPolicyRepository.save(normalPolicy);

        // 테스트용 쿠폰 정책 생성 (소진 테스트용 - 1개만 발급 가능)
        CouponPolicy limitedPolicy = new CouponPolicy();
        limitedPolicy.setDiscountAmount(2000);
        limitedPolicy.setExpiredDays(14);
        limitedPolicy.setTotalQuantity(1);
        limitedPolicy.setCreatedAt(LocalDateTime.now());
        limitedPolicy.setUpdatedAt(LocalDateTime.now());
        couponPolicyRepository.save(limitedPolicy);

        // 테스트용 사용자 생성 (2명)
        if (userRepository.count() < 2) {
            User user1 = new User();
            user1.setPointBalance(5000);
            userRepository.save(user1);

            User user2 = new User();
            user2.setPointBalance(5000);
            userRepository.save(user2);
        }
    }
}