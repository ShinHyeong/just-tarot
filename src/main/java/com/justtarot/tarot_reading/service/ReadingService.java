package com.justtarot.tarot_reading.service;

import com.justtarot.tarot_reading.dto.reading.DrawnCard;
import com.justtarot.tarot_reading.dto.reading.analysis.CardInteraction;
import com.justtarot.tarot_reading.dto.reading.analysis.QuestionAnalysis;
import com.justtarot.tarot_reading.dto.reading.request.PreparedReadingRequest;
import com.justtarot.tarot_reading.dto.reading.request.ReadingRequest;
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

        if (analysis.offTopic()) {
            log.info("타로 상담 대상이 아닌 요청입니다. userId={}", userId);
            return PreparedReadingRequest.notSupported();   // 카드 X, DB 기록 X, 이후 LLM 호출 X
        }

        //되묻기는 1차 요청에서 1번만, 물어볼 후보가 확보되었을 때만: 사용자에게 빈 배열을 제안하지 않게
        if (!request.clarified() && analysis.needsClarification()) {
            return PreparedReadingRequest.needsClarification(analysis.candidate());
        }

        //2차 요청까지 한 질문이 모호하든 말든 이후 프로세스를 진행한다
        //2차 요청까지 한 질문이 모호할 경우 로그를 남긴다
        if (request.clarified() && analysis.ambiguous()) {
            log.info("2차 시도에도 질문이 모호합니다. 모호한 상태로 진행합니다. userId={}", userId);
        }

        List<DrawnCard> drawnCards = cardDrawer.draw(request.drawCount());
        Long readingId = readingRecorder.record(userId, request.question(), request.clarification(), drawnCards);
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