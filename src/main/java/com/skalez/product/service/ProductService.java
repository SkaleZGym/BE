package com.skalez.product.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.web.PageResponse;
import com.skalez.product.api.ProductResponse;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductStatus;
import com.skalez.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /** categoryCode가 없으면 전체 상품 */
    public PageResponse<ProductResponse> getProducts(String categoryCode, Pageable pageable, Locale locale) {
        SupportedLanguage language = SupportedLanguage.from(locale);
        Page<Product> products = (categoryCode == null || categoryCode.isBlank())
                ? productRepository.findByStatus(ProductStatus.ON_SALE, pageable)
                : productRepository.findByCategoryCode(categoryCode, ProductStatus.ON_SALE, pageable);
        return PageResponse.from(products.map(product -> ProductResponse.of(product, language)));
    }

    public ProductResponse getProduct(Long id, Locale locale) {
        return productRepository.findByIdAndStatus(id, ProductStatus.ON_SALE)
                .map(product -> ProductResponse.of(product, SupportedLanguage.from(locale)))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}