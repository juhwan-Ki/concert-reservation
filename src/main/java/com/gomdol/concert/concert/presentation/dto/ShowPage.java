package com.gomdol.concert.concert.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공연 페이지 응답")
public record ShowPage(
        List<ShowResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

}
