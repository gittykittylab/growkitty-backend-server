package kr.hhplus.be.server.product.infrastructure.repository;

import kr.hhplus.be.server.product.domain.TopProductView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopProductJpaRepository extends JpaRepository<TopProductView, Long> {
}
