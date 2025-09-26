package com.gomdol.concert.payment.domain;

import com.gomdol.concert.payments.domain.PaymentStatus;
import com.gomdol.concert.payments.domain.model.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;


public class PaymentTest {

    @Test
    public void 결제_상태가_PENDING인_결제를_생성한다() throws Exception {
        // given
        Payment payment = mockPayment();
        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -90999})
    public void 금액이_0이하면_결제요청_에러를_발생시킨다(long amount) throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> Payment.create(
                        1L,
                        FIXED_UUID,
                        "paymentCode",
                        "test-asdasd124-asd125124312412",
                        amount
                ));
    }

    @Test
    public void 결제_상태가_PENDING인_결제를_성공처리한다() {
        // given
        Payment payment = mockPayment();
        // when && then
        payment.succeed();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    public void 결제_상태가_PENDING인_결제를_실패처리한다() {
        // given
        Payment payment = mockPayment();
        // when && then
        payment.failed();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    public void 결제_상태가_PENDING가_아니면_성공처리시_에러를_발생시킨다() {
        // given
        Payment payment = mockPayment(PaymentStatus.CANCELLED);
        // when && then
       assertThrows(IllegalStateException.class, payment::succeed);
    }

    @Test
    public void 결제_상태가_PENDING가_아니면_실패처리시_에러를_발생시킨다() {
        // given
        Payment payment = mockPayment(PaymentStatus.SUCCEEDED);
        // when && then
        assertThrows(IllegalStateException.class, payment::failed);
    }

    private Payment mockPayment() {
        return Payment.create(
                1L,
                FIXED_UUID,
                "paymentCode",
                "test-asdasd124-asd125124312412",
                50000L
        );
    }

    private Payment mockPayment(PaymentStatus status) {
        return Payment.of(
                null,
                1L,
                FIXED_UUID,
                "paymentCode",
                "test-asdasd124-asd125124312412",
                50000L,
                status,
                null
        );
    }
}
