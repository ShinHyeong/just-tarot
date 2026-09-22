package com.justtarot.tarot_reading.domain.reading;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert //빈 필드들이 Null로 덮어써지는 것을 막기 위해서
public class Reading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(length = 255)
    private String clarification;

    private LocalDateTime createdAt;

    public Reading(Long userId, String question, String clarification) {
        this.userId = userId;
        this.question = question;
        this.clarification = clarification;
    }
}
