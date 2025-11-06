package com.gomdol.concert.show.domain.model;

import lombok.Getter;

@Getter
public enum ShowStatus {
    SCHEDULED("오픈 예정"), ON_SALE("판매중"), SOLD_OUT("매진"), CANCELLED("취소");

    private final String desc;

    private ShowStatus(String desc) {
        this.desc = desc;
    }
}
