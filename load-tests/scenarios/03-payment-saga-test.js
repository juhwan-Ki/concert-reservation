/**
 * 시나리오 3: 결제 Saga Pattern 부하 테스트
 *
 * 목적: Kafka 기반 분산 트랜잭션 처리량 및 완료율 검증
 *
 * 시나리오:
 * - 100명이 동시에 결제 요청
 * - Saga 정상 완료율 측정
 * - Payment 상태가 COMPLETED로 변경되는 시간 측정
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { uuid } from '../utils/helpers.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom Metrics
const paymentCreated = new Counter('payment_created');
const paymentCompleted = new Counter('payment_completed');
const paymentFailed = new Counter('payment_failed');
const sagaDuration = new Trend('saga_completion_duration');

export const options = {
  scenarios: {
    payment_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50 },  // 50명까지 증가
        { duration: '30s', target: 50 },  // 50명 유지
        { duration: '10s', target: 0 },   // 감소
      ],
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<2000'], // 결제는 Saga이므로 2초 허용
    'http_req_failed': ['rate<0.05'],
    'saga_completion_duration': ['p(95)<15000'], // Saga 완료 15초 이내
  },
};

export default function () {
  const userId = `user-${__VU}`;
  const reservationId = Math.floor(Math.random() * 1000) + 1; // 랜덤 예약 ID
  const idempotencyKey = uuid();

  const payload = JSON.stringify({
    reservationId: reservationId,
    amount: 150000,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer test-${userId}`,
      'Idempotency-Key': idempotencyKey,
    },
  };

  // 1. 결제 요청
  const startTime = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/payments/`,
    payload,
    params
  );

  const created = check(res, {
    'payment created': (r) => r.status === 200,
    'has paymentId': (r) => r.json() && r.json().paymentId,
  });

  if (!created) {
    paymentFailed.add(1);
    console.error(`VU ${__VU}: Payment creation failed - ${res.status}`);
    return;
  }

  paymentCreated.add(1);
  const paymentId = res.json().paymentId;

  // 2. Saga 완료 폴링 (최대 20초 대기)
  let completed = false;
  let attempts = 0;
  const maxAttempts = 20; // 20초

  while (!completed && attempts < maxAttempts) {
    sleep(1);
    attempts++;

    // Payment 상태 조회 (실제로는 GET /api/v1/payments/{paymentId} 필요)
    // 현재는 간소화하여 생략
    // TODO: Payment 상태 조회 API 구현 시 추가
  }

  const duration = Date.now() - startTime;

  if (completed) {
    paymentCompleted.add(1);
    sagaDuration.add(duration);
    console.log(`VU ${__VU}: Saga completed in ${duration}ms`);
  } else {
    console.warn(`VU ${__VU}: Saga not completed within ${maxAttempts}s`);
  }

  sleep(1);
}

export function handleSummary(data) {
  const created = data.metrics.payment_created?.values.count || 0;
  const completed = data.metrics.payment_completed?.values.count || 0;
  const failed = data.metrics.payment_failed?.values.count || 0;

  const completionRate = created > 0 ? (completed / created * 100).toFixed(2) : 0;

  let summary = '\n' + '='.repeat(60) + '\n';
  summary += 'Payment Saga Test Summary\n';
  summary += '='.repeat(60) + '\n\n';
  summary += `Total Payments Created: ${created}\n`;
  summary += `Completed: ${completed} (${completionRate}%)\n`;
  summary += `Failed: ${failed}\n\n`;

  if (data.metrics.saga_completion_duration) {
    summary += `Saga Duration:\n`;
    summary += `  avg: ${data.metrics.saga_completion_duration.values.avg.toFixed(2)}ms\n`;
    summary += `  p(95): ${data.metrics.saga_completion_duration.values['p(95)'].toFixed(2)}ms\n\n`;
  }

  if (parseFloat(completionRate) >= 95) {
    summary += 'TEST PASSED: Saga completion rate >= 95%\n';
  } else {
    summary += 'TEST WARNING: Saga completion rate < 95%\n';
  }

  summary += '='.repeat(60) + '\n';

  return {
    '../results/saga-test-summary.json': JSON.stringify(data, null, 2),
    'stdout': summary,
  };
}
