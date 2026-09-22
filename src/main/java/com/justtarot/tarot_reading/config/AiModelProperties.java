package com.justtarot.tarot_reading.config;


import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 프로세스(호출 지점)별 모델 설정.
 * 모델 교체가 코드 수정이 아니라 설정 변경이 되도록 application.yaml 로 뺐다.
 *
 * @param questionAnalyzer  [1] 질문 분석
 * @param analyst           [3-1] 질문 맥락에 맞는 카드 상호작용 분석
 * @param reader            [3-2] 서사 생성
 */
@ConfigurationProperties(prefix = "tarot.ai")
public record AiModelProperties(
        ModelSpec questionAnalyzer,
        ModelSpec analyst,
        ModelSpec reader
) {
    /**
     * @param model           모델 ID
     * @param reasoningEffort none | minimal | low | medium | high | xhigh | max (모델별 지원 범위 다름)
     * @param verbosity       low | medium | high
     * @param store           로그 저장
     */
    public record ModelSpec(
            String model,
            @DefaultValue("medium") String reasoningEffort,
            @DefaultValue("medium") String verbosity,
            @DefaultValue("false") boolean store
    ) {
        public OpenAiChatOptions.Builder toOptions() {
            OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                    .model(model)
                    .reasoningEffort(reasoningEffort)
                    .verbosity(verbosity);
            if (store) {
                builder.store(true);
            }
            return builder;
        }
    }
}
