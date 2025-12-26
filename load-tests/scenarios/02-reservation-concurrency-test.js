/**
 * 시나리오 2: 동시 예약 경쟁 테스트
 *
 * 목적: 동일 좌석에 대한 동시 예약 요청 시 분산락 정확성 검증
 *
 * 시나리오:
 * - 100명이 동일 좌석을 동시에 예약 시도
 * - 정확히 1명만 성공, 99명은 409 Conflict 응답
 */

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { uuid } from '../utils/helpers.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SHOW_ID = __ENV.SHOW_ID || '101';
const SEAT_ID = __ENV.SEAT_ID || '1'; // 모두 동일 좌석 경쟁

// Custom Metrics
const reservationSuccess = new Counter('reservation_success');
const reservationConflict = new Counter('reservation_conflict');
const reservationError = new Counter('reservation_error');
const accuracyRate = new Rate('accuracy_rate'); // 정확히 1명만 성공하는지

export const options = {
  scenarios: {
    concurrent_reservation: {
      executor: 'shared-iterations',
      vus: 100,           // 동시 100명
      iterations: 100,    // 총 100번 시도
      maxDuration: '30s',
    },
  },
  thresholds: {
    'reservation_success': ['count==1'],      // 정확히 1명만 성공
    'reservation_conflict': ['count==99'],    // 99명은 충돌
    'http_req_failed': ['rate<0.05'],         // 시스템 에러 < 5%
  },
};

export default function () {
  const userId = `user-${__VU}`;
  const queueToken = 'test-entered-token'; // 사전에 ENTERED 상태 토큰 발급 필요
  const idempotencyKey = uuid();

  const payload = JSON.stringify({
    seatIds: [parseInt(SEAT_ID)], // 모두 동일 좌석
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer test-${userId}`,
      'Idempotency-Key': idempotencyKey,
      'Queue-Token': queueToken,
    },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/reservations/${SHOW_ID}`,
    payload,
    params
  );

  const result = check(res, {
    'status is 200 or 201 (success)': (r) => r.status === 200 || r.status === 201,
    'status is 409 (conflict)': (r) => r.status === 409,
    'status is 500 (error)': (r) => r.status >= 500,
  });

  if (res.status === 200 || res.status === 201) {
    reservationSuccess.add(1);
    console.log(`VU ${__VU}: Reservation SUCCESS - ${res.body}`);
  } else if (res.status === 409) {
    reservationConflict.add(1);
  } else {
    reservationError.add(1);
    console.error(`VU ${__VU}: Unexpected status ${res.status} - ${res.body}`);
  }
}

export function handleSummary(data) {
  const successCount = data.metrics.reservation_success?.values.count || 0;
  const conflictCount = data.metrics.reservation_conflict?.values.count || 0;
  const errorCount = data.metrics.reservation_error?.values.count || 0;

  const isAccurate = successCount === 1 && conflictCount === 99;

  let summary = '\n' + '='.repeat(60) + '\n';
  summary += 'Reservation Concurrency Test Summary\n';
  summary += '='.repeat(60) + '\n\n';
  summary += `Total Attempts: ${successCount + conflictCount + errorCount}\n`;
  summary += `  Success (200/201): ${successCount}\n`;
  summary += `  Conflict (409): ${conflictCount}\n`;
  summary += `  Error (5xx): ${errorCount}\n\n`;

  if (isAccurate) {
    summary += 'TEST PASSED: Exactly 1 success, 99 conflicts!\n';
    summary += 'Distributed lock is working correctly.\n';
  } else {
    summary += 'TEST FAILED: Expected 1 success and 99 conflicts.\n';
    summary += 'Distributed lock may have issues.\n';
  }

  summary += '='.repeat(60) + '\n';

  return {
    '../results/concurrency-test-summary.json': JSON.stringify(data, null, 2),
    'stdout': summary,
  };
}
