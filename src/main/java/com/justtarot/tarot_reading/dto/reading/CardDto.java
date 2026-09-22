package com.justtarot.tarot_reading.dto.reading;

import com.justtarot.tarot_reading.domain.card.Arcana;
import com.justtarot.tarot_reading.domain.card.Card;
import com.justtarot.tarot_reading.domain.card.Suit;
import com.justtarot.tarot_reading.domain.card.Symbol;

import java.util.List;

public record CardDto(
        int id,
        String name,
        Arcana arcana,
        Suit suit,
        int cardNumber,
        List<Symbol> symbols,
        List<String> themes,
        String uprightEnergy,
        String reversedEnergy,
        String axis
) {
    public static CardDto from(Card card) {
        return new CardDto(
                card.getId(),
                card.getNameKo(),
                card.getArcana(),
                card.getSuit(),
                card.getCardNumber(),
                card.getSymbols(),
                card.getThemes(),
                card.getUprightEnergy(),
                card.getReversedEnergy(),
                card.getAxis()
        );
    }
}
