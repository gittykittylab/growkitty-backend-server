package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.domain.repository.TopProductRepository;
import kr.hhplus.be.server.product.domain.TopProductView;
import kr.hhplus.be.server.product.infrastructure.repository.TopProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TopProductRepositoryImpl implements TopProductRepository {
    private final TopProductJpaRepository topProductJpaRepository;

    @Override
    public List<TopProductView> findAll() {
        return topProductJpaRepository.findAll();
    }
}
