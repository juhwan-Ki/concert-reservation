/**
 * 시나리오 4: 장시간 부하 내구성 테스트 (Soak Test)
 *
 * 목적: 일정 부하를 장시간 유지하여 메모리 누수, 커넥션 풀 관리 검증
 *
 * 패턴:
 * - 5분간 100명 유지 (실제로는 30분 권장하지만 테스트용으로 5분)
 * - 대기열 상태 확인 70%, 콘서트 조회 20%, 예약 시도 10%
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { randomChoice, uuid } from '../utils/helpers.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DURATION = __ENV.SOAK_DURATION || '5m'; // 환경변수로 조정 가능

// Custom Metrics
const queueChecks = new Counter('queue_checks');
const concertViews = new Counter('concert_views');
const reservationAttempts = new Counter('reservation_attempts');
const responseTimes = new Trend('custom_response_time');

export const options = {
  scenarios: {
    soak: {
      executor: 'constant-vus',
      vus: 100,
      duration: DURATION,
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<1000', 'p(99)<2000'],
    'http_req_failed': ['rate<0.01'],
    'custom_response_time': ['p(95)<1000'],
  },
};

export default function () {
  const userId = `user-${__VU}`;
  const token = `test-${userId}`;

  // 70%: 대기열 상태 확인
  // 20%: 콘서트 목록 조회
  // 10%: 예약 시도
  const action = weightedRandom([
    { weight: 70, value: 'queue' },
    { weight: 20, value: 'concert' },
    { weight: 10, value: 'reservation' },
  ]);

  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  };

  let res;
  const startTime = Date.now();

  switch (action) {
    case 'queue':
      // 대기열 상태 확인 폴링
      const queuePayload = JSON.stringify({
        targetId: 1,
        token: `queue-token-${userId}`,
      });

      res = http.post(
        `${BASE_URL}/api/v1/queues/enter`,
        queuePayload,
        {
          headers: {
            ...params.headers,
            'Content-Type': 'application/json',
          },
        }
      );

      queueChecks.add(1);
      break;

    case 'concert':
      // 콘서트 목록 조회
      res = http.get(
        `${BASE_URL}/api/v1/concerts/?page=0&size=20`,
        params
      );

      concertViews.add(1);
      break;

    case 'reservation':
      // 예약 시도
      const reservationPayload = JSON.stringify({
        seatIds: [Math.floor(Math.random() * 100) + 1],
      });

      res = http.post(
        `${BASE_URL}/api/v1/reservations/101`,
        reservationPayload,
        {
          headers: {
            ...params.headers,
            'Content-Type': 'application/json',
            'Idempotency-Key': uuid(),
            'Queue-Token': `queue-token-${userId}`,
          },
        }
      );

      reservationAttempts.add(1);
      break;
  }

  const duration = Date.now() - startTime;
  responseTimes.add(duration);

  check(res, {
    'status is 2xx or 4xx': (r) => r.status >= 200 && r.status < 500,
  });

  sleep(3); // 3초마다 요청
}

function weightedRandom(items) {
  const totalWeight = items.reduce((sum, item) => sum + item.weight, 0);
  let random = Math.random() * totalWeight;

  for (const item of items) {
    if (random < item.weight) {
      return item.value;
    }
    random -= item.weight;
  }

  return items[items.length - 1].value;
}

export function handleSummary(data) {
  const queueCount = data.metrics.queue_checks?.values.count || 0;
  const concertCount = data.metrics.concert_views?.values.count || 0;
  const reservationCount = data.metrics.reservation_attempts?.values.count || 0;
  const total = queueCount + concertCount + reservationCount;

  let summary = '\n' + '='.repeat(60) + '\n';
  summary += 'Soak Test Summary\n';
  summary += '='.repeat(60) + '\n\n';
  summary += `Duration: ${DURATION}\n`;
  summary += `Virtual Users: 100\n\n`;
  summary += `Total Requests: ${total}\n`;
  summary += `  Queue Checks: ${queueCount} (${(queueCount/total*100).toFixed(1)}%)\n`;
  summary += `  Concert Views: ${concertCount} (${(concertCount/total*100).toFixed(1)}%)\n`;
  summary += `  Reservations: ${reservationCount} (${(reservationCount/total*100).toFixed(1)}%)\n\n`;

  if (data.metrics.http_req_duration) {
    summary += `Response Time:\n`;
    summary += `  avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += `  p(95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += `  p(99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n\n`;
  }

  if (data.metrics.http_req_failed) {
    const errorRate = (data.metrics.http_req_failed.values.rate * 100).toFixed(2);
    summary += `Error Rate: ${errorRate}%\n\n`;
  }

  summary += '='.repeat(60) + '\n';

  return {
    '../results/soak-test-summary.json': JSON.stringify(data, null, 2),
    'stdout': summary,
  };
}
