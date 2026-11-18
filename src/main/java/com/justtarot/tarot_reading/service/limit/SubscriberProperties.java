package com.justtarot.tarot_reading.service.limit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 구독자 이용 정책 (application.yaml 의 tarot.subscriber 로 바인딩)
 *
 * @param readingLimitPerCycle 결제 주기(결제일 ~ 다음 결제일)당 리딩 수. 원가 상한을 책임진다
 */
@ConfigurationProperties(prefix = "tarot.subscriber")
public record SubscriberProperties(
        @DefaultValue("25") int readingLimitPerCycle
) {
}
