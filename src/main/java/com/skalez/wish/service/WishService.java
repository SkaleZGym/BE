package com.skalez.wish.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.product.api.ProductResponse;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductStatus;
import com.skalez.product.repository.ProductRepository;
import com.skalez.wish.domain.WishItem;
import com.skalez.wish.repository.WishItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishService {
    private final WishItemRepository wishItemRepository;
    private final ProductRepository productRepository;
    public List<ProductResponse> getWishes(Long memberId, Locale locale){
        SupportedLanguage language = SupportedLanguage.from(locale);
        return wishItemRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream().map(WishItem::getProduct).filter(product -> product.getStatus() == ProductStatus.ON_SALE).map(product -> ProductResponse.of(product, language)).toList();
    }
    @Transactional
    public void addWish(Long memberId, Long productId){
        if(wishItemRepository.existsByMemberIdAndProductId(memberId, productId)){return;}
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ON_SALE).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        wishItemRepository.save(new WishItem(memberId, product));
    }
    @Transactional
    public void removeWish(Long memberId, Long productId){
        wishItemRepository.findByMemberIdAndProductId(memberId, productId).ifPresent(wishItemRepository::delete);
    }
}
