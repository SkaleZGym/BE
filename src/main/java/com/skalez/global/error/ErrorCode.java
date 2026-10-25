package com.skalez.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "error.invalid-input"),

    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "error.member.email-duplicated"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.member.invalid-credentials"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.member.not-found"),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "error.member.suspended"),
    SELF_MODIFICATION(HttpStatus.BAD_REQUEST, "error.member.self-modification"),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.category.not-found"),
    CATEGORY_CODE_DUPLICATED(HttpStatus.CONFLICT, "error.category.code-duplicated"),
    CATEGORY_PARENT_INVALID(HttpStatus.BAD_REQUEST, "error.category.parent-invalid"),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "error.category.in-use"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "error.product.not-found"),
    VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "error.product.variant-not-found"),
    PRICE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "error.product.price-not-available"),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "error.product.out-of-stock"),
    SKU_DUPLICATED(HttpStatus.CONFLICT, "error.product.sku-duplicated"),

    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "error.cart.item-not-found"),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "error.cart.empty"),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.order.not-found"),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "error.order.invalid-status"),
    TRACKING_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "error.order.tracking-required"),

    PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST, "error.payment.not-paid"),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "error.payment.amount-mismatch"),
    PAYMENT_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "error.payment.gateway"),
    PAYMENT_CURRENCY_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "error.payment.currency-not-supported"),

    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.inquiry.not-found"),
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.banner.not-found");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    public HttpStatus status() {
        return status;
    }

    public String messageKey() {
        return messageKey;
    }
}
