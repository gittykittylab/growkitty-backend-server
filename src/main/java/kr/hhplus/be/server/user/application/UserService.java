package kr.hhplus.be.server.user.application;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
import kr.hhplus.be.server.user.domain.PointHistory;
import kr.hhplus.be.server.user.domain.dto.response.PointBalanceResponse;
import kr.hhplus.be.server.user.domain.repository.PointHistoryRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
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

    @Transactional
    public void chargePointWithPessimisticLock(Long userId, int amount) {
        // 비관적 락을 사용하여 사용자 조회
        User user = userRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        // 포인트 충전
        user.chargePoint(amount);

        // 포인트 충전 이력 저장
        PointHistory history = PointHistory.createChargeHistory(userId, amount);
        pointHistoryRepository.save(history);
    }

    @Transactional
    public void usePointWithPessimisticLock(Long userId, int amount) {
        // 비관적 락을 사용하여 사용자 조회
        User user = userRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        // 포인트 사용
        user.usePoint(amount);

        // 포인트 사용 이력 저장
        PointHistory history = PointHistory.createUseHistory(userId, amount);
        pointHistoryRepository.save(history);
    }

//    // 낙관적 락이 적용된 포인트 충전 기능
//    @Transactional
//    public void chargePointWithOptimisticLock(Long userId, int amount, int maxRetries) {
//        int retryCount = 0;
//
//        while (true) {
//            try {
//                User user = userRepository.findById(userId)
//                        .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
//
//                // 포인트 충전
//                user.chargePoint(amount);
//
//                // 포인트 충전 이력 저장
//                PointHistory history = PointHistory.createChargeHistory(userId, amount);
//                pointHistoryRepository.save(history);
//
//                return; // 성공 시 종료
//            } catch (OptimisticLockingFailureException e) {
//                retryCount++;
//                if (retryCount >= maxRetries) {
//                    throw new RuntimeException("포인트 충전 중 충돌이 발생했습니다. 최대 재시도 횟수를 초과했습니다.", e);
//                }
//
//                // 잠시 대기 후 재시도
//                try {
//                    long backoffTime = (long) (Math.pow(2, retryCount) * 100);
//                    Thread.sleep(backoffTime);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException("포인트 충전 재시도 중 인터럽트가 발생했습니다.", ie);
//                }
//            }
//        }
//    }
//
//    // 낙관적 락이 적용된 포인트 차감 기능
//    @Transactional
//    public void usePointWithOptimisticLock(Long userId, int amount, int maxRetries) {
//        int retryCount = 0;
//
//        while (true) {
//            try {
//                User user = userRepository.findById(userId)
//                        .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
//
//                // 포인트 사용
//                user.usePoint(amount);
//
//                // 포인트 사용 이력 저장
//                PointHistory history = PointHistory.createUseHistory(userId, amount);
//                pointHistoryRepository.save(history);
//
//                return; // 성공 시 종료
//            } catch (OptimisticLockingFailureException e) {
//                retryCount++;
//                if (retryCount >= maxRetries) {
//                    throw new RuntimeException("포인트 사용 중 충돌이 발생했습니다. 최대 재시도 횟수를 초과했습니다.", e);
//                }
//
//                // 잠시 대기 후 재시도
//                try {
//                    long backoffTime = (long) (Math.pow(2, retryCount) * 100);
//                    Thread.sleep(backoffTime);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException("포인트 사용 재시도 중 인터럽트가 발생했습니다.", ie);
//                }
//            }
//        }
//    }
}
