package com.gomdol.concert.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageableUtils {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 20;

    public static Pageable sanitize(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = pageable.getPageSize() <= 0 ? DEFAULT_SIZE : Math.min(pageable.getPageSize(), MAX_SIZE);
        return PageRequest.of(page, size, pageable.getSort());
    }
}
