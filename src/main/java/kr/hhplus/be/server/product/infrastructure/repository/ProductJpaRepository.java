package kr.hhplus.be.server.product.infrastructure.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId")
    Optional<Product> findByIdWithPessimisticLock(@Param("productId") Long productId);

    List<Product> findAllById(List<Long> topProductsIdsLong);
}
