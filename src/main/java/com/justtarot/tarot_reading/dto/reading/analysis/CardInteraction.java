package com.justtarot.tarot_reading.dto.reading.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * 캐시된 카드 데이터를 재료로, 질문 문맥에 맞춰 카드 간 상호작용을 분석한 결과.
 * 2단계 서사 생성 뼈대로 쓸 구조를 만든다.
 */
public record CardInteraction(
        @JsonPropertyDescription("""
                슈트 분포 / 숫자 단계 / 메이저-마이너 비중을 근거로 한 전체 그림. \
                카드 낱장 의미를 나열하지 않는다.""")
        String flow,

        @JsonPropertyDescription("""
                서로 다른 카드의 두 상징이 반대되는 작동을 가리키는 쌍. 최대 3개. \
                양쪽 모두 pole 속성을 가져야 하며, a/b 조합은 따지지 않는다. 근거가 약하면 빈 배열.""")
        List<SymbolPair> tensions,

        @JsonPropertyDescription("""
                같은 축의 같은 쪽에 서거나 주제가 겹치는 쌍. 최대 3개. 근거가 약하면 빈 배열.""")
        List<SymbolPair> reinforcements,

        @JsonPropertyDescription("해석의 축이 되는 카드. 반드시 <spread>에 실제로 있는 <name> 값을 그대로 쓴다.")
        String pivotCardName,

        @JsonPropertyDescription("""
                그 카드가 축인 이유. '메이저라서', '가장 강한 카드라서'는 이유가 아니다. \
                위에서 고른 쌍에 많이 등장한다는 것도 이유가 아니다. \
                그 카드의 축이 다른 카드들의 무엇을 가르거나 잇는지로 적는다.""")
        String pivotReason,

        @JsonPropertyDescription("""
                위 분석이 질문의 어느 지점과 맞닿는지. 접점을 먼저 적고, 카드 상징의 영역과 질문의 영역이 \
                어긋나면 가장 중요한 어긋남 하나만 덧붙인다. 답을 내지 말고 접점만 적는다.""")
        String questionLink
) {
    public record SymbolPair(
            @JsonPropertyDescription("<spread>에 있는 카드 이름 그대로.")
            String fromCardName,

            @JsonPropertyDescription("fromCardName 카드의 <symbols> 안에 있는 name 값 그대로. 이 쌍의 근거가 되는 상징 하나.")
            String fromSymbol,

            @JsonPropertyDescription("<spread>에 있는 카드 이름 그대로.")
            String toCardName,

            @JsonPropertyDescription("toCardName 카드의 <symbols> 안에 있는 name 값 그대로.")
            String toSymbol,

            @JsonPropertyDescription("두 상징이 어떤 축에서 맞물리는지 한 문장. 상징 이름을 다시 나열하지 말고 관계만 적는다.")
            String reason
    ) {}
}
