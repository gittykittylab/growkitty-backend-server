package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.domain.TopProductRepository;
import kr.hhplus.be.server.product.domain.TopProductView;
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
