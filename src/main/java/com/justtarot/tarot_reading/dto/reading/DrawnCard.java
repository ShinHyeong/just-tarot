package com.justtarot.tarot_reading.dto.reading;

public record DrawnCard(
        CardDto cardDto,
        boolean reversed
) {}
