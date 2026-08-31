package com.justtarot.tarot_reading.controller;

import com.justtarot.tarot_reading.dto.request.PreparedReadingRequest;
import com.justtarot.tarot_reading.dto.request.ReadingRequest;
import com.justtarot.tarot_reading.dto.response.ClarificationResponse;
import com.justtarot.tarot_reading.dto.response.ReadingResponse;
import com.justtarot.tarot_reading.service.ReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReadingController {
    private final ReadingService readingService;

    /* 스트리밍 X 버전 */
    @PostMapping("/reading")
    public ResponseEntity<?> read(@RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody ReadingRequest request){

        PreparedReadingRequest preparedRequest = readingService.prepare(userId, request);
        if (preparedRequest.ambiguous()) {
            return ResponseEntity.ok(ClarificationResponse.of(request, preparedRequest.candidates()));
        }

        return ResponseEntity.ok(ReadingResponse.of(preparedRequest.cards(), readingService.interpret(preparedRequest)));
    }

    /* 스트리밍 O 버전 */
    @PostMapping(value = "/reading/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> readStream(@RequestHeader("X-User-Id") Long userId,
                                                    @Valid @RequestBody ReadingRequest request) {
        PreparedReadingRequest preparedRequest = readingService.prepare(userId, request);
        if (preparedRequest.ambiguous()) {
            return Flux.just(event("clarification",
                    ClarificationResponse.of(request, preparedRequest.candidates())));
        }

        Flux<ServerSentEvent<Object>> cards = Flux.just(event("card", preparedRequest.cards()));

        Flux<ServerSentEvent<Object>> tokens = readingService.interpretStream(preparedRequest)
                .map(token -> event("token", token));

        return Flux.concat(cards, tokens, Flux.just(event("done", preparedRequest.readingId())))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Flux.just(event("error", "해석 중 오류가 발생했습니다. 잠시후 다시 시도해주세요.")));
    }

    private ServerSentEvent<Object> event(String name, Object data) {
        return ServerSentEvent.builder().event(name).data(data).build();
    }
}
