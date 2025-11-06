package com.gomdol.concert.concert.infra.persistence.command;

import com.gomdol.concert.concert.application.port.out.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConcertRepositoryImpl implements ConcertRepository {

    private final ConcertJpaRepository jpaRepository;
}
