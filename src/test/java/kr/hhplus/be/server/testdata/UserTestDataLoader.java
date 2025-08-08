package kr.hhplus.be.server.testdata;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("user-test")
@RequiredArgsConstructor
public class UserTestDataLoader implements ApplicationRunner {

    private final UserJpaRepository userJpaRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 이미 데이터가 있는지 확인
        if (userJpaRepository.count() >= 5) {
            System.out.println("User 테스트 데이터가 이미 존재합니다. 건너뜁니다.");
            return;
        }

        // 테스트용 포인트 잔액 배열
        int[] pointBalances = {1000, 5000, 100, 999500, 0};
        List<User> users = new ArrayList<>();

        for (int pointBalance : pointBalances) {
            User user = new User();
            user.setPointBalance(pointBalance);
            users.add(user);
        }

        userJpaRepository.saveAll(users);
        System.out.println("User 테스트 데이터 " + users.size() + "건 생성 완료");
    }
}