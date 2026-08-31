package com.justtarot.tarot_reading.dto.response;

import com.justtarot.tarot_reading.dto.DrawnCard;
import com.justtarot.tarot_reading.dto.request.ReadingRequest;

import java.util.List;

public record ReadingResponse(
        String type,
        List<DrawnCard> cards,
        String interpretation
) {
    public static ReadingResponse of(List<DrawnCard> cards,
                                     String interpretation) {
        return new ReadingResponse(
                "READING",
                cards,
                interpretation
        );
    }
}
