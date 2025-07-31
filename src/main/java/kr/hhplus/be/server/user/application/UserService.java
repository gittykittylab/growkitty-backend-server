package kr.hhplus.be.server.user.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.user.domain.PointHistory;
import kr.hhplus.be.server.user.domain.dto.response.PointBalanceResponse;
import kr.hhplus.be.server.user.domain.PointHistoryRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 포인트 잔액 조회
    public PointBalanceResponse getPointBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        return new PointBalanceResponse(user.getPointBalance());
    }
    // 포인트 충전
    @Transactional
    public void chargePoint(Long userId, int amount){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        // 포인트 충전
        user.chargePoint(amount);

        // 포인트 충전 이력 저장
        PointHistory history = PointHistory.createChargeHistory(userId, amount);

        pointHistoryRepository.save(history);
    }

    // 포인트 사용
    @Transactional
    public void usePoint(Long userId, int amount){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        // 포인트 사용
        user.usePoint(amount);

        // 포인트 사용 이력 저장
        PointHistory history = PointHistory.createUseHistory(userId, amount);

        pointHistoryRepository.save(history);
    }
}
