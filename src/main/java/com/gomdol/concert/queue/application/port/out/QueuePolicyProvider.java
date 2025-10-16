package com.gomdol.concert.queue.application.port.out;

public interface QueuePolicyProvider {
    long waitingTtlSeconds(); // 대기 ttl
    long enteredTtlSeconds(); // 입장 ttl
    int capacity(); // 최대 입장 허용 수
//    int  admissionRatePerSec(Long targetId); // 승격용, 여기선 선언만
}
