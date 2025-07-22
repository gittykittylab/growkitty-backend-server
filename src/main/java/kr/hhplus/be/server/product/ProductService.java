package kr.hhplus.be.server.product;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    //상품 목록 조회
    public List<ProductResponse> getProducts(){
        List<Product> products = productRepository.findAll();
        return  products.stream()
                .map(product -> new ProductResponse(product))
                .collect(Collectors.toList());
    }
}
