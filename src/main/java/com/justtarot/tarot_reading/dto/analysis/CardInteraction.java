package com.justtarot.tarot_reading.dto.analysis;

import java.util.List;

/**
 * 캐시된 카드 데이터를 재료로, 질문 문맥에 맞춰 카드 간 상호작용을 분석한다.
 * 2단계 서사 생성 뼈대로 쓸 구조를 만든다.
 * @param flow 메이저-마이너 비중을 포함한 서사 흐름
 * @param tensions 충돌하는 상징 쌍
 * @param reinforcements 서로 강화하는 상징 쌍
 * @param pivotCardName 해석의 축이 되는 카드 이름
 * @param pivotReason 그 카드가 축인 이유
 * @param questionLink 이 분석이 사용자 질문의 어느 지점과 맞닿는가
 */
public record CardInteraction(
        String flow,
        List<SymbolPair> tensions,
        List<SymbolPair> reinforcements,
        String pivotCardName,
        String pivotReason,
        String questionLink
) {
    public record SymbolPair(
            //ex. 짝사랑이 망해가는 상황이라면
            String fromCardName, // 타워(역방향)의 무너지는 탑
            String toCardName, //연인(정방향)의 두 인물
            String reason
    ) {}
}
