package com.gomdol.concert.concert.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConcertPolicies {

    public static void validateDeleted(LocalDateTime deletedAt) {
        if (deletedAt != null)
            throw new IllegalArgumentException("존재하지 않는 콘서트 입니다");
    }

    public static void validateInPeriod(LocalDate startAt, LocalDate endAt, LocalDate today) {
        if (startAt == null || endAt == null)
            throw new IllegalArgumentException("시작일과 종료일은 필수입니다.");

        boolean inRange = !today.isBefore(startAt) && !today.isAfter(endAt);
        if (!inRange)
            throw new IllegalArgumentException("콘서트 기간이 아닙니다.");
    }
}
