package com.skalez.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannerText {
    @Column(name = "headline", length = 200)
    private String headline;
    @Column(name = "subtext", length = 500)
    private String subtext;
    /** 가격 문구 (예: ₩69,000 · $49). 시장마다 달라서 언어별로 따로 적는다 */
    @Column(name = "price", length = 50)
    private String price;
    public BannerText(String headline, String subtext, String price){
        this.headline = headline;
        this.subtext = subtext;
        this.price = price;
    }
}
