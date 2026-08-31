package com.justtarot.tarot_reading.service;

import com.justtarot.tarot_reading.dto.DrawnCard;
import com.justtarot.tarot_reading.dto.analysis.CardInteraction;
import com.justtarot.tarot_reading.dto.analysis.QuestionAnalysis;
import com.justtarot.tarot_reading.dto.request.PreparedReadingRequest;
import com.justtarot.tarot_reading.dto.request.ReadingRequest;
import com.justtarot.tarot_reading.service.draw.CardDrawer;
import com.justtarot.tarot_reading.service.interpretation.CardInterpreter;
import com.justtarot.tarot_reading.service.interpretation.PromptBuilder;
import com.justtarot.tarot_reading.service.question.QuestionAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingService {
    private final QuestionAnalyzer questionAnalyzer;
    private final CardDrawer cardDrawer;
    private final ReadingRecorder readingRecorder;
    private final PromptBuilder promptBuilder;
    private final CardInterpreter cardInterpreter;

    /**
     * 카드 해석 전 작업 : 질문 분석 -> 카드 뽑기 -> 이력 기록
     * 질문이 모호하다면 사용자에게 되묻는다. 이탈 방지를 위해 되묻기는 딱 한 번만 한다.
     */
    public PreparedReadingRequest prepare(Long userId, ReadingRequest request) {
        QuestionAnalysis analysis = questionAnalyzer.analyze(request);

        if (analysis.ambiguous() && !request.isRetry()) { //한번도 되물음 당하지 않았다면
            return PreparedReadingRequest.needsClarification(analysis.candidates());
        }

        if (request.isRetry() && analysis.ambiguous()) {
            log.info("2차 시도에도 질문이 모호합니다. 모호한 상태로 진행합니다. userId={}", userId);
        }

        List<DrawnCard> drawnCards = cardDrawer.draw(request.drawCount());
        Long readingId = readingRecorder.record(userId, request.effectiveQuestion(), drawnCards);
        return PreparedReadingRequest.ready(readingId, request.effectiveQuestion(), analysis, drawnCards);
    }

    /**
     *  해석 시작 (스트리밍 X 버전)
     */
    public String interpret(PreparedReadingRequest request) {
        return cardInterpreter.narrate(narrativePrompt(request.effectiveQuestion() , request.questionAnalysis(), request.cards()));
    }

    /**
     *  해석 시작 (스트리밍 O 버전)
     */
    public Flux<String> interpretStream(PreparedReadingRequest request) {
        return cardInterpreter.narrateStream(narrativePrompt(request.effectiveQuestion() , request.questionAnalysis(), request.cards()));
    }

    private String narrativePrompt(String question, QuestionAnalysis analysis, List<DrawnCard> drawnCards) {
        String interactionPrompt = promptBuilder.buildInteractionPrompt(question, analysis, drawnCards);
        CardInteraction interaction = cardInterpreter.analyzeInteraction(interactionPrompt);
        return promptBuilder.buildNarrativePrompt(question, analysis, drawnCards, interaction);
    }
}