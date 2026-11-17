package com.justtarot.tarot_reading.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 무료 이용권 1개 차감. 한도검사+증가를 한 문장으로 처리해 동시 요청이 한도를 넘지 못하게 한다.
     * 저장된 주기가 현재 주기(cycleStart)가 아니면(첫 사용 포함) 1부터 다시 센다 — freeReadingUsed 를
     * 먼저 대입해야 이전 주기 값으로 CASE 가 평가된다.
     * @return 1 이면 차감 성공, 0 이면 한도 초과
     */
    @Modifying
    @Query("""
            update User u
               set u.freeReadingUsed = case when u.freeReadingCycleStart = :cycleStart then u.freeReadingUsed + 1 else 1 end,
                   u.freeReadingCycleStart = :cycleStart
             where u.id = :id
               and (u.freeReadingCycleStart is null or u.freeReadingCycleStart <> :cycleStart or u.freeReadingUsed < :limit)
            """)
    int tryConsumeFree(@Param("id") Long id, @Param("cycleStart") LocalDate cycleStart, @Param("limit") int limit);

    /**
     * 차감 이후 해석 실패 시 되돌린다. 그 사이 주기가 바뀌었으면 되돌릴 대상이 없어 no-op.
     */
    @Modifying
    @Query("""
            update User u
               set u.freeReadingUsed = u.freeReadingUsed - 1
             where u.id = :id and u.freeReadingCycleStart = :cycleStart and u.freeReadingUsed > 0
            """)
    int refundFree(@Param("id") Long id, @Param("cycleStart") LocalDate cycleStart);
}
