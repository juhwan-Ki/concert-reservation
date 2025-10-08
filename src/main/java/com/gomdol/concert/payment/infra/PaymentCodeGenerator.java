package com.gomdol.concert.payment.infra;

import com.gomdol.concert.common.util.CodeGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentCodeGenerator implements CodeGenerator {
    private static final String PREFIX = "payment-";

    @Override
    public String newCodeGenerate() {
        return PREFIX + UUID.randomUUID();
    }
}
