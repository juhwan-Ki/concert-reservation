package com.gomdol.concert.user.domain.policy;

public class UserPolicy {

    public static void validateUser(String userId) {
        if(userId == null || userId.isBlank() || userId.length() != 36)
            throw new IllegalArgumentException("요청한 사용자 ID가 올바른 형식이 아닙니다.");
    }
}
