package com.justtarot.tarot_reading.service.interpretation;

import com.justtarot.tarot_reading.domain.card.Arcana;
import com.justtarot.tarot_reading.domain.card.Symbol;
import com.justtarot.tarot_reading.dto.reading.CardDto;
import com.justtarot.tarot_reading.dto.reading.DrawnCard;
import com.justtarot.tarot_reading.dto.reading.analysis.CardInteraction;
import com.justtarot.tarot_reading.dto.reading.analysis.QuestionAnalysis;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

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

        appendSpread(sb, drawnCards, this::describe);

        return sb.toString();
    }

    public String buildNarrativePrompt(String question,
                                       QuestionAnalysis analysis,
                                       List<DrawnCard> drawnCards,
                                       CardInteraction interaction) {
        StringBuilder sb = new StringBuilder();

        appendQuestionContext(sb, question, analysis);

        appendSpread(sb, drawnCards, this::describeBrief);

        appendInteraction(sb, interaction);

        return sb.toString();
    }

    /* 사용자 입력이 태그 경계를 위조하지 못하게 막는다. */
    private String escape(String raw) {
        if (raw == null) { return ""; }
        return raw.replace("<", "&lt;").replace(">", "&gt;");
    }

    private void appendQuestionContext(StringBuilder sb, String question, QuestionAnalysis analysis) {
        sb.append("<question>\n").append(escape(question)).append("\n</question>\n\n");
        sb.append("<question_analysis>\n")
                .append("  <intent>").append(escape(analysis.intent())).append("</intent>\n")
                .append("  <emotion>").append(escape(analysis.emotion())).append("</emotion>\n")
                .append("  <situation>").append(escape(analysis.situation())).append("</situation>\n")
                .append("  <category>").append(escape(analysis.category())).append("</category>\n")
                .append("  <ambiguous>").append(analysis.ambiguous() ? "true" : "false").append("</ambiguous>\n");
        sb.append("</question_analysis>\n\n");
    }

    /**
    * 프롬프트의 카드 정보 파트 구성
    */
    private void appendSpread(StringBuilder sb,
                              List<DrawnCard> drawnCards,
                              Function<DrawnCard, String> describer) {
        sb.append("<spread>\n");
        for (DrawnCard drawnCard : drawnCards) {
            sb.append("  <card>\n")
                .append(describer.apply(drawnCard));
            sb.append("  </card>\n");
        }
        sb.append("</spread>\n\n");
    }

    /* 1단계(질문 맥락에 맞는 카드간 상호작용 분석)를 위해 필요한 카드 정보*/
    private String describe(DrawnCard drawnCard) {
        CardDto c = drawnCard.cardDto();
        boolean reversed = drawnCard.reversed();
        String classification = c.arcana() == Arcana.MAJOR
                ? "메이저 / 번호 " + c.cardNumber()
                : c.suit().label() + " / 번호 " + c.cardNumber();

        StringBuilder sb = new StringBuilder();
        sb.append("    <name>").append(escape(c.name())).append("</name>\n");
        sb.append("    <orientation>").append(reversed ? "역방향" : "정방향").append("</orientation>\n");
        sb.append("    <classification>").append(classification).append("</classification>\n");
        sb.append("    <energy>").append(escape(reversed ? c.reversedEnergy() : c.uprightEnergy())).append("</energy>\n");
        sb.append("    <axis>").append(escape(c.axis())).append("</axis>\n");
        sb.append("    <symbols>\n");
        for (Symbol s : c.symbols()) {
            sb.append("      <symbol name=\"").append(escape(s.getSymbol())).append("\"");
            if (s.getPole() != null) {
                sb.append(" pole=\"").append(escape(s.getPole())).append("\"");
            }
            sb.append(">").append(escape(s.getMeaning())).append("</symbol>\n");
        }
        sb.append("    </symbols>\n");
        sb.append("    <themes>\n");
        for (String theme : c.themes()) {
            sb.append("      <theme>").append(escape(theme)).append("</theme>\n");
        }
        sb.append("    </themes>\n");

        return sb.toString();
    }

    /* 2단계(사용자 서사 생성)에서는 간소화된 카드 정보만 LLM에게 제공*/
    private String describeBrief(DrawnCard drawnCard) {
        CardDto c = drawnCard.cardDto();
        boolean reversed = drawnCard.reversed();

        StringBuilder sb = new StringBuilder();
        sb.append("    <name>").append(escape(c.name())).append("</name>\n");
        sb.append("    <orientation>").append(reversed ? "역방향" : "정방향").append("</orientation>\n");
        sb.append("    <energy>").append(escape(reversed ? c.reversedEnergy() : c.uprightEnergy())).append("</energy>\n");

        return sb.toString();
    }

    private void appendInteraction(StringBuilder sb, CardInteraction interaction) {
        sb.append("<interaction>\n")
            .append("  <flow>").append(escape(interaction.flow())).append("</flow>\n")
            .append("  <pivot>\n")
            .append("    <card>").append(escape(interaction.pivotCardName())).append("</card>\n")
            .append("    <reason>").append(escape(interaction.pivotReason())).append("</reason>\n")
            .append("  </pivot>\n")
            .append("  <question_link>").append(escape(interaction.questionLink())).append("</question_link>\n");
        appendPairs(sb, "tensions", interaction.tensions());
        appendPairs(sb, "reinforcements", interaction.reinforcements());
        sb.append("</interaction>\n\n");
    }

    private void appendPairs(StringBuilder sb, String tag, List<CardInteraction.SymbolPair> pairs) {
        if (pairs == null || pairs.isEmpty()) { return; }
        sb.append("  <").append(tag).append(">\n");
        for (CardInteraction.SymbolPair pair : pairs) {
            sb.append("    <pair>\n")
                    .append("      <from symbol=\"").append(escape(pair.fromSymbol())).append("\">")
                    .append(escape(pair.fromCardName())).append("</from>\n")
                    .append("      <to symbol=\"").append(escape(pair.toSymbol())).append("\">")
                    .append(escape(pair.toCardName())).append("</to>\n")
                    .append("      <reason>").append(escape(pair.reason())).append("</reason>\n")
                    .append("    </pair>\n");
        }
        sb.append("  </").append(tag).append(">\n");
    }
}
