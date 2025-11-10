package com.gomdol.concert.reservation.domain.model;

import com.gomdol.concert.user.domain.policy.UserPolicy;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 예약 도메인 객체 (순수 자바, 프레임워크 의존성 없음)
@Getter
public class Reservation {

    private final Long id;
    private final String userId;
    private final String reservationCode;
    private final String requestId;
    private List<ReservationSeat> reservationSeats;
    private final long amount;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;

    private Reservation(Long id, String userId, String reservationCode, String requestId, List<ReservationSeat> reservationSeats, long amount, LocalDateTime expiresAt, LocalDateTime confirmedAt) {
        UserPolicy.validateUser(userId);
        validateReservationCode(reservationCode);
        validateReservationSeat(reservationSeats);
        validateAmount(amount);
//        validateExpiresAt(expiresAt);
        validateConfirmedAt(confirmedAt);

        this.id = id;
        this.userId = userId;
        this.reservationCode = reservationCode;
        this.requestId = requestId;
        this.reservationSeats = reservationSeats;
        this.amount = amount;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
    }

    // 팩토리 메서드 - 새로운 예약 생성
    public static Reservation create(String userId, String reservationCode, String requestId, List<ReservationSeat> reservationSeats, long amount, int holdMinutes) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(holdMinutes); // 예약 점유 시간은 10분으로 함
        return new Reservation(null, userId, reservationCode, requestId, reservationSeats, amount, expiresAt, null);
    }

    // 기존 예약 복원 (DB에서 조회한 데이터로)
    public static Reservation of(Long id, String userId, String reservationCode, String requestId, List<ReservationSeat> reservationSeats, long amount, LocalDateTime expiresAt, LocalDateTime confirmedAt) {
        return new Reservation(id, userId, reservationCode, requestId, reservationSeats, amount, expiresAt, confirmedAt);
    }

    // 비즈니스 메서드
    public void cancelSeats() {
        validateCanCancel();
        this.reservationSeats = reservationSeats.stream()
                .map(ReservationSeat::cancel)
                .collect(Collectors.toList());
        this.expiresAt = null;
        this.confirmedAt = LocalDateTime.now();
    }

    public void expireSeats() {
        this.reservationSeats = reservationSeats.stream()
                .map(ReservationSeat::expire)
                .collect(Collectors.toList());
    }

    public void confirmSeats() {
        validateCanConfirm();

        this.reservationSeats = reservationSeats.stream()
                .map(ReservationSeat::confirm)
                .toList();
        this.expiresAt = null;
        this.confirmedAt = LocalDateTime.now();
    }

    // 검증 메서드
    private void validateCanConfirm() {
        if (isExpired())
            throw new IllegalStateException("예약이 만료되었습니다.");

        if (reservationSeats.isEmpty())
            throw new IllegalStateException("예약할 좌석이 없습니다.");

        if(isConfirmed())
            throw new IllegalStateException("확정된 좌석이 있습니다.");

        if (!allSeatsAreHold())
            throw new IllegalStateException("모든 좌석이 HOLD 상태가 아닙니다.");
    }

    private void validateCanCancel() {
        if (isExpired())
            throw new IllegalStateException("예약이 만료되었습니다.");

        if (reservationSeats.isEmpty())
            throw new IllegalStateException("예약할 좌석이 없습니다.");

        if(isCancel())
            throw new IllegalStateException("이미 취소된 좌석입니다.");

        if (allSeatsAreCancel())
            throw new IllegalStateException("이미 취소되거나 만료된 좌석입니다.");
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isConfirmed() {
        return reservationSeats.stream().anyMatch(ReservationSeat::isConfirmed);
    }

    public boolean isCancel() {
        return reservationSeats.stream().anyMatch(ReservationSeat::isCanceled);
    }

    public boolean allSeatsAreConfirmed() {
        return reservationSeats.stream().allMatch(ReservationSeat::isConfirmed);
    }

    public boolean allSeatsAreHold() {
        return reservationSeats.stream().allMatch(ReservationSeat::isHold);
    }

    public boolean allSeatsAreCancel() {
        return reservationSeats.stream().allMatch(ReservationSeat::isCanceled);
    }

    public void validateAllSeatsPaymentEligible() {
        validateCanConfirm();
        validateCanCancel();
    }

    private void validateReservationCode(String reservationCode) {
        if(reservationCode == null || reservationCode.isEmpty())
            throw new IllegalArgumentException("reservationCode는 반드시 존재해야합니다.");
    }

    private void validateReservationSeat(List<ReservationSeat> reservationSeats) {
        if(reservationSeats == null || reservationSeats.isEmpty())
            throw new IllegalArgumentException("예약 좌석은 반드시 존재해야합니다.");
    }

    private static void validateAmount(long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
    }

//    private static void validateExpiresAt(LocalDateTime expiresAt) {
//        if(expiresAt != null && !expiresAt.isAfter(LocalDateTime.now()))
//            throw new IllegalStateException("만료시간은 현재시간 보다 이후여야 합니다.");
//    }

    private static void validateConfirmedAt(LocalDateTime confirmedAt) {
        if (confirmedAt != null && confirmedAt.isAfter(LocalDateTime.now()))
            throw new IllegalStateException("확정 시각은 현재 시각 이전이거나 같아야 합니다.");
    }
}

