package com.justtarot.tarot_reading.dto.reading.response;

import com.justtarot.tarot_reading.dto.reading.DrawnCard;

import java.util.List;

/**
 * 클라이언트가 쓸 카드 정보
 * @param id 클라이언트가 추후 카드 이미지 찾는데 사용할 용도
 * @param name
 * @param reversed
 */
public record DrawnCardResponse(
        int id,
        String name,
        boolean reversed
) {
    public static DrawnCardResponse from(DrawnCard drawnCard) {
        return new DrawnCardResponse(
                drawnCard.cardDto().id(),
                drawnCard.cardDto().name(),
                drawnCard.reversed()
        );
    }

    public static List<DrawnCardResponse> from(List<DrawnCard> drawnCards) {
        return drawnCards.stream()
                .map(DrawnCardResponse::from)
                .toList();
    }
}
