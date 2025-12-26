/**
 * 인증 관련 유틸리티
 */

import { check } from 'k6';
import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * 테스트용 사용자 토큰 생성
 * 실제로는 인증 API를 호출하지만, 현재는 테스트용 토큰 반환
 */
export function getAuthToken(userId) {
  // TODO: 실제 인증 구현 시 수정
  // POST /api/v1/auth/login
  return `test-user-${userId}`;
}

/**
 * 대기열 토큰 발급
 */
export function issueQueueToken(userId, targetId) {
  const idempotencyKey = `queue-${userId}-${targetId}-${Date.now()}`;

  const payload = JSON.stringify({
    userId: userId,
    targetId: targetId
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getAuthToken(userId)}`,
      'Idempotency-Key': idempotencyKey,
    },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/queues/tokens/${targetId}`,
    payload,
    params
  );

  const success = check(res, {
    'queue token issued': (r) => r.status === 201,
  });

  if (success && res.json()) {
    return res.json().token;
  }

  console.error(`Failed to issue queue token: ${res.status} - ${res.body}`);
  return null;
}

/**
 * 대기열 상태 확인 및 입장
 */
export function checkQueueStatus(userId, targetId, queueToken) {
  const payload = JSON.stringify({
    targetId: targetId,
    token: queueToken
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getAuthToken(userId)}`,
    },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/queues/enter`,
    payload,
    params
  );

  if (res.status === 200 && res.json()) {
    return res.json();
  }

  return null;
}

export { BASE_URL };
