package com.gomdol.concert.concert.domain;

public class ConcertTest {

    /*
    * 콘서트 도메인 테스트
    * - 생성
    * 1. 데이터가 정상적으로 입력되면 PRIVATE 상태로 생성된다
    * 2. 데이터가 정상적으로 입력되지 않으면(null or "") 이면 예외가 발생한다
    * 3. 입력값은 양끝 공백 제거하여 처리한다
    * - 수정
    * 1. 상태 변경이 가능하다
    * 2. 이미 PUBLIC 상태를 PUBLIC 으로 공개하면 예외가 발생한다
    * 3. CANCELED 에서 상태 변경/및 수정하면 예외가 발생한다(취소가 되었기 때문에 변경 불가)
    * 4. 값 변경은 HIDDEN, PRIVATE 에서만 가능하다
    * */
}
