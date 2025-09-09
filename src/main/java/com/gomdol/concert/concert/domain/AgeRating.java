package com.gomdol.concert.concert.domain;

import lombok.Getter;

@Getter
public enum AgeRating {
    ALL("전체관람가"),
    AGE_12("만 12세 이상"),
    AGE_15("만 15세 이상"),
    AGE_19("만 19세 이상");

    private final String desc;

    AgeRating(String desc) {
        this.desc = desc;
    }
}
