# Apache Kafka 기초 개념 정리

## 목차
1. [이벤트 기반 아키텍처](#1-이벤트-기반-아키텍처)
1. [Kafka란 무엇인가?](#2-kafka란?)
3. [Kafka 장단점](#3-kafka-장단점)
4. [프로젝트 적용 사례](#4-프로젝트-적용-사례)

---

## 1. 이벤트 기반 아키텍처

### 1-1. 이벤트 기반 아키텍처(Event-Driven Architecture)란?
이벤트 기반 아키텍처는 어떤 일이 일어났다는 사실(이벤트)를 기반으로 시스템을 연결하는 방식이다 <br>
서비스들은 이벤트를 발행(publish)하고 다른 서비스들은 해당 이벤트를 구독(subscribe)하여 비동기로 이벤트를 처리한다

### 1-2. 구성 요소
1. Event Producer (발행자)
    - 이벤트를 만들어서 브로커(카프카 등)에 보냄
    - 예: 결제 서비스가 `PaymentCompleted` 이벤트 발행
2. Event Broker (이벤트 전달 시스템)
    - 이벤트를 저장/전달/버퍼링/재전송 같은 이벤트 처리를 수행
    - 대표: Apache Kafka, RabbitMQ 등
3. Event Consumer (구독자)
    - 특정 이벤트를 구독해서 자기 로직 수행
    - 예: 좌석 서비스가 `PaymentCompleted`를 받아 좌석 확정

### 1-3 동작 흐름 예시
ex) 결제 완료 → 포인트 차감 → 좌석 확정

1. 결제 서비스가 결제 처리 완료
2. `PaymentCompleted` 이벤트 발행
3. 포인트 서비스가 이벤트 구독 → 포인트 차감 처리
4. 좌석 확정 서비스가 이벤트 구독 → 좌석 확정 처리

여기서 중요한 점:
- 결제 서비스는 포인트/좌석 서비스가 존재하는지 몰라도 됨
- 구독자들은 서로 직접 호출하지 않고 이벤트만 보고 움직임

### 1-4. 이벤트 기반 아키텍처 사용 시 장점
#### 기존 동기 방식의 문제점

ex) 결제 → 포인트 차감 → 좌석 확정

```
[Client] → [Payment Service] → [Point Service] → [Reservation Service]
              |                     |                    |
              └───────────────── 동기 대기 ────────────────┘
```

**문제점:**
1. **강결합 (Tight Coupling)**
    - Payment가 Point, Reservation 모두에 의존
    - 하나의 서비스 변경 시 다른 서비스도 영향

2. **장애 전파**
    - Reservation Service 장애 시 전체 결제 실패
    - 부분 실패 처리 어려움

3. **낮은 확장성**
    - Point Service 부하 시 Payment도 느려짐
    - 개별 서비스 스케일링 어려움

4. **긴 응답 시간**
    - 모든 단계가 완료될 때까지 대기
    - 사용자 경험 저하

#### 이벤트 기반 방식의 장점

```
[Payment Service] → [Kafka Topic: commands]
                           ↓
                    [Point Service] → [Kafka Topic: events]
                           ↓
                    [Payment Service] → [Kafka Topic: commands]
                           ↓
                    [Reservation Service] → [Kafka Topic: events]
                           ↓
                    [Payment Service]
```

**장점:**
1. **느슨한 결합 (Loose Coupling)**
    - 각 서비스는 이벤트만 발행/구독
    - 서비스 간 직접 의존성 없음

2. **장애 격리**
    - 한 서비스 장애가 다른 서비스에 즉시 영향 없음
    - 보상 트랜잭션으로 롤백 가능

3. **독립적 확장**
    - 각 서비스를 개별적으로 스케일링
    - Consumer 추가로 처리량 증가

4. **빠른 응답**
    - 비동기 처리로 즉시 응답
    - 백그라운드에서 순차 처리

5. **감사 추적 (Audit Trail)**
    - 모든 이벤트가 Kafka에 기록
    - 시스템 상태 변화 추적 가능

## 2. Kafka란?

### 정의
Apache Kafka는 **분산 이벤트 스트리밍 플랫폼**으로 “이벤트를 안전하게 저장하고, 여러 소비자가 읽어가며 처리하게 하는” 시스템이다

### 전통적 메시지 큐와의 차이

| 구분 | 전통적 메시지 큐 (RabbitMQ, ActiveMQ) | Apache Kafka |
|------|----------------------------------|--------------|
| **데이터 저장** | 메모리 기반, 소비 후 삭제 | 디스크 기반, 영구 저장 |
| **처리량** | 초당 수천~수만 건 | 초당 수백만 건 |
| **재처리** | 어려움 (메시지 소비 시 삭제) | 쉬움 (Offset 조정) |
| **순서 보장** | 큐 단위 | Partition 단위 |
| **아키텍처** | Broker 중심 | Log 중심 |
| **사용 사례** | 작업 큐, RPC 패턴 | 이벤트 스트리밍, 로그 수집 |

### 핵심 컨셉
```
Kafka = 분산 커밋 로그 (Distributed Commit Log) + Pub/Sub 메시징
```

- **이벤트 로그**: 발생한 모든 이벤트를 시간 순서대로 기록
- **영속성**: 설정한 기간(retention period)만큼 디스크에 저장
- **확장성**: Partition 단위로 수평 확장 가능

---

## 2. 핵심 개념

### 2.1 기본 구성 요소

#### Topic
- 이벤트를 분류하는 **카테고리** 또는 **피드 이름**
- 예시: `payment-events`, `reservation-events`, `user-activity-logs`
- 여러 개의 Partition으로 구성

```
Topic: payment-events
├── Partition 0: [Event1, Event2, Event3, ...]
├── Partition 1: [Event4, Event5, Event6, ...]
└── Partition 2: [Event7, Event8, Event9, ...]
```

#### Partition
- Topic의 **물리적 분할 단위**
- 각 Partition은 **순서가 보장되는 불변 이벤트 시퀀스**
- Partition 수만큼 병렬 처리 가능

**중요한 특징:**
- 동일 Key를 가진 메시지는 같은 Partition으로 전송 (순서 보장)
- Partition은 증가만 가능, 감소 불가능
- Replication Factor만큼 복제되어 고가용성 확보

**Kafka에서 메시지 순서 보장과 처리량을 동시에 확보하기 위해 어떻게 설계해야나?**
- 순서 보장의 기준이 되는 값을 메시지 Key로 설정하고, Topic의 Partition의 수를 늘려 병렬 처리를 극대화
- Key를 가진 메시지는 같은 Partition으로 전송된다는 점을 이용
- 여기서 주의할 점은 순서 보장을 위해서 같은 Key로 이벤트를 발행하게 되면 하나의 Partition이 많은 처리를 하게됨
- 이러한 상황은 전역적인 순서를 포기하는 대신 논리적인 분할 순서로 변경하거나 kafka 앞단에 순서를 정하는 로직을 처리하는 등의 방법을 사용하여 순서를 유지
  - 전역적 순서 : 모든 요청을 1~10000 순서로 정확히 처리 
  - 논리적 순선 : 쿠폰 재고를 여러 그룹으로 나누고 각 그룹 안에서는 선착순 보장

#### Producer
- Kafka Topic에 **이벤트를 발행**하는 애플리케이션
- 어떤 Partition으로 보낼지 결정 (Key 기반 또는 Round-robin)

#### Consumer
- Topic에서 **이벤트를 구독**하는 애플리케이션
- Offset을 통해 어디까지 읽었는지 추적

#### Consumer Group
- 동일한 Topic을 **분산 처리**하는 Consumer 집합
- 각 Partition은 Consumer Group 내 하나의 Consumer에만 할당
- Consumer 수 = Partition 수일 때 최적 (1:1 매핑)

```
Topic: orders (Partition 3개)
Consumer Group: order-processor
├── Consumer 1 → Partition 0
├── Consumer 2 → Partition 1
└── Consumer 3 → Partition 2
```

#### Broker
- Kafka 서버 노드
- 여러 Broker로 클러스터 구성
- Leader/Follower 구조로 Partition 복제

#### Offset
- Partition 내 각 메시지의 **고유 위치 값**
- Consumer가 어디까지 읽었는지 추적하는 포인터
- 0부터 시작하는 증가하는 정수

```
Partition 0:
Offset: 0    1    2    3    4    5
Data:  [E1] [E2] [E3] [E4] [E5] [E6]
               ↑
          Consumer의 현재 위치
```

### 2.2 Kafka의 핵심 특징

#### 1️⃣ 순서 보장 (Ordering)
- **Partition 단위**로만 순서 보장
- 동일 Key를 가진 메시지는 같은 Partition으로 전송되어 순서 유지
- Topic 전체 순서는 보장 안 됨 (Partition이 여러 개인 경우)

**예시:**
```java
// 같은 userId는 항상 같은 Partition으로
producer.send(new ProducerRecord<>("user-events", userId, event));
```

#### 2️⃣ 영속성 (Durability)
- 모든 메시지는 **디스크에 저장**
- Retention 정책에 따라 일정 기간 보관 (예: 7일, 30일)
- 장애 발생 시에도 데이터 유실 없음

#### 3️⃣ 확장성 (Scalability)
- Partition 추가로 수평 확장
- Consumer Group으로 처리 능력 증가
- 클러스터에 Broker 추가로 용량 확장

#### 4️⃣ 내구성 (Fault Tolerance)
- Replication Factor로 데이터 복제 (예: 3개 복제본)
- Broker 장애 시 자동 Failover
- Leader 선출로 서비스 중단 최소화

---

---

## 3. Kafka 장단점

### ✅ 장점

#### 1. 확장성 (Scalability)
```
처리량 증가 방법:
1. Partition 수 증가 (병렬 처리)
2. Consumer 수 증가 (Consumer Group)
3. Broker 수 증가 (클러스터 확장)
```

#### 2. 내구성 & 신뢰성 (Durability & Reliability)
```
Replication Factor = 3
[Broker 1 (Leader)] → [Broker 2 (Follower)] → [Broker 3 (Follower)]

Broker 1 장애 시:
→ Broker 2가 자동으로 Leader 승격
→ 데이터 유실 없음
```

#### 3. 재처리 가능 (Replayability)
```java
// 특정 시점부터 재소비
consumer.seek(partition, offset);

// 특정 시간대부터 재소비
consumer.seekToBeginning(partition);
```

**활용 사례:**
- 버그 수정 후 과거 데이터 재처리
- 새로운 분석 로직 적용
- 장애 복구 후 누락 데이터 처리

#### 4. 느슨한 결합 (Loose Coupling)
```
Producer와 Consumer가 서로를 모름:
- Producer는 Topic에만 발행
- Consumer는 Topic에서만 구독
- 새로운 Consumer 추가 시 기존 시스템 무영향
```

#### 5. 대용량 처리 (High Throughput)
- 초당 수백만 건 메시지 처리
- 배치 처리 및 압축으로 네트워크 효율성
- Zero-copy 기술로 CPU 오버헤드 최소화

### ❌ 단점 & 고려사항

#### 1. 운영 복잡도 (Operational Complexity)
**필요한 관리:**
- Zookeeper 클러스터 (또는 KRaft 모드)
- Broker 클러스터
- 모니터링 시스템 (Prometheus, Grafana)
- 디스크 용량 관리

**학습 곡선:**
- Partition, Offset, Replication 개념 이해
- Consumer Group Rebalancing 동작 원리
- 성능 튜닝 파라미터 (batch size, linger.ms, compression 등)

#### 2. 즉시성 부족 (Eventual Consistency)
```
동기 방식:
Client → Service → 즉시 결과 반환 ✅

비동기 방식:
Client → Service → 202 Accepted
                 ↓ (Kafka)
              나중에 완료 ⏰
```

**해결 방안:**
- 폴링 (Polling): 주기적으로 상태 확인
- 웹소켓/SSE: 실시간 알림
- Outbox Pattern: 트랜잭션 보장

#### 3. 메시지 순서 보장 제한
```
Topic 전체 순서: ❌ (Partition이 여러 개)
Partition 내 순서: ✅

해결책: Key 기반 Partitioning
producer.send(new ProducerRecord<>("topic", userId, event));
→ 같은 userId는 항상 같은 Partition
```

#### 4. 중복 처리 가능성
**At-least-once 전달:**
```
Producer → Kafka (성공) → ACK 손실 → Producer 재전송
→ 중복 메시지 발생 가능
```

**해결 방안:**
```java
// 1. Idempotent Producer 활성화
props.put("enable.idempotence", true);

// 2. Consumer에서 멱등성 보장
@Transactional
public void handleEvent(PaymentEvent event) {
    if (processedEventRepository.existsByRequestId(event.getRequestId())) {
        return; // 이미 처리된 이벤트 무시
    }
    // 비즈니스 로직 처리
    processedEventRepository.save(new ProcessedEvent(event.getRequestId()));
}
```

### 언제 Kafka를 사용할까?

#### ✅ Kafka가 적합한 경우
- 초당 수천 건 이상의 메시지 처리
- 이벤트 히스토리가 중요한 경우 (재처리, 감사)
- 여러 Consumer가 동일 데이터를 소비
- 실시간 스트림 처리 (집계, 변환, 분석)
- 마이크로서비스 간 비동기 통신

#### ❌ Kafka가 과한 경우
- 초당 수십 건 미만의 저처리량
- 실시간 응답이 필수적인 경우 (동기 API 사용)
- 단순한 작업 큐 (RabbitMQ가 더 간단)
- 운영 리소스 부족

---

## 4. 프로젝트 적용 사례

### 4.1 Saga Pattern 구현

**비즈니스 플로우: 콘서트 티켓 결제**

#### 정상 흐름

```
1. [Payment Service]
   └─ Payment 생성 (PENDING)
   └─ UsePointCommand → Outbox 저장

2. [Outbox Scheduler]
   └─ Outbox → Kafka Topic: point-commands

3. [Point Service]
   └─ UsePointCommand 소비
   └─ 포인트 차감
   └─ PointUsedEvent(success) → Outbox 저장

4. [Outbox Scheduler]
   └─ Outbox → Kafka Topic: point-events

5. [Payment Service]
   └─ PointUsedEvent 소비
   └─ Payment 상태 업데이트 (PROCESSING)
   └─ ConfirmSeatsCommand → Outbox 저장

6. [Outbox Scheduler]
   └─ Outbox → Kafka Topic: reservation-commands

7. [Reservation Service]
   └─ ConfirmSeatsCommand 소비
   └─ 좌석 확정
   └─ SeatsConfirmedEvent → Outbox 저장

8. [Outbox Scheduler]
   └─ Outbox → Kafka Topic: reservation-events

9. [Payment Service]
   └─ SeatsConfirmedEvent 소비
   └─ Payment 상태 업데이트 (COMPLETED) ✅
```

#### 실패 시 보상 트랜잭션 (Compensating Transaction)

```
시나리오: 좌석 확정 실패

1-5. (정상 흐름과 동일)

6. [Reservation Service]
   └─ ConfirmSeatsCommand 소비
   └─ 좌석 확정 실패 (이미 매진)
   └─ SeatsConfirmedEvent(failed) → Outbox 저장

7. [Payment Service]
   └─ SeatsConfirmedEvent(failed) 소비
   └─ Payment 상태 업데이트 (FAILED)
   └─ RefundPointCommand → Outbox 저장 (보상)

8. [Point Service]
   └─ RefundPointCommand 소비
   └─ 포인트 환불 ✅
```

### 4.2 Outbox Pattern

**문제: 데이터베이스 트랜잭션과 Kafka 발행의 원자성 보장**

```
잘못된 방식:
@Transactional
public void createPayment() {
    paymentRepository.save(payment);  // DB 저장 성공
    kafkaProducer.send(event);         // Kafka 발행 실패
    // → 데이터 불일치 발생!
}
```

**Outbox Pattern 해결책:**

```java
@Transactional
public void createPayment() {
    // 1. 동일 트랜잭션 내에서 Outbox에 저장
    paymentRepository.save(payment);
    outboxRepository.save(new Outbox(
        topic = "point-commands",
        payload = new UsePointCommand(...)
    ));
    // → DB 커밋으로 원자성 보장 ✅
}

// 2. 별도 스케줄러가 Outbox → Kafka 발행
@Scheduled(fixedDelay = 1000)
public void publishOutbox() {
    List<Outbox> pending = outboxRepository.findByStatus(PENDING);
    for (Outbox outbox : pending) {
        kafkaProducer.send(outbox.getTopic(), outbox.getPayload());
        outboxRepository.updateStatus(outbox.getId(), PUBLISHED);
    }
}
```

**장점:**
- DB 트랜잭션과 메시지 발행의 원자성 보장
- Kafka 장애 시에도 데이터 유실 없음
- 재시도 로직 구현 용이

### 4.3 현재 코드 구조

```
src/main/java/com/gomdol/concert/
├── payment/
│   ├── application/
│   │   └── event/
│   │       ├── PointEventConsumer.java          # PointUsedEvent 구독
│   │       └── ReservationEventConsumer.java    # SeatsConfirmedEvent 구독
│   └── domain/
│       └── event/
│           └── UsePointCommand.java             # 포인트 차감 명령
│
├── point/
│   ├── application/
│   │   └── event/
│   │       └── PointCommandConsumer.java        # UsePointCommand 구독
│   └── domain/
│       └── event/
│           └── PointUsedEvent.java              # 포인트 차감 완료 이벤트
│
├── reservation/
│   ├── application/
│   │   └── event/
│   │       └── ReservationCommandConsumer.java  # ConfirmSeatsCommand 구독
│   └── domain/
│       └── event/
│           └── SeatsConfirmedEvent.java         # 좌석 확정 완료 이벤트
│
└── common/
    ├── outbox/
    │   ├── domain/
    │   │   └── Outbox.java                      # Outbox 엔티티
    │   └── infra/
    │       └── scheduler/
    │           └── OutboxScheduler.java         # Outbox → Kafka 발행
    └── kafka/
        ├── KafkaProducerConfig.java
        └── KafkaConsumerConfig.java
```
