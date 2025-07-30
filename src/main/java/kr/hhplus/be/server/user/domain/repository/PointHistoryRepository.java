package kr.hhplus.be.server.user.domain.repository;

import kr.hhplus.be.server.user.domain.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository{
    void save(PointHistory history);
}
