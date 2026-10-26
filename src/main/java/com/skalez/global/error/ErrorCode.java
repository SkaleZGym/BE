package com.skalez.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "error.invalid-input"),

    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "error.member.email-duplicated"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.member.invalid-credentials"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.member.not-found"),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "error.member.suspended"),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "error.member.current-password-mismatch"),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "error.member.password-reset-token-invalid"),
    UPLOAD_INVALID_FILE(HttpStatus.BAD_REQUEST, "error.upload.invalid-file"),
    SOCIAL_LOGIN_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "error.social.not-configured"),
    SOCIAL_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "error.social.failed"),
    SOCIAL_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "error.social.email-required"),
    SOCIAL_EMAIL_IN_USE(HttpStatus.CONFLICT, "error.social.email-in-use"),
    SOCIAL_SIGNUP_EXPIRED(HttpStatus.BAD_REQUEST, "error.social.signup-expired"),
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
    COURIER_REQUIRED(HttpStatus.BAD_REQUEST, "error.order.courier-required"),
    SHIPPING_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "error.shipping.not-available"),

    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "error.coupon.not-found"),
    COUPON_CODE_DUPLICATED(HttpStatus.CONFLICT, "error.coupon.code-duplicated"),
    COUPON_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "error.coupon.not-available"),
    COUPON_MIN_ORDER_NOT_MET(HttpStatus.BAD_REQUEST, "error.coupon.min-order-not-met"),
    COUPON_USAGE_EXCEEDED(HttpStatus.CONFLICT, "error.coupon.usage-exceeded"),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.inquiry.not-found"),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "error.inquiry.already-answered"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "error.review.not-found"),
    REVIEW_NOT_PURCHASED(HttpStatus.FORBIDDEN, "error.review.not-purchased"),
    REVIEW_DUPLICATED(HttpStatus.CONFLICT, "error.review.duplicated"),
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.banner.not-found"),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.notice.not-found"),
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "error.faq.not-found");
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
