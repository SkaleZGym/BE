package com.skalez.order.domain;

import com.skalez.global.security.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 해외 주소도 담을 수 있게 국가코드 + 자유 형식 주소 2줄.
 * 개인정보(받는 사람·전화번호·우편번호·주소)는 AES-256으로 암호화해서 저장한다.
 * 암호문은 원문보다 길어서 컬럼 길이를 넉넉하게 잡는다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress {

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String recipient;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String phone;

    @Column(nullable = false, length = 2)
    private String countryCode; // ISO 3166: KR, US, JP, CN (통계·배송비 계산용이라 암호화하지 않음)

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String postalCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 500)
    private String address1;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 500)
    private String address2;

    public ShippingAddress(String recipient, String phone, String countryCode,
                           String postalCode, String address1, String address2) {
        this.recipient = recipient;
        this.phone = phone;
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.address1 = address1;
        this.address2 = address2;
    }
}