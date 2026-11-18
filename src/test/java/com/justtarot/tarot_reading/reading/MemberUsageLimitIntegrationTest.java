package com.justtarot.tarot_reading.reading;

import com.jayway.jsonpath.JsonPath;
import com.justtarot.tarot_reading.domain.card.Arcana;
import com.justtarot.tarot_reading.domain.card.Suit;
import com.justtarot.tarot_reading.domain.subscription.Plan;
import com.justtarot.tarot_reading.domain.subscription.Subscription;
import com.justtarot.tarot_reading.domain.subscription.SubscriptionRepository;
import com.justtarot.tarot_reading.dto.reading.CardDto;
import com.justtarot.tarot_reading.dto.reading.DrawnCard;
import com.justtarot.tarot_reading.dto.reading.analysis.CardInteraction;
import com.justtarot.tarot_reading.dto.reading.analysis.QuestionAnalysis;
import com.justtarot.tarot_reading.service.draw.CardDrawer;
import com.justtarot.tarot_reading.service.interpretation.CardInterpreter;
import com.justtarot.tarot_reading.service.question.QuestionAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원 무료 한도(가입일 기준 7일 주기)·구독자 결제 주기 한도 통합 테스트.
 * 정책 값은 기본값(회원 주기당 3회, 구독자 주기당 25건)을 그대로 쓴다.
 * 사용량이 계정 단위로 저장되므로 테스트마다 다른 이메일을 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberUsageLimitIntegrationTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String READING_BODY = """
            {"question": "올해 이직운이 궁금해요", "drawCount": 3, "retryCount": 0}
            """;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    SubscriptionRepository subscriptionRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    QuestionAnalyzer questionAnalyzer;
    @MockitoBean
    CardDrawer cardDrawer;
    @MockitoBean
    CardInterpreter cardInterpreter;

    @BeforeEach
    void stubHappyPath() {
        when(questionAnalyzer.analyze(any())).thenReturn(readyAnalysis());
        when(cardDrawer.draw(anyInt())).thenReturn(drawnCards());
        when(cardInterpreter.analyzeInteraction(any())).thenReturn(interaction());
        when(cardInterpreter.narrate(any())).thenReturn("모의 해석 텍스트");
    }

    /* ---------- 회원 ---------- */

    @Test
    void 회원은_주_3회_후_429_MEMBER_WEEKLY_LIMIT() throws Exception {
        String token = signupAndLogin("weekly-limit@example.com");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(reading(token)).andExpect(status().isOk());
        }
        mockMvc.perform(reading(token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(4294));
    }

    @Test
    void 회원_되묻기_응답은_이용권을_차감하지_않는다() throws Exception {
        String token = signupAndLogin("weekly-clarify@example.com");
        when(questionAnalyzer.analyze(any()))
                .thenReturn(ambiguousAnalysis(), readyAnalysis());

        mockMvc.perform(reading(token)).andExpect(status().isUnprocessableEntity());
        for (int i = 0; i < 3; i++) {   // 되묻기가 차감됐다면 3번째에서 429
            mockMvc.perform(reading(token)).andExpect(status().isOk());
        }
    }

    @Test
    void 회원_해석_실패시_이용권이_환불된다() throws Exception {
        String token = signupAndLogin("weekly-refund@example.com");
        when(cardInterpreter.narrate(any()))
                .thenThrow(new RuntimeException("LLM 실패"))
                .thenReturn("재시도 해석");

        mockMvc.perform(reading(token)).andExpect(status().isInternalServerError());
        for (int i = 0; i < 3; i++) {   // 환불이 안 됐다면 3번째에서 429
            mockMvc.perform(reading(token)).andExpect(status().isOk());
        }
    }

    @Test
    void 회원_가입일로부터_7일이_지나면_새_주기로_리셋된다() throws Exception {
        String email = "cycle-reset@example.com";
        String token = signupAndLogin(email);
        // 7일 전 가입, 첫 주기에 3회를 다 쓴 상태 → 오늘이 두 번째 주기의 시작일
        LocalDate signedUp = LocalDate.now(KST).minusDays(7);
        setMemberUsage(email, signedUp, 3, signedUp);

        mockMvc.perform(reading(token)).andExpect(status().isOk());
    }

    @Test
    void 회원_가입_6일째는_아직_같은_주기다() throws Exception {
        String email = "cycle-same@example.com";
        String token = signupAndLogin(email);
        // 월요일 달력 주와 상관없이, 가입일 기준으로는 아직 첫 주기
        LocalDate signedUp = LocalDate.now(KST).minusDays(6);
        setMemberUsage(email, signedUp, 3, signedUp);

        mockMvc.perform(reading(token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(4294));
    }

    /* ---------- 구독자 ---------- */

    @Test
    void 구독자는_주기_한도_소진시_429_SUBSCRIBER_CYCLE_LIMIT() throws Exception {
        String email = "cycle-limit@example.com";
        String token = signupAndLogin(email);
        Long subscriptionId = subscribe(email, LocalDateTime.now(KST).minusDays(3)).getId();
        jdbcTemplate.update("update subscription set reading_used = 24 where id = ?", subscriptionId);

        mockMvc.perform(reading(token)).andExpect(status().isOk());   // 25번째
        mockMvc.perform(reading(token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(4295));
    }

    @Test
    void 구독자는_회원_주간_한도와_무관하게_이용한다() throws Exception {
        String email = "subscriber-free@example.com";
        String token = signupAndLogin(email);
        subscribe(email, LocalDateTime.now(KST).minusDays(1));

        for (int i = 0; i < 4; i++) {   // 회원이었다면 4번째에서 429
            mockMvc.perform(reading(token)).andExpect(status().isOk());
        }
    }

    @Test
    void 만료된_구독만_있으면_회원으로_취급한다() throws Exception {
        String email = "subscriber-expired@example.com";
        String token = signupAndLogin(email);
        subscribe(email, LocalDateTime.now(KST).minusMonths(2));   // expiresAt = 1개월 전

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(reading(token)).andExpect(status().isOk());
        }
        mockMvc.perform(reading(token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(4294));
    }

    @Test
    void 갱신_결제_행은_사용량_0부터_시작한다() throws Exception {
        String email = "subscriber-renew@example.com";
        String token = signupAndLogin(email);
        Long previous = subscribe(email, LocalDateTime.now(KST).minusMonths(1).minusDays(1)).getId();
        jdbcTemplate.update("update subscription set reading_used = 25 where id = ?", previous);
        subscribe(email, LocalDateTime.now(KST).minusDays(1));     // 이번 결제일

        mockMvc.perform(reading(token)).andExpect(status().isOk());
    }

    /* ---------- helpers ---------- */

    private MockHttpServletRequestBuilder reading(String token) {
        return post("/api/reading")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(READING_BODY);
    }

    private void setMemberUsage(String email, LocalDate signedUp, int used, LocalDate cycleStart) {
        jdbcTemplate.update(
                "update account set created_at = ?, free_reading_used = ?, free_reading_cycle_start = ? where email = ?",
                signedUp.atTime(12, 0), used, cycleStart, email);
    }

    private Subscription subscribe(String email, LocalDateTime startedAt) {
        Long userId = jdbcTemplate.queryForObject("select id from account where email = ?", Long.class, email);
        return subscriptionRepository.save(
                new Subscription(userId, Plan.BASIC, startedAt, startedAt.plusMonths(1)));
    }

    private String signupAndLogin(String email) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "password123", "nickname": "limit-tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private QuestionAnalysis readyAnalysis() {
        return new QuestionAnalysis("이직 가능성 확인", "불안", "이직 준비 중", "진로", false, false, "");
    }

    private QuestionAnalysis ambiguousAnalysis() {
        return new QuestionAnalysis("불명확", "불안", "명시되지 않음", "기타", false, true, "이직을 고민 중이신가요?");
    }

    private List<DrawnCard> drawnCards() {
        return List.of(
                new DrawnCard(card(0, "바보"), false),
                new DrawnCard(card(1, "마법사"), true),
                new DrawnCard(card(2, "여사제"), false)
        );
    }

    private CardDto card(int id, String name) {
        return new CardDto(id, name, Arcana.MAJOR, Suit.NONE, id, List.of(), List.of("새 출발"), "시작", "무모함", "자유-구속");
    }

    private CardInteraction interaction() {
        return new CardInteraction("흐름 요약", List.of(), List.of(), "바보", "축 이유", "질문 접점");
    }
}
