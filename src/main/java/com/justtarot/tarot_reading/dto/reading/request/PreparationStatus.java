package com.justtarot.tarot_reading.dto.reading.request;

public enum PreparationStatus {
    READY,          // 카드까지 뽑았고 해석으로 넘어간다
    CLARIFICATION,  // 되물어야 한다
    NOT_SUPPORTED   // 타로로 답할 요청이 아니다
}
