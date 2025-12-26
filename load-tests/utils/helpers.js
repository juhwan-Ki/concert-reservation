/**
 * 테스트 헬퍼 함수
 */

import { sleep } from 'k6';

/**
 * UUID v4 생성 (간소화 버전)
 */
export function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

/**
 * 랜덤 지연
 */
export function randomSleep(min, max) {
  const delay = Math.random() * (max - min) + min;
  sleep(delay);
}

/**
 * 배열에서 랜덤 선택
 */
export function randomChoice(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/**
 * 결과 메트릭 출력
 */
export function printMetrics(data) {
  console.log('='.repeat(50));
  console.log('Test Metrics:');
  console.log(JSON.stringify(data, null, 2));
  console.log('='.repeat(50));
}
