package com.justtarot.tarot_reading.service.draw;

import com.justtarot.tarot_reading.domain.card.TotalCardsCache;
import com.justtarot.tarot_reading.dto.CardDto;
import com.justtarot.tarot_reading.dto.DrawnCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CardDrawer{
    private final SecureRandom secureRandom;
    private final TotalCardsCache totalCardsCache;

    public List<DrawnCard> draw(int drawCount){
        List<CardDto> pool = new ArrayList<>(totalCardsCache.getAll());

        List<DrawnCard> drawnCards = new ArrayList<>(drawCount);
        for (int i = 0; i < drawCount; i++){
            int drawnIndex = secureRandom.nextInt(pool.size());
            CardDto drawnCard = pool.remove(drawnIndex);

            boolean isReversed = secureRandom.nextBoolean();

            drawnCards.add(new DrawnCard(drawnCard, isReversed));
        }
        return drawnCards;
    }

}
