package com.gomdol.concert.point.presentation.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PointHistoryPage(
        List<PointHistoryItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        OffsetDateTime snapshotAt
) {
}
