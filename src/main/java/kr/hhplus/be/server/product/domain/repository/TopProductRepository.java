package kr.hhplus.be.server.product.domain.repository;

import kr.hhplus.be.server.product.domain.TopProductView;

import java.util.List;

public interface TopProductRepository {
    List<TopProductView> findAll();
}
