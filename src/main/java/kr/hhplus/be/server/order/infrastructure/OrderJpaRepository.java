package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository  extends JpaRepository<Order, Long> {
}
