package com.justtarot.tarot_reading.service.interpretation;

import com.justtarot.tarot_reading.dto.CardDto;
import com.justtarot.tarot_reading.dto.DrawnCard;
import com.justtarot.tarot_reading.dto.analysis.CardInteraction;
import com.justtarot.tarot_reading.dto.analysis.QuestionAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * "이번 요청"의 데이터 전처리 + 지시만 프롬프트로 만든다
 * 지시는 데이터 뒤에 둔다 : 긴 데이터 블록 앞에 지시를 두면 모델이 중간에 지시를 잃는 경우가 있기 때문에
 * Spring AI의 PromptTemplate(String Template기반)은 '{'을 플레이스홀더로 해석한다.
 * 사용자 질문에 중괄호가 섞이면 렌더링이 깨지므로 템플릿 엔진을 쓰지 않고 직접 조립하였다.
 */
@Component
public class PromptBuilder {
    public String buildInteractionPrompt(String question,
                                         QuestionAnalysis analysis,
                                         List<DrawnCard> drawnCards) {
        StringBuilder sb = new StringBuilder();

        appendQuestionContext(sb, question, analysis);
        appendSpread(sb, drawnCards);

        sb.append("""
                위 카드 조합에 대해 다음을 분석하라.
                1. flow: 세 축(원소 흐름 / / 메이저-마이너 비중)을 근거로 한 서사 흐름
                2. tensions: 카드들 사이에서 충돌하거나 긴장을 만드는 상징 쌍
                3. reinforcements: 서로 강화하는 상징 쌍
                4. pivotCardName, pivotReason: 이 조합에서 해석의 축이 되는 카드와 그 이유
                5. questionLink: 위 분석이 사용자 질문의 어떤 지점과 맞닿는지

                tensions 와 reinforcements 는 각각 최대 3쌍까지만 고른다.
                억지로 채우지 말고, 근거가 약하면 적게 적어라.
                
                """);

        return sb.toString();
    }

    public String buildNarrativePrompt(String question, QuestionAnalysis analysis, List<DrawnCard> drawnCards,  CardInteraction interaction) {
        StringBuilder sb = new StringBuilder();

        appendQuestionContext(sb, question, analysis);
        appendSpread(sb, drawnCards);
        appendInteraction(sb, interaction);

        sb.append("""
                위 상호작용 분석을 뼈대로, 사용자 질문에 답하는 하나의 이어지는 이야기를 써라.
                tensions 를 이야기의 갈등으로, pivot 을 전환점으로 사용하라.
                
                """);

        // 되물었는데도 질문이 여전히 모호한 채로 넘어온 경우.
        // 해석을 거부하는 대신 톤을 조정한다.
        if (analysis.ambiguous()) {
            sb.append("""
                    참고: 이 질문은 상황이 충분히 구체적이지 않다.
                    특정 인물이나 사건을 단정해서 지목하지 말고,
                    사용자가 자기 상황에 대입해 읽을 수 있도록 조금 더 열린 문장으로 써라.
                    마지막에 "어떤 상황을 떠올리며 물으셨는지 알려주시면 더 좁혀서 볼 수 있어요"
                    같은 한 문장을 자연스럽게 덧붙여라.
                    
                    """);
        }

        return sb.toString();
    }

    private void appendQuestionContext(StringBuilder sb, String question, QuestionAnalysis analysis) {
        sb.append("[질문]\n").append(question).append("\n\n");
        sb.append("[질문 분석]\n")
                .append("- 의도: ").append(analysis.intent()).append("\n")
                .append("- 감정: ").append(analysis.emotion()).append("\n")
                .append("- 정황: ").append(analysis.situation()).append("\n")
                .append("- 주제: ").append(analysis.category()).append("\n");
        sb.append("\n");
    }

    private void appendSpread(StringBuilder sb, List<DrawnCard> drawnCards) {
        sb.append("[스프레드]\n");
        for (DrawnCard drawnCard : drawnCards) {
            sb.append(describe(drawnCard)).append("\n");
        }
        sb.append("\n");
    }

    private String describe(DrawnCard drawnCard) {
        CardDto cardInfo = drawnCard.cardDto();
        boolean reversed = drawnCard.reversed();
        String symbolsToString = cardInfo.symbols().stream()
                .map(s -> s.getSymbol()+"("+s.getMeaning()+")")
                .collect(Collectors.joining(", "));
        return String.format("""
                    %s (%s)
                    - 아르카나: %s / 원소: %s / 숫자: %d
                    - 지금 방향의 에너지: %s
                    - 이 카드가 품은 긴장: %s
                    - 상징: %s
                    - 주제: %s
                """,
                cardInfo.name(),
                reversed ? "역방향" : "정방향",
                cardInfo.arcana(), cardInfo.suit(), cardInfo.cardNumber(),
                reversed ? cardInfo.reversedEnergy() : cardInfo.uprightEnergy(),
                cardInfo.tension(),
                symbolsToString,
                String.join(", ", cardInfo.themes())
        );
    }

    private void appendInteraction(StringBuilder sb, CardInteraction interaction) {
        sb.append("[상호작용 분석]\n")
                .append("- 흐름: ").append(interaction.flow()).append('\n')
                .append("- 축이 되는 카드: ").append(interaction.pivotCardName()).append("\n")
                .append(" — ").append(interaction.pivotReason()).append('\n')
                .append("- 질문과의 접점: ").append(interaction.questionLink()).append('\n');

        appendPairs(sb, "긴장", interaction.tensions());
        appendPairs(sb, "강화", interaction.reinforcements());
        sb.append("\n");
    }

    private void appendPairs(StringBuilder sb, String label, List<CardInteraction.SymbolPair> pairs) {
        if (pairs==null || pairs.isEmpty()) { return; }
        sb.append("- ").append(label).append(":\n");
        for (CardInteraction.SymbolPair pair : pairs) {
            sb.append("  - ").append(pair.fromCardName()).append(" <-> ").append(pair.toCardName())
                    .append(" : ").append(pair.reason())
                    .append("\n");
        }
    }
}
