package com.justtarot.tarot_reading.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "account") // user 는 MySQL 예약어라 테이블명은 account
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private int freeReadingUsed;

    private LocalDate freeReadingCycleStart;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now(KST);
    }
}
