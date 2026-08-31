package com.justtarot.tarot_reading.domain.card;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Arcana arcana;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Suit suit;

    @Column(nullable = false)
    private int cardNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Symbol> symbols;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> themes;

    @Column(nullable = false)
    private String uprightEnergy;

    @Column(nullable = false)
    private String reversedEnergy;

    @Column(nullable = false)
    private String tension;
}
