package kr.hhplus.be.server.user.infrastructure;

import kr.hhplus.be.server.user.domain.PointHistory;
import kr.hhplus.be.server.user.domain.repository.PointHistoryRepository;
import kr.hhplus.be.server.user.infrastructure.repository.PointHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {
    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public void save(PointHistory history) {
        pointHistoryJpaRepository.save(history);
    }
}
