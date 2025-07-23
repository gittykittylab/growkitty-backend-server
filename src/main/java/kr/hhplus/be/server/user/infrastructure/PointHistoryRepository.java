package kr.hhplus.be.server.user.infrastructure;

import kr.hhplus.be.server.user.domain.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}
