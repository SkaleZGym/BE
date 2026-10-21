package com.skalez.cart.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
public final class CartDtos {
    private CartDtos(){}
    public record AddCartItemRequest(@NotNull Long variantId, @Min(1) @Max(99) int quantity){}
    public record ChangeQuantityRequest(@Min(1) @Max(99) int quantity){}
    public record CartItemResponse(Long cartItemId, Long productId, Long variantId, String name, String size, String imageUrl, long unitPrice, int quantity, long lineTotal, int stock){}
    public record CartResponse(List<CartItemResponse> items, String currency, long itemsAmount, long shippingFee, long totalAmount){}
}
