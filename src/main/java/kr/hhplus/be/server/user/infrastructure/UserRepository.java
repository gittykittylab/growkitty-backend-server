package kr.hhplus.be.server.user.infrastructure;

import kr.hhplus.be.server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<User, Long> {
}
