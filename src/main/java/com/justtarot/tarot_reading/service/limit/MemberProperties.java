package com.justtarot.tarot_reading.service.limit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 구독하지 않은 회원의 이용 정책 (application.yaml 의 tarot.member 로 바인딩)
 *
 * @param weeklyFreeLimit 7일 주기 무료 이용권 수 (가입일 기준 리셋)
 */
@ConfigurationProperties(prefix = "tarot.member")
public record MemberProperties(
        @DefaultValue("3") int weeklyFreeLimit
) {
}
