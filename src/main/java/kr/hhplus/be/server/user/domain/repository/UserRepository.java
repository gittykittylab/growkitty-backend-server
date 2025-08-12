package kr.hhplus.be.server.user.domain.repository;

import kr.hhplus.be.server.user.domain.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long userId);

    Optional<User> findByIdWithPessimisticLock(Long userId);
}
