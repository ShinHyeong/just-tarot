package com.justtarot.tarot_reading.domain.subscription;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

/**
 * 구독 결제 1회 = 1행. startedAt(결제일) ~ expiresAt 이 한 결제 주기이고,
 * 갱신 결제는 새 행을 만들므로 readingUsed 가 자연스럽게 0부터 다시 시작한다 (결제일 기준 리셋).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Plan plan;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int readingUsed;

    private LocalDateTime createdAt;

    public Subscription(Long accountId, Plan plan, LocalDateTime startedAt, LocalDateTime expiresAt) {
        this.accountId = accountId;
        this.plan = plan;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }
}
