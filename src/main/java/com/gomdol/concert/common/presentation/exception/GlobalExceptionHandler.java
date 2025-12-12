package com.gomdol.concert.common.presentation.exception;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ ObjectOptimisticLockingFailureException.class, OptimisticLockException.class })
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public ErrorResponse handleOptimistic(Exception e, HttpServletRequest req) {
        log.warn("optimistic_lock_conflict userId={} reqId={} uri={} exception={} message={}",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"),
                req.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
        return ErrorResponse.of("CONFLICT", "동시에 처리되어 요청이 충돌했습니다. 같은 키로 다시 시도하세요.", req.getRequestURI());
    }

    @ExceptionHandler({
        PessimisticLockException.class,
        LockTimeoutException.class,
        CannotAcquireLockException.class,
        QueryTimeoutException.class
    })
    @ResponseStatus(HttpStatus.LOCKED) // 423
    public ErrorResponse handlePessimistic(Exception e, HttpServletRequest req) {
        log.warn("pessimistic_lock_timeout userId={} reqId={} uri={} exception={} message={}",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"),
                req.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
        return ErrorResponse.of("LOCKED", "처리 중인 요청이 있습니다. 잠시 후 다시 시도하세요.", req.getRequestURI());
    }

    @ExceptionHandler(DeadlockLoserDataAccessException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public ErrorResponse handleDeadlock(DeadlockLoserDataAccessException e, HttpServletRequest req) {
        log.error("deadlock_detected userId={} reqId={} uri={} message={}",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"),
                req.getRequestURI(), e.getMessage(), e);
        return ErrorResponse.of("DEADLOCK", "요청 처리 중 충돌이 발생했습니다. 다시 시도하세요.", req.getRequestURI());
    }

    @ExceptionHandler(LockAcquisitionException.class)
    @ResponseStatus(HttpStatus.LOCKED) // 423
    public ErrorResponse handleDistributedLock(LockAcquisitionException e, HttpServletRequest req) {
        log.warn("distributed_lock_acquisition_failed userId={} reqId={} uri={} lockKey={} waitTime={}ms",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"),
                req.getRequestURI(), e.getLockKey(), e.getWaitTime().toMillis());
        return ErrorResponse.of("LOCK_ACQUISITION_FAILED",
                "다른 요청이 처리 중입니다. 잠시 후 다시 시도하세요.",
                req.getRequestURI());
    }

    @ExceptionHandler(LockReleaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500
    public ErrorResponse handleLockRelease(LockReleaseException e, HttpServletRequest req) {
        log.error("distributed_lock_release_failed userId={} reqId={} uri={} lockKey={} message={}",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"),
                req.getRequestURI(), e.getLockKey(), e.getMessage(), e);
        return ErrorResponse.of("LOCK_RELEASE_FAILED",
                "일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요.",
                req.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class) // 잔액부족 등
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY) // 422
    public ErrorResponse handleBusiness(IllegalStateException e, HttpServletRequest req) {
        log.warn("business_violation userId={} reqId={} uri={} msg={}",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"), req.getRequestURI(), e.getMessage());
        return ErrorResponse.of("BUSINESS_ERROR", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ErrorResponse handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        log.warn("validation_error fields={}", e.getBindingResult().getFieldErrors());
        return ErrorResponse.of("BAD_REQUEST", "입력값이 올바르지 않습니다.", req.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)  // 404
    public ErrorResponse handleNotFound(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("not_found userId={} reqId={} uri={} msg={}",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"),
                req.getRequestURI(), e.getMessage());
        return ErrorResponse.of("NOT_FOUND", e.getMessage(), req.getRequestURI());
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500
    public ErrorResponse handleUnknown(Exception e, HttpServletRequest req) {
        log.error("unhandled_error userId={} reqId={} uri={}",
                req.getHeader("X-User-Id"), req.getHeader("Idempotency-Key"), req.getRequestURI(), e);
        return ErrorResponse.of("INTERNAL_ERROR", "일시적인 오류가 발생했습니다.", req.getRequestURI());
    }
}

