package com.justtarot.tarot_reading.service.question;

import com.justtarot.tarot_reading.dto.analysis.QuestionAnalysis;
import com.justtarot.tarot_reading.dto.request.ReadingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuestionAnalyzer {
    private static final String SYSTEM_PROMPT = """
            너는 타로 상담 전에 내담자의 질문을 정리하는 분석가다.
            점을 치지 말고, 카드를 언급하지 말고, 위로하지도 마라.
            질문 텍스트에 실제로 드러난 것만 추출한다.
 
            필드 규칙:
            - situation 은 질문에 명시된 사실만 적는다. 추측해서 채우지 마라.
              드러난 정황이 없으면 정확히 "명시되지 않음" 이라고 적는다.
            - emotion 은 질문의 어조에서 읽히는 감정을 한 단어에서 한 구절로 적는다.
            - category 는 다음 중 하나로만 고른다:
              짝사랑, 연애, 진로, 금전, 인간관계, 건강, 기타
 
            ambiguous가 규칙:
            - 다음 중 하나라도 해당하면 true 다.
              (1) 무엇에 대한 질문인지 주제가 특정되지 않음
              (2) 대상이나 상황이 전혀 없어 해석의 방향을 잡을 수 없음
              (3) 서로 다른 고민 두 개 이상이 섞여 있음
            - ambiguous가 true 면 candidates 에 사용자가 고를 만한 구체적인 상황 후보를
              2~4개, 각각 한 문장으로 적는다. ambiguous가 false 면 빈 배열이다.
            - 중요: ambiguous가 true 여도 intent/emotion/situation/category 는
              반드시 최선의 추정으로 채운다. 비워두거나 "알 수 없음"으로 도망가지 마라.
              되묻기가 실패해도 이 값들만으로 해석을 진행해야 하기 때문이다.
 
            질문이 타로 상담과 무관하거나(예: 코드 작성 요청, 욕설, 광고)
            해석할 내용이 없으면 category 를 "기타", ambiguous가 를 true 로 두고
            candidates 에 상담 가능한 주제를 제안하라.
            """;

    private final ChatClient chatClient;

    public QuestionAnalyzer(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultOptions(ChatOptions.builder().temperature(0.2)) // 분석이므로 창의성 불필요
                .build();
    }

    public QuestionAnalysis analyze(ReadingRequest request) {
        try {
            QuestionAnalysis analysis = chatClient.prompt()
                    .user("[질문]\n"+request.effectiveQuestion())
                    .call()
                    .entity(QuestionAnalysis.class);
            return analysis == null ? QuestionAnalysis.fallback(request.effectiveQuestion()) : analysis;

        } catch (Exception e) {
            //분석 실패로 리딩 전체를 실패시키지 않게
            log.warn("질문 분석 실패, fallback 하겠습니다. question: {}", request.effectiveQuestion());
            return QuestionAnalysis.fallback(request.effectiveQuestion());
        }
    }
}
