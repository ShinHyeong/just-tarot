package com.justtarot.tarot_reading.dto.reading.response;

import com.justtarot.tarot_reading.dto.reading.DrawnCard;

import java.util.List;

public record ReadingResponse(
        String type,
        List<com.justtarot.tarot_reading.dto.reading.response.DrawnCardResponse> cards,
        String interpretation
) {
    public static ReadingResponse of(List<DrawnCard> cards,
                                     String interpretation) {
        return new ReadingResponse(
                "READING",
                com.justtarot.tarot_reading.dto.reading.response.DrawnCardResponse.from(cards),
                interpretation
        );
    }
}
