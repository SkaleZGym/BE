package com.skalez.cart.service;

import com.skalez.cart.api.CartDtos.AddCartItemRequest;
import com.skalez.cart.api.CartDtos.CartItemResponse;
import com.skalez.cart.api.CartDtos.CartResponse;
import com.skalez.cart.domain.CartItem;
import com.skalez.cart.repository.CartItemRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.money.Money;
import com.skalez.shipping.service.ShippingFeeService;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductStatus;
import com.skalez.product.domain.ProductVariant;
import com.skalez.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ShippingFeeService shippingFeeService;


    /** 가격은 담을 때가 아니라 볼 때마다 현재 가격으로 계산한다 */
    public CartResponse getCart(Long memberId, Locale locale) {
        SupportedLanguage language = SupportedLanguage.from(locale);
        String currency = language.currency();

        List<CartItemResponse> items = cartItemRepository.findByMemberIdOrderByCreatedAtAsc(memberId).stream()
                .map(item -> toResponse(item, language))
                .toList();

        Money itemsAmount = items.stream()
                .map(item -> Money.of(item.lineTotal(), currency))
                .reduce(Money.zero(currency), Money::plus);
        Money shippingFee = shippingFeeService.estimate(itemsAmount);

        return new CartResponse(
                items,
                currency,
                itemsAmount.getAmount(),
                shippingFee.getAmount(),
                itemsAmount.plus(shippingFee).getAmount()
        );
    }

    /** 같은 옵션을 또 담으면 수량을 더한다 */
    @Transactional
    public void addItem(Long memberId, AddCartItemRequest request) {
        ProductVariant variant = variantRepository.findById(request.variantId())
                .filter(found -> found.getProduct().getStatus() == ProductStatus.ON_SALE)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        cartItemRepository.findByMemberIdAndVariantId(memberId, variant.getId())
                .ifPresentOrElse(
                        item -> item.changeQuantity(checkStock(variant, item.getQuantity() + request.quantity())),
                        () -> cartItemRepository.save(
                                new CartItem(memberId, variant, checkStock(variant, request.quantity())))
                );
    }

    @Transactional
    public void changeQuantity(Long memberId, Long cartItemId, int quantity) {
        CartItem item = findMyItem(memberId, cartItemId);
        item.changeQuantity(checkStock(item.getVariant(), quantity));
    }

    @Transactional
    public void removeItem(Long memberId, Long cartItemId) {
        cartItemRepository.delete(findMyItem(memberId, cartItemId));
    }

    private CartItem findMyItem(Long memberId, Long cartItemId) {
        return cartItemRepository.findByIdAndMemberId(cartItemId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private int checkStock(ProductVariant variant, int quantity) {
        if (variant.getStock() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        return quantity;
    }

    private CartItemResponse toResponse(CartItem item, SupportedLanguage language) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        Money unitPrice = product.priceIn(language.currency());
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                variant.getId(),
                product.translationFor(language).getName(),
                variant.getSize(),
                product.getImageUrl(),
                unitPrice.getAmount(),
                item.getQuantity(),
                unitPrice.times(item.getQuantity()).getAmount(),
                variant.getStock()
        );
    }
}