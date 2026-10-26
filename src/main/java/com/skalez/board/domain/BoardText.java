package com.skalez.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardText {
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "content", nullable = false, length = 10000)
    private String content;
    public BoardText(String title, String content){
        this.title = title;
        this.content = content;
    }
}
