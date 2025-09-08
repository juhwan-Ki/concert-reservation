package com.gomdol.concert.concert.domain;

import lombok.Getter;

@Getter
public enum ConcertStatus {
    PRIVATE("비공개"), PUBLIC("공개"), HIDDEN("숨김"), CANCELED("취소");

    private final String desc;

    private ConcertStatus(String desc) {
        this.desc = desc;
    }
}
