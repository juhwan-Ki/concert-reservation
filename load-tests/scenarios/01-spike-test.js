/**
 * 시나리오 1: 티켓 오픈 스파이크 테스트
 *
 * 목적: 티켓 오픈 순간의 급격한 트래픽 스파이크 대응 능력 검증
 *
 * 패턴:
 * - 0~10s: 0 → 1000명으로 급증
 * - 10~30s: 1000명 유지
 * - 30~40s: 1000 → 0으로 감소
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { uuid } from '../utils/helpers.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TARGET_ID = __ENV.TARGET_ID || '1'; // 콘서트 ID

// Custom Metrics
const queueTokenIssued = new Counter('queue_token_issued');
const queueTokenFailed = new Counter('queue_token_failed');
const queueTokenDuration = new Trend('queue_token_duration');
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 1000 }, // 스파이크: 10초 내 1000명
        { duration: '20s', target: 1000 }, // 유지: 1000명
        { duration: '10s', target: 0 },    // 감소
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<500', 'p(99)<1000'], // 95%ile < 500ms
    'http_req_failed': ['rate<0.01'],                 // 실패율 < 1%
    'error_rate': ['rate<0.01'],
  },
};

export default function () {
  const userId = `user-${__VU}`;
  const idempotencyKey = uuid();

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer test-${userId}`,
      'Idempotency-Key': idempotencyKey,
    },
  };

  // 대기열 토큰 발급
  const startTime = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/queues/tokens/${TARGET_ID}`,
    null,
    params
  );
  const duration = Date.now() - startTime;

  const success = check(res, {
    'status is 201': (r) => r.status === 201,
    'has token': (r) => r.json() && r.json().token,
    'response time < 500ms': () => duration < 500,
  });

  if (success) {
    queueTokenIssued.add(1);
    queueTokenDuration.add(duration);
  } else {
    queueTokenFailed.add(1);
    errorRate.add(1);
    console.error(`VU ${__VU}: Failed - Status ${res.status}, Duration ${duration}ms`);
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    '../results/spike-test-summary.json': JSON.stringify(data, null, 2),
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  let summary = '\n' + '='.repeat(60) + '\n';
  summary += indent + 'Spike Test Summary\n';
  summary += '='.repeat(60) + '\n\n';

  // HTTP Metrics
  if (data.metrics.http_req_duration) {
    summary += indent + 'HTTP Request Duration:\n';
    summary += indent + `  avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += indent + `  p(95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += indent + `  p(99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n\n`;
  }

  // Custom Metrics
  if (data.metrics.queue_token_issued) {
    summary += indent + 'Queue Tokens:\n';
    summary += indent + `  Issued: ${data.metrics.queue_token_issued.values.count}\n`;
    summary += indent + `  Failed: ${data.metrics.queue_token_failed?.values.count || 0}\n\n`;
  }

  // Error Rate
  if (data.metrics.error_rate) {
    const rate = (data.metrics.error_rate.values.rate * 100).toFixed(2);
    summary += indent + `Error Rate: ${rate}%\n\n`;
  }

  summary += '='.repeat(60) + '\n';
  return summary;
}
