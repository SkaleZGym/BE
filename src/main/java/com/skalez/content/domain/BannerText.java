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
    public BannerText(String headline, String subtext){
        this.headline = headline;
        this.subtext = subtext;
    }
}
