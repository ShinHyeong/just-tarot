package com.justtarot.tarot_reading.dto.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 앞 3자리 : HTTP 상태코드
 * 마지막 자리 : 세부케이스 구분
 */
@Getter
@RequiredArgsConstructor
public enum StatusCode {
    SUCCESS(2000, "요청에 성공했습니다"),

    CREATED(2010, "생성되었습니다"),

    ACCEPTED(2020, "요청이 접수되었습니다"),

    INVALID_INPUT(4000, "잘못된 요청입니다."),

    READING_ACCESS_DENIED(4030, "해당 리딩 이력에 대한 권한이 없습니다."),

    READING_NOT_FOUND(4040, "해당 리딩 이력을 찾을 수 없습니다."),

    CLARIFICATION_REQUIRED(4221, "질문을 좀더 구체적으로 알려주세요"),
    QUESTION_NOT_SUPPORTED(4222, "타로로는 마음이나 지금 처한 상황에 대한 고민을 봐드릴 수 있어요. 요즘 어떤 게 마음에 걸리시는지 적어주시면 그걸로 봐드릴게요."),

    INTERNAL_ERROR(5000, "서버 내부 오류가 발생했습니다."),
    READING_FAILED(5001, "해석 중 오류가 발생했습니다. 잠시후 다시 시도해주세요.");

    private final int code;
    private final String message;
}