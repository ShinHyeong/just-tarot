package com.justtarot.tarot_reading.controller;

import com.justtarot.tarot_reading.dto.global.response.ApiResponse;
import com.justtarot.tarot_reading.dto.reading.request.PreparedReadingRequest;
import com.justtarot.tarot_reading.dto.reading.request.ReadingRequest;
import com.justtarot.tarot_reading.dto.global.response.StatusCode;
import com.justtarot.tarot_reading.dto.reading.response.DrawnCardResponse;
import com.justtarot.tarot_reading.dto.reading.response.ReadingResponse;
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

    /* 스트리밍 X 버전 — 되묻기·주제 밖 질문은 예외로 던져져 GlobalExceptionHandler 가 응답한다 */
    @PostMapping("/reading")
    public ResponseEntity<?> read(@RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody ReadingRequest request){
        PreparedReadingRequest preparedRequest = readingService.prepare(userId, request);

        return ResponseEntity.ok(ApiResponse.success(
                ReadingResponse.of(preparedRequest.cards(), readingService.interpret(preparedRequest))));
    }

    /* 스트리밍(SSE) 버전 — 되묻기·주제 밖 질문은 prepare 동기 구간에서 던져져 SSE 시작 전에 JSON 으로 반환된다 */
    @PostMapping(value = "/reading/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> readStream(@RequestHeader("X-User-Id") Long userId,
                                                    @Valid @RequestBody ReadingRequest request) {
        PreparedReadingRequest preparedRequest = readingService.prepare(userId, request);

        // 네트워크 낭비 방지을 위해 SSE event 필드를 구분자로 쓰고 순수 문자열만을 전송할 수도 있지만,
        // 다음과 같은 이유로 JSON 래핑이 부하보다 이점이 더 크다고 판단험
        // 1. 띄어쓰기 보존: Raw 전송 시 SSE 파서 규격에 의해 토큰 앞뒤 띄어쓰기가 누락되는 문제 방지 (JSON 직렬화 활용).
        // 2. FE 파싱 일관성 : 모든 이벤트(error/card/token)의 응답 구조를 통일하여 프론트엔드 파싱 일관성
        Flux<ServerSentEvent<Object>> cards =
                Flux.just(event("card",
                        ApiResponse.success(DrawnCardResponse.from(preparedRequest.cards()))));

        Flux<ServerSentEvent<Object>> tokens =
                Flux.defer(() -> readingService.interpretStream(preparedRequest))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(token -> event("token",
                                ApiResponse.success(token)));

        return Flux.concat(cards, tokens, Flux.just(event("done", preparedRequest.readingId())))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Flux.just(event("error",
                        ApiResponse.fail(StatusCode.READING_FAILED))));
    }

    private ServerSentEvent<Object> event(String name, Object data) {
        return ServerSentEvent.builder().event(name).data(data).build();
    }
}
