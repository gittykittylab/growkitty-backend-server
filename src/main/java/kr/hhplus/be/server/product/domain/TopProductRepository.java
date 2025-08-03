package kr.hhplus.be.server.product.domain;

import java.util.List;

public interface TopProductRepository {
    List<TopProductView> findAll();
}
