package com.gomdol.concert.concert.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


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

    @Test
    @DisplayName("일반 유저가 상태가 public이 아닌 아이디를 조회하면 ForbiddenException 예외를 던진다")
    public void 일반_유저가_상태가_PUBLIC이_아닌_아이디를_조회하면_ForbiddenException_예외를_던진다() throws Exception {
        // given

        // when

        // then
    }

    @Test
    @DisplayName("deleteAt이 not-null인 값을 조회하면 NotFoundException 예외를 던진다")
    public void deleteAt이_not_null인_값을_조회하면_NotFoundException_예외를_던진다() throws Exception {
        // given
        Long id = 1L;
        // when

        // then
    }
}
