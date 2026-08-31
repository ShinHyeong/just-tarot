package com.justtarot.tarot_reading.dto.analysis;

import java.util.List;

public record QuestionAnalysis(
        //LLM이 이해한 정보 정리
        String intent,
        String emotion,
        String situation,
        String category,

        boolean ambiguous, //카드해석 시작하기엔 정보가 부족한지
        List<String> candidates //만약 그렇다면 2~4개의 상황 후보가 채워진다.
) {
    //QuestionAnalyzer(LLM호출)이 실패했을 때 리딩 요청을 죽이지 않기 위한 최소 분석값
    public static QuestionAnalysis fallback(String question) {
        return new QuestionAnalysis(
                question,
                "명시되지 않음",
                "명시되지 않음",
        "기타",
                false,
                List.of()
        );
    }
}
