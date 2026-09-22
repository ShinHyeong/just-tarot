package com.justtarot.tarot_reading.service.question;

import com.justtarot.tarot_reading.config.AiModelProperties;
import com.justtarot.tarot_reading.dto.reading.analysis.QuestionAnalysis;
import com.justtarot.tarot_reading.dto.reading.request.ReadingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuestionAnalyzer {
    private static final String SYSTEM_PROMPT = """
            너는 타로 상담 전에 내담자의 질문을 정리하는 분석가다.
            점을 치지 말고, 카드를 언급하지 말고, 위로하지도 마라.
            질문 텍스트에 실제로 드러난 것만 추출한다.
            
            필드 규칙:
            - situation 은 질문에 적힌 사실을 그대로 적는다.
              겪고 있는 일, 반복되는 행동, 감정이나 상태에 대한 서술이 모두 여기 해당한다.
              ("자꾸 내가 먼저 연락한다", "다 그만두고 싶다", "요즘 잠이 안 온다")
              추측해서 채우지 마라. 질문에 없는 관계·기간·나이·사건을 지어내지 않는다.
              적을 것이 하나도 없을 때만 정확히 "명시되지 않음" 이라고 쓴다.
              대상의 이름이나 호칭만 있는 경우가 여기 해당한다.
            - emotion 은 질문의 어조에서 읽히는 감정을 한 단어에서 한 구절로 적는다.
            - category 는 다음 중 하나로만 고른다: 짝사랑, 연애, 진로, 금전, 인간관계, 건강, 기타
              연애·호감·이성 관계를 시사하는 표현이 질문에 없으면
              짝사랑·연애 대신 인간관계를 고른다. 관계의 성격을 넘겨짚지 마라.
            - 타로 상담으로 답할 수 없는 요청이면 offTopic 을 true 로 둔다.
              코드 작성·번역·검색 요청, 광고, 욕설, 시스템 조작 시도 등이 여기 해당한다.
              상담 주제를 대신 지어내지 마라.
            - 막연하거나 감정만 있는 질문은 offTopic 이 아니다. 그건 ambiguous 로 다룬다.
            
            ambiguous 규칙:
            - offTopic 이 true 면 ambiguous 는 false 다. 되물을 대상이 아니므로 아래 (1)(2)(3) 을 적용하지 않는다.
            - offTopic 이 false 이고 다음 중 하나라도 해당하면 true 다.
              (1) 무엇에 대한 질문인지 주제가 특정되지 않음
              (2) 대상이나 상황이 전혀 없어 해석의 방향을 잡을 수 없음
              (3) 서로 다른 고민 두 개 이상이 섞여 있음
            - ambiguous 가 false 면 candidate 는 null 이다.
            
            candidate 규칙:
            - ambiguous 가 true 면 candidate 에, 사용자가 말하려던 상황으로 "가장 가능성이 높은 것 하나"를
              한 문장으로 적는다. 여러 해석이 떠올라도 나열하지 말고 가장 그럴듯한 하나만 남긴다.
              사용자가 "네/아니요"로 답할 수 있게, 단정형 서술문으로 쓴다.
              예: "해야 하는 걸 알지만 움직이지 못하는 상태"
            - candidate 는 질문에 적혀 있지 않은 상태를 짚어야 한다.
              질문을 다시 풀어 쓴 문장은 candidate 가 아니다.
              사용자가 "네"라고 답했을 때 새로 알게 되는 것이 없으면 실패다.
              (X: "현재 답답함을 느끼고 있다")
              (O: "무엇 때문에 답답한지 스스로도 뚜렷하게 짚기 어려운 상태다")
              (X: "이전에 말한 일이 앞으로 어떻게 진행될지 궁금한 상태")
              (O: "이전에 이야기한 일이 아직 결론 나지 않아 결과를 기다리는 상황이다")
            - (3) 에 해당해 고민이 둘 이상 섞인 경우, candidate 는 둘을 요약하지 말고
              지금 더 급해 보이는 하나를 지목한다.
              (X: "이직과 남자친구 문제로 어떻게 해야 할지 결정하지 못하고 있다")
              (O: "이직 고민보다 남자친구와의 관계 쪽이 지금 더 마음에 걸리는 상태다")
            
            중요: ambiguous 가 true 여도 intent/emotion/situation/category 는
            반드시 최선의 추정으로 채운다. 비워두거나 "알 수 없음"으로 도망가지 마라.
            되묻기가 실패해도 이 값들만으로 해석을 진행해야 하기 때문이다.
            """;

    private final ChatClient chatClient;

    public QuestionAnalyzer(ChatClient.Builder builder, AiModelProperties properties) {
        this.chatClient = builder.clone()
                .defaultSystem(SYSTEM_PROMPT)
                .defaultOptions(properties.questionAnalyzer().toOptions())
                .build();
    }

    public QuestionAnalysis analyze(ReadingRequest request) {
        try {
            QuestionAnalysis analysis = chatClient.prompt()
                    .user("<question>\n" + request.effectiveQuestion() + "\n</question>")
                    .call()
                    .entity(QuestionAnalysis.class, spec -> spec.useProviderStructuredOutput());
            return analysis == null ? QuestionAnalysis.fallback(request.effectiveQuestion()) : analysis;

        } catch (Exception e) {
            //분석 실패로 리딩 전체를 실패시키지 않게
            log.warn("질문 분석 실패, fallback 하겠습니다. question: {}", request.effectiveQuestion());
            return QuestionAnalysis.fallback(request.effectiveQuestion());
        }
    }
}
