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
    private final List<ReservationSeat> reservationSeats;
    private final long amount;
    private final LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;

    private Reservation(Long id, String userId, String reservationCode, List<ReservationSeat> reservationSeats, long amount, LocalDateTime expiresAt, LocalDateTime confirmedAt) {
        UserPolicy.validateUser(userId);
        validateReservationCode(reservationCode);
        validateReservationSeat(reservationSeats);
        validateAmount(amount);
        validateExpiresAt(expiresAt);
        validateConfirmedAt(confirmedAt);

        this.id = id;
        this.userId = userId;
        this.reservationCode = reservationCode;
        this.reservationSeats = reservationSeats;
        this.amount = amount;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
    }

    // 팩토리 메서드 - 새로운 예약 생성
    public static Reservation create(String userId, String reservationCode, List<ReservationSeat> reservationSeats, long amount) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10); // 예약 점유 시간은 10분으로 함

        return new Reservation(null, userId, reservationCode, reservationSeats, amount, expiresAt, null);
    }

    // 기존 예약 복원 (DB에서 조회한 데이터로)
    public static Reservation of(Long id, String userId, String reservationCode, List<ReservationSeat> reservationSeats, long amount, LocalDateTime expiresAt, LocalDateTime confirmedAt) {
        return new Reservation(id, userId, reservationCode, reservationSeats, amount, expiresAt, confirmedAt);
    }

    // 비즈니스 메서드들 (상태를 변경하지 않고 새 객체 반환)
    public Reservation confirmPayment() {
        validateCanConfirm();
        List<ReservationSeat> confirmedSeats = reservationSeats.stream()
                .map(ReservationSeat::confirm)
                .collect(Collectors.toList());
        LocalDateTime now = LocalDateTime.now();

        return new Reservation(id, userId, reservationCode, confirmedSeats, amount, expiresAt, now);
    }

    public Reservation cancel() {
        List<ReservationSeat> canceledSeats = reservationSeats.stream()
                .map(ReservationSeat::cancel)
                .collect(Collectors.toList());
        LocalDateTime now = LocalDateTime.now();

        return new Reservation(id, userId, reservationCode, canceledSeats, amount, expiresAt, now);
    }

    public Reservation expire() {
        List<ReservationSeat> expiredSeats = reservationSeats.stream()
                .map(ReservationSeat::expire)
                .collect(Collectors.toList());
        LocalDateTime now = LocalDateTime.now();

        return new Reservation(id, userId, reservationCode, expiredSeats, amount, now, null);
    }

    public Reservation confirmSeats(List<Long> seatIds) {
        List<ReservationSeat> confirmedSeats = reservationSeats.stream()
                .map(ReservationSeat::confirm)
                .toList();
        LocalDateTime now = LocalDateTime.now();

        return new Reservation(id, userId, reservationCode, confirmedSeats, amount, expiresAt, now);
    }

    // 검증 메서드
    private void validateCanConfirm() {
        if (isExpired())
            throw new IllegalStateException("예약이 만료되었습니다.");

        if (reservationSeats.isEmpty())
            throw new IllegalStateException("예약할 좌석이 없습니다.");

        if (!allSeatsAreHold())
            throw new IllegalStateException("모든 좌석이 HOLD 상태가 아닙니다.");
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean allSeatsAreConfirmed() {
        return reservationSeats.stream().allMatch(ReservationSeat::isConfirmed);
    }

    public boolean allSeatsAreHold() {
        return reservationSeats.stream().allMatch(ReservationSeat::isHold);
    }

    private void validateReservationCode(String reservationCode) {
        if(reservationCode == null || reservationCode.isEmpty())
            throw new IllegalStateException("reservationCode는 반드시 존재해야합니다.");
    }

    private void validateReservationSeat(List<ReservationSeat> reservationSeats) {
        if(reservationSeats == null || reservationSeats.isEmpty())
            throw new IllegalStateException("예약좌석은 반드시 존재해야합니다.");
    }

    private static void validateAmount(long amount) {
        if (amount < 0)
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
    }

    private static void validateExpiresAt(LocalDateTime expiresAt) {
        if(LocalDateTime.now().isAfter(expiresAt))
            throw new IllegalStateException("만료시간은 현재시간 보다 이전이여야 합니다.");
    }

    private static void validateConfirmedAt(LocalDateTime confirmedAt) {
        if(confirmedAt != null && LocalDateTime.now().isBefore(confirmedAt))
            throw new IllegalStateException("완료시간은 현재시간 보다 이후여야 합니다.");
    }
}

