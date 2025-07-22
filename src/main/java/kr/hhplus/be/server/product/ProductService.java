package kr.hhplus.be.server.product;

import kr.hhplus.be.server.common.exception.EntityNotFoundException;
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

    // 상품 상세 조회
    public ProductDetailResponse getProductById(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("상품을 찾을 수 없습니다."));
        return new ProductDetailResponse(product);
    }
}
