package com.gomdol.concert.venue.domain.model;

import lombok.Getter;

@Getter
public class VenueSeat {
    private final Long id;
    private final Long venueId;
    private final String seatLabel;
    private final String rowLabel;
    private final int seatNumber;
    private final long price;

    private VenueSeat(Long id, Long venueId, String seatLabel, String rowLabel, int seatNumber, long price) {
        this.venueId = requirePositive(venueId, "venueId");
        this.rowLabel = normalizeRow(rowLabel);
        this.seatNumber = requirePositive(seatNumber, "seatNumber");
        this.price = requireNonNegative(price, "price");

        String composed = composeSeatLabel(this.rowLabel, this.seatNumber);
        if (seatLabel == null || !seatLabel.equals(composed))
            throw new IllegalArgumentException("seatLabel은 rowLabel/seatNumber 규칙과 일치해야 합니다. expected=" + composed);

        this.id = id;
        this.seatLabel = seatLabel;
    }

    public static VenueSeat create(Long venueId, String rowLabel, int seatNumber, long price) {
        return new VenueSeat(null, venueId, composeSeatLabel(rowLabel, seatNumber), rowLabel, seatNumber, price);
    }

    public static VenueSeat of(Long id, Long venueId, String seatLabel, String rowLabel, int seatNumber, long price) {
        return new VenueSeat(id, venueId, seatLabel, rowLabel, seatNumber, price);
    }

    private static String composeSeatLabel(String rowLabel, int seatNumber) {
        return normalizeRow(rowLabel) + "-" + seatNumber;
    }

    private static String normalizeRow(String row) {
        if (row == null)
            throw new IllegalArgumentException("rowLabel은 null일 수 없습니다.");
        String r = row.trim().toUpperCase();
        if (!r.matches("^[A-Z][A-Z0-9]{0,3}$"))
            throw new IllegalArgumentException("rowLabel 형식이 올바르지 않습니다. 예: A, B1, AA, A12");

        return r;
    }

    private static Long requirePositive(Long v, String name) {
        if (v == null || v <= 0) throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        return v;
    }

    private static long requireNonNegative(long v, String name) {
        if (v < 0) throw new IllegalArgumentException(name + "는 0 이상이어야 합니다.");
        return v;
    }

    private static int requirePositive(int v, String name) {
        if (v <= 0) throw new IllegalArgumentException(name + "는 1 이상이어야 합니다.");
        return v;
    }
}
