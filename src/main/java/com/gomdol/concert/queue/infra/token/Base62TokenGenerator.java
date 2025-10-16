package com.gomdol.concert.queue.infra.token;

import com.gomdol.concert.queue.application.port.out.TokenGenerator;

import java.security.SecureRandom;

public class Base62TokenGenerator implements TokenGenerator {
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final int length;

    public Base62TokenGenerator(int length) {
        if (length < 8)
            throw new IllegalArgumentException("length too short");
        this.length = length;
    }

    @Override
    public String newToken() {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++)
            buf[i] = BASE62[random.nextInt(BASE62.length)];
        return new String(buf);
    }
}
