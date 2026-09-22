package com.justtarot.tarot_reading.domain.card;

public enum Suit {
    NONE("없음"),
    WANDS("완드(의지, 행동)"),
    CUPS("컵(감정, 관계)"),
    SWORDS("소드(사고, 갈등)"),
    PENTACLES("펜타클(현실, 물질)");

    private final String label;

    Suit(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
