package kr.hhplus.be.server.product.domain;


import kr.hhplus.be.server.product.domain.dto.response.TopProductResponse;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> findAll();

    Optional<Product> findById(Long productId);

    void save(Product product);

}
