package kr.hhplus.be.server.user;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 포인트 충전
    @Transactional
    public void chargePoint(Long userId, int amount){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        user.chargePoint(amount);

        //이력 저장
        PointHistory history = new PointHistory();
        history.setUserId(userId);
        history.setAmount(amount);
        history.setPointType("CHARGE");
        history.setCreatedAt(LocalDateTime.now());

        pointHistoryRepository.save(history);
    }


}
