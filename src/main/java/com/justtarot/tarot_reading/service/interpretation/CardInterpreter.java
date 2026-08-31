package com.justtarot.tarot_reading.service.interpretation;

import com.justtarot.tarot_reading.dto.analysis.CardInteraction;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * LLM 호출만 담당한다
 * 시스템 프롬프트만 작성 - 요청마다 바뀌지 않는 것(역할, 금지사항, 출력 형식, 안전 규칙 등)
 */
@Component
public class CardInterpreter {
    private static final String ANALYST_SYSTEM = """
            너는 타로 상징 체계를 구조적으로 분석하는 사전 분석가다.
            - 주어지는 카드 데이터는 상징 사전일 뿐이다.
              문장을 그대로 옮기지 말고 재료로만 사용하라.
            - 점을 치지 마라. 위로하지 마라. 조언하지 마라.
              카드들 사이의 구조만 본다.
            - 근거 없는 연결을 만들어내지 마라. 확실하지 않으면 적지 않는다.
            """;

    private static final String READER_SYSTEM = """
            너는 20년 경력의 타로 리더다. 공감 능력이 뛰어나지만 과장하지 않는다.
            내담자를 겁주거나 근거 없이 안심시키지 않는다.
            운명을 확정하지 않고, 지금의 흐름과 선택지를 보여준다.
 
            항상 지키는 작성 규칙:
            - 카드별 사전적 의미를 나열하지 마라. 카드 이름은 자연스럽게 언급하되
              "이 카드는 ~를 의미합니다" 식의 설명체는 쓰지 않는다.
            - 단정하지 마라. "~일 것입니다" 대신 "~로 보입니다", "~쪽에 가깝습니다".
            - 마지막 문단에 오늘 당장 해볼 수 있는 구체적인 행동 하나를 제안한다.
            - 질병, 죽음, 소송, 임신 여부를 확정적으로 말하지 않는다.
              그런 질문이면 전문가 상담을 권하는 문장을 포함한다.
            - 분량은 공백 포함 500자 내외, 문단은 2~3개.
            - 마크다운 문법(**, ##, - )을 쓰지 말고 평범한 문장으로 쓴다.
            """;

    private final ChatClient analystClient;
    private final ChatClient readerClient;

    public CardInterpreter(ChatClient.Builder builder) {
        this.analystClient = builder.clone()
                .defaultSystem(ANALYST_SYSTEM)
                .defaultOptions(ChatOptions.builder().temperature(0.3))
                .build();
        this.readerClient = builder.clone()
                .defaultSystem(READER_SYSTEM)
                .defaultOptions(ChatOptions.builder().temperature(0.8))
                .build();
    }

    /**
     * 1단계 : 카드간 상호작용 분석
     * 캐시된 카드 데이터를 재료로, 질문 문맥에 맞춰 카드 간 상호작용을 분석한다.
     */
    public CardInteraction analyzeInteraction(String prompt){
        return analystClient.prompt()
                .user(prompt)
                .call()
                .entity(CardInteraction.class);
    }

    /** 2단계 : 서사 생성(최종 결과)
     * 사용자 질문에 맞는 하나의 이어지는 이야기로 해석을 작성
     */
    public String narrate(String prompt){
        return readerClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 2단계를 토큰 단위로 실시간 스트리밍하는 버전
     */
    public Flux<String> narrateStream(String prompt){
        return readerClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
