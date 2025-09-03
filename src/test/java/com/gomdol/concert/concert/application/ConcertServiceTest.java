package com.gomdol.concert.concert.application;

import com.gomdol.concert.concert.infra.repository.ConcertRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {

    @Mock
    private ConcertRepository concertRepository;

    @InjectMocks
    private ConcertService concertService;

    /*
    * 콘서트 서비스 테스트
    * - 단건 조회
    * 1. 아이디에 해당하는 값이 존재하면 DTO로 매핑해 반환한다
    * 2. 아이디에 해당하는 값이 없으면 NotFoundException을 발생한다
    * 3. 일반 유저가 PUBLIC이 아닌 콘서트를 조회하면 ForbiddenException을 발생시킨다
    * 4. deleteYn 값이 Y인 값이 조회되면 NotFoundException을 발생한다
    * 5. 현재 날짜가 시작일 이전이면 NotFoundException을 발생한다
    * 6. 현재 날짜가 종료일 이후면 NotFoundException을 발생한다
    * - 목록 조회
    * 1. 페이징 기본값을 적용해서 페이징이된 리스트를 반환한다
    * 2. 리스트 사이즈가 20을 넘는 값이 들어오면 20으로 고정된다
    * 3. 리스트 사이즈가 0보다 작으면 0으로 고정된다
    * 4. 결과가 비어 있으면 빈 리스트를 반환한다
    * TODO : 검색 조건은 따로 구현하지 않았는데 추후 구현 필요
    * */
}
