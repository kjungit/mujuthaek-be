package kr.mujuthaek.api.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import kr.mujuthaek.api.eligibility.dto.request.JudgmentApplicantRequest;
import kr.mujuthaek.api.eligibility.model.JudgmentResult;
import kr.mujuthaek.api.eligibility.model.MatchResult;
import kr.mujuthaek.api.eligibility.model.NoticeCandidate;
import kr.mujuthaek.api.eligibility.rule.Eligibility;
import kr.mujuthaek.api.eligibility.rule.frame.FrameJudge;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 창업지원 사업의 지원 지역이 목록과 상세 판정에 똑같이 들어가고, 사업공간에는 들어가지 않는지 봐요 (2026-11-02 운영 「충북 창업인재 지원사업」이 서울 사용자에게
 * 지원 가능). 틀에 지역 조건이 없는 정답 케이스 틀(K-Startup 글로벌 진출, 양산 희망상가)에 지역만 붙여요.
 */
class MatchProgramRegionTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final UUID CHUNGBUK_PROGRAM =
      UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID YANGSAN_SPACE = UUID.fromString("00000000-0000-0000-0000-000000000102");

  /** 마포에 살고 2023-10-17에 개업해 사업장은 없는 사람: 지역만 빼면 K-Startup 글로벌 진출 정답 케이스로 지원 가능이에요. */
  private static final JudgmentApplicantRequest MAPO =
      JSON.readValue(
          """
          {"residenceRegionCode": "1144000000",
           "business": {"hasSite": false, "history": [{"openedOn": "2023-10-17"}]}}""",
          JudgmentApplicantRequest.class);

  private final MatchQueryService service =
      new MatchQueryService(
          field ->
              switch (field) {
                case STARTUP_SUPPORT ->
                    List.of(
                        candidate(
                            CHUNGBUK_PROGRAM,
                            true,
                            List.of("4311100000", "4311200000"),
                            "startup-support/kstartup-gvc"));
                case BUSINESS_SPACE ->
                    List.of(
                        candidate(
                            YANGSAN_SPACE,
                            false,
                            List.of("4833000000"),
                            "business-space/yangsan-hope-shop"));
                case HOUSING -> List.of();
              },
          new JudgmentQueryService(
              (kind, year, program, size) -> OptionalLong.empty(),
              (day, region, area) -> OptionalLong.empty()));

  @Test
  void supportListAndDetailBothJudgeTheProgramRegion() {
    assertThat(service.judgeStartupSupportAll(MAPO))
        .singleElement()
        .extracting(MatchResult.Item::status)
        .isEqualTo(Eligibility.NEEDS_INPUT);

    JudgmentResult one =
        service.judgeStartupSupport(CHUNGBUK_PROGRAM, MAPO).orElseThrow().judgment();
    assertThat(one.result()).isEqualTo(Eligibility.NEEDS_INPUT);
    assertThat(one.common())
        .extracting(JudgmentResult.Criterion::id)
        .contains(FrameJudge.PROGRAM_REGION);
  }

  @Test
  void spaceRegionIsWhereTheSpaceIsNotAnEligibilityRule() {
    JudgmentResult space = service.judgeBusinessSpace(YANGSAN_SPACE, MAPO).orElseThrow().judgment();
    assertThat(space.common())
        .extracting(JudgmentResult.Criterion::id)
        .doesNotContain(FrameJudge.PROGRAM_REGION);
    assertThat(space.groups())
        .flatExtracting(JudgmentResult.Group::criteria)
        .extracting(JudgmentResult.Criterion::id)
        .doesNotContain(FrameJudge.PROGRAM_REGION);
  }

  private static NoticeCandidate candidate(
      UUID id, boolean policy, List<String> regionCodes, String frameFile) {
    return new NoticeCandidate(
        id,
        policy,
        1,
        id.toString(),
        null,
        LocalDate.parse("2026-11-30"),
        regionCodes,
        frame(frameFile),
        null);
  }

  private static JsonNode frame(String file) {
    try (InputStream in =
        MatchProgramRegionTest.class.getResourceAsStream("/eligibility/frames/" + file + ".json")) {
      return JSON.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8)).get("frame");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
