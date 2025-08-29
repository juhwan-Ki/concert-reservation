package com.gomdol.concert.concert.presentation.dto;

import java.util.List;

public record ConcertPage(
        List<ConcertResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
