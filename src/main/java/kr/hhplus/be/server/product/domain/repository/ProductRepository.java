package kr.hhplus.be.server.product.domain.repository;


import kr.hhplus.be.server.product.domain.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> findAll();

    Optional<Product> findById(Long productId);

    void save(Product product);

}
