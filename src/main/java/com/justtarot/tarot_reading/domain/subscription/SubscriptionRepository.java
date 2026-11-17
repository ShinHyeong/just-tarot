package com.justtarot.tarot_reading.domain.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * 지금 유효한 결제 주기. 주기가 겹치면(조기 갱신 등) 가장 최근 결제를 쓴다.
     */
    Optional<Subscription> findFirstByAccountIdAndStartedAtLessThanEqualAndExpiresAtAfterOrderByStartedAtDesc(
            Long accountId, LocalDateTime startedAt, LocalDateTime expiresAt);

    default Optional<Subscription> findActive(Long accountId, LocalDateTime now) {
        return findFirstByAccountIdAndStartedAtLessThanEqualAndExpiresAtAfterOrderByStartedAtDesc(accountId, now, now);
    }

    /**
     * 이 주기의 리딩 1건 차감. 한도검사+증가를 한 문장으로 처리해 동시 요청이 한도를 넘지 못하게 한다.
     * @return 1 이면 차감 성공, 0 이면 한도 초과
     */
    @Modifying
    @Query("update Subscription s set s.readingUsed = s.readingUsed + 1 where s.id = :id and s.readingUsed < :limit")
    int tryConsume(@Param("id") Long id, @Param("limit") int limit);

    @Modifying
    @Query("update Subscription s set s.readingUsed = s.readingUsed - 1 where s.id = :id and s.readingUsed > 0")
    int refund(@Param("id") Long id);
}
