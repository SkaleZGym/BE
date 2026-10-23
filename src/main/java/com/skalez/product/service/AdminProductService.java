package com.skalez.product.service;

import com.skalez.category.domain.Category;
import com.skalez.category.repository.CategoryRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.web.PageResponse;
import com.skalez.product.api.AdminProductDtos.AdminProductResponse;
import com.skalez.product.api.AdminProductDtos.ProductSaveRequest;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductStatus;
import com.skalez.product.domain.ProductVariant;
import com.skalez.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /** status가 없으면 숨긴 상품까지 전체 */
    public PageResponse<AdminProductResponse> getProducts(ProductStatus status, Pageable pageable) {
        Page<Product> products = status == null
                ? productRepository.findAll(pageable)
                : productRepository.findByStatus(status, pageable);
        return PageResponse.from(products.map(AdminProductResponse::from));
    }

    public AdminProductResponse getProduct(Long productId) {
        return AdminProductResponse.from(find(productId));
    }

    @Transactional
    public AdminProductResponse create(ProductSaveRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException(ErrorCode.SKU_DUPLICATED);
        }
        Product product = new Product(request.sku(), findCategory(request.categoryCode()), request.imageUrl());
        apply(product, request);
        return AdminProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public AdminProductResponse update(Long productId, ProductSaveRequest request) {
        Product product = find(productId);
        if (!product.getSku().equals(request.sku())) {
            if (productRepository.existsBySku(request.sku())) {
                throw new BusinessException(ErrorCode.SKU_DUPLICATED);
            }
            product.changeSku(request.sku());
        }
        product.changeInfo(findCategory(request.categoryCode()), request.imageUrl());
        apply(product, request);
        return AdminProductResponse.from(product);
    }

    /** 삭제 대신 숨김(HIDDEN). 주문 내역·장바구니가 상품을 참조하고 있어서 지우지 않는다 */
    @Transactional
    public AdminProductResponse changeStatus(Long productId, ProductStatus status) {
        Product product = find(productId);
        product.changeStatus(status);
        return AdminProductResponse.from(product);
    }

    @Transactional
    public AdminProductResponse changeStock(Long productId, Long variantId, int stock) {
        Product product = find(productId);
        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));
        variant.changeStock(stock);
        return AdminProductResponse.from(product);
    }

    /** 등록·수정 공통: 번역, 가격, 사이즈 재고, 뱃지, 상태를 요청 내용대로 맞춘다 */
    private void apply(Product product, ProductSaveRequest request) {
        if (!request.translations().containsKey(SupportedLanguage.DEFAULT.tag())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT); // 한국어 이름은 필수
        }
        request.translations().forEach((tag, t) ->
                product.putTranslation(SupportedLanguage.fromTag(tag), t.name(), t.description()));

        product.clearPrices();
        request.prices().forEach((currency, price) ->
                product.putPrice(currency, price.amount(), price.listAmount()));

        request.variants().forEach(v -> product.upsertVariant(v.size(), v.stock()));
        product.replaceBadges(request.badges() == null ? Set.of() : request.badges());
        product.changeStatus(request.status());
    }

    private Product find(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category findCategory(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}