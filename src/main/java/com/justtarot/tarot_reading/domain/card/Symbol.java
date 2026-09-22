package com.justtarot.tarot_reading.domain.card;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Symbol {
    private String symbol;
    private String meaning;
    private String pole;
}
