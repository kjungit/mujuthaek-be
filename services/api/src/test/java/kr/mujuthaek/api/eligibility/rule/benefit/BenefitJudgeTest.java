package kr.mujuthaek.api.eligibility.rule.benefit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.mujuthaek.api.eligibility.rule.Applicant;
import kr.mujuthaek.api.eligibility.rule.ApplicantBuilder;
import kr.mujuthaek.api.eligibility.rule.Bands;
import kr.mujuthaek.api.eligibility.rule.IncomeBenchmarks;
import kr.mujuthaek.api.eligibility.rule.Item;
import kr.mujuthaek.api.eligibility.rule.Overall;
import kr.mujuthaek.api.eligibility.rule.Status;
import kr.mujuthaek.api.eligibility.rule.benefit.BenefitJudge.Condition;
import org.junit.jupiter.api.Test;

/** 혜택 조건 판정 (benefit-condition-types.draft.md). 오늘은 2026-10-22. */
class BenefitJudgeTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

  /** 2026년 기준중위소득 월액: 1인 250만, 2인 400만 */
  private static final IncomeBenchmarks MEDIAN =
      (standard, year, size) -> size == 1 ? 2_500_000L : size == 2 ? 4_000_000L : null;

  private static Condition c(String type, Map<String, Object> value, int group) {
    return new Condition(type, value, group, true, type + " 원문");
  }

  private static Status one(Condition c, Applicant a) {
    return BenefitJudge.judge(List.of(c), false, a, TODAY, MEDIAN).groups().getFirst().status();
  }

  private static Item item(Condition c, Applicant a) {
    return BenefitJudge.judge(List.of(c), false, a, TODAY, MEDIAN)
        .groups()
        .getFirst()
        .items()
        .getFirst();
  }

  private static ApplicantBuilder a() {
    return new ApplicantBuilder();
  }

  @Test
  void 나이는_출생_연월로_보고_생일_달이면_확인_필요() {
    Condition age = c("AGE", Map.of("min", 19, "max", 39), 1);
    assertThat(one(age, a().birthMonth(1997, 3).build())).isEqualTo(Status.MET);
    assertThat(one(age, a().birthMonth(1980, 3).build())).isEqualTo(Status.NOT_MET);
    assertThat(one(age, a().birthMonth(1986, 10).build())).isEqualTo(Status.UNKNOWN);
    assertThat(one(age, a().build())).isEqualTo(Status.UNKNOWN);
    assertThat(item(age, a().build()).needs()).containsExactly("applicant.birthDate");
  }

  @Test
  void 생년월일이_있으면_생일_달에도_나이가_정해진다() {
    Condition age = c("AGE", Map.of("min", 19, "max", 39), 1);
    // 오늘 2026-10-22: 1986-10-22생은 40세, 1986-10-23생은 아직 39세
    assertThat(one(age, a().birthDate(1986, 10, 22).build())).isEqualTo(Status.NOT_MET);
    assertThat(one(age, a().birthDate(1986, 10, 23).build())).isEqualTo(Status.MET);
  }

  @Test
  void 가구_소득은_받은_퍼센트_구간을_그대로_비교하고_걸치면_확인_필요() {
    Condition income = c("INCOME_MEDIAN_PCT", Map.of("max", 180, "basis", "HOUSEHOLD"), 1);
    assertThat(one(income, a().household(1).income("MEDIAN_INCOME", 90, 100).build()))
        .isEqualTo(Status.MET);
    assertThat(one(income, a().household(1).income("MEDIAN_INCOME", 180, 190).build()))
        .isEqualTo(Status.NOT_MET);
    assertThat(one(income, a().household(1).income("MEDIAN_INCOME", 170, 190).build()))
        .isEqualTo(Status.UNKNOWN);
  }

  @Test
  void 가구_소득_기준인데_본인_소득만_있으면_넘을_때만_불충족_아니면_원문_확인() {
    Condition pct = c("INCOME_MEDIAN_PCT", Map.of("max", 180, "basis", "HOUSEHOLD"), 1);
    Item under = item(pct, a().household(2).income("MEDIAN_INCOME", 90, 100).build());
    assertThat(under.status()).isEqualTo(Status.UNKNOWN);
    assertThat(under.reason()).isEqualTo(Bands.HOUSEHOLD_INCOME_UNKNOWN);
    assertThat(under.needs()).isEmpty();
    Item over = item(pct, a().household(2).income("MEDIAN_INCOME", 200, 210).build());
    assertThat(over.status()).isEqualTo(Status.NOT_MET);
    assertThat(over.reason()).isEqualTo(Bands.OWN_INCOME_OVER);

    // 연 5,000만 원 이하: 2인 90~100% = 월 360만~400만 → 연 4,320만~4,800만 → 가구는 몰라요
    Condition amount = c("INCOME_AMOUNT", Map.of("max", 5000, "basis", "HOUSEHOLD"), 1);
    assertThat(item(amount, a().household(2).income("MEDIAN_INCOME", 90, 100).build()).reason())
        .isEqualTo(Bands.HOUSEHOLD_INCOME_UNKNOWN);
    // 2인 110~120% = 연 5,280만~5,760만 → 본인만으로 넘어요
    assertThat(one(amount, a().household(2).income("MEDIAN_INCOME", 110, 120).build()))
        .isEqualTo(Status.NOT_MET);

    Item none = item(pct, a().household(2).build());
    assertThat(none.status()).isEqualTo(Status.UNKNOWN);
    assertThat(none.reason()).isEqualTo(Bands.NO_INCOME);
  }

  @Test
  void 본인_소득은_1인_기준으로_바꿔_비교한다() {
    // 2인 기준 50~60% = 월 200만~240만 → 1인 기준 80~96% → 100% 이하 충족
    Condition income = c("INCOME_MEDIAN_PCT", Map.of("max", 100, "basis", "PERSONAL"), 1);
    assertThat(one(income, a().household(2).income("MEDIAN_INCOME", 50, 60).build()))
        .isEqualTo(Status.MET);
    // 연 소득 2,500만 원 이하: 1인 90~100% = 월 225만~250만 → 연 2,700만~3,000만 → 불충족
    Condition amount = c("INCOME_AMOUNT", Map.of("max", 2500, "basis", "PERSONAL"), 1);
    assertThat(one(amount, a().household(1).income("MEDIAN_INCOME", 90, 100).build()))
        .isEqualTo(Status.NOT_MET);
  }

  @Test
  void 자산_무주택_혼인_자녀() {
    Condition asset = c("ASSET_AMOUNT", Map.of("max", 34500, "kind", "TOTAL"), 1);
    assertThat(one(asset, a().assets(0L, 200_000_000L).build())).isEqualTo(Status.MET);
    assertThat(one(asset, a().assets(345_000_000L, null).build())).isEqualTo(Status.NOT_MET);

    Condition homeless = c("HOMELESS_YEARS", Map.of("min", 1), 1);
    assertThat(one(homeless, a().homeOwner(false).homelessYears("1_3").build()))
        .isEqualTo(Status.MET);
    assertThat(one(homeless, a().homeOwner(true).build())).isEqualTo(Status.NOT_MET);

    Condition newlywed = c("MARITAL", Map.of("in", List.of("NEWLYWED")), 1);
    assertThat(one(newlywed, a().marital("MARRIED").married(2022, 5).build()))
        .isEqualTo(Status.MET);
    assertThat(one(newlywed, a().marital("MARRIED").married(2015, 5).build()))
        .isEqualTo(Status.NOT_MET);
    assertThat(one(newlywed, a().marital("SINGLE").build())).isEqualTo(Status.NOT_MET);

    Condition kids = c("CHILDREN", Map.of("minCount", 2), 1);
    assertThat(one(kids, a().children(1).build())).isEqualTo(Status.NOT_MET);
    assertThat(one(kids, a().children(2).build())).isEqualTo(Status.MET);
  }

  @Test
  void 거주_조건은_사는_곳이면_충족_아니면_이사_안내() {
    Condition residence = c("RESIDENCE", Map.of("regions", List.of("52730")), 1);
    assertThat(one(residence, a().region("52730").build())).isEqualTo(Status.MET);
    Item moved =
        BenefitJudge.judge(List.of(residence), false, a().region("11440").build(), TODAY, MEDIAN)
            .groups()
            .getFirst()
            .items()
            .getFirst();
    assertThat(moved.status()).isEqualTo(Status.UNKNOWN);
    assertThat(moved.reason()).isEqualTo("이 지역으로 이사하면 받을 수 있어요");
  }

  @Test
  void 거주_줄은_덜_자세히_넣었으면_이사가_아니라_더_넣으라고_한다() {
    Condition suwon = c("RESIDENCE", Map.of("regions", List.of("41110")), 1);
    Item sidoOnly = item(suwon, a().region("41000").build());
    assertThat(sidoOnly.status()).isEqualTo(Status.UNKNOWN);
    assertThat(sidoOnly.reason()).isEqualTo("사는 곳을 시군구까지 넣으면 판정돼요");
    assertThat(sidoOnly.needs()).containsExactly("applicant.regionCode");
    Item cityOnly =
        item(c("RESIDENCE", Map.of("regions", List.of("41117")), 1), a().region("41110").build());
    assertThat(cityOnly.reason()).isEqualTo("사는 곳을 구까지 넣으면 판정돼요");
  }

  @Test
  void 같은_그룹은_또는_다른_그룹은_그리고() {
    Applicant single = a().marital("SINGLE").birthMonth(1997, 3).build();
    var result =
        BenefitJudge.judge(
            List.of(
                c("AGE", Map.of("min", 19, "max", 39), 1),
                c("MARITAL", Map.of("in", List.of("MARRIED")), 2),
                c("HOUSEHOLD_TYPE", Map.of("in", List.of("SINGLE_PERSON")), 2)),
            false,
            a().marital("SINGLE").birthMonth(1997, 3).household(1).build(),
            TODAY,
            MEDIAN);
    assertThat(result.overall()).isEqualTo(Overall.ELIGIBLE);
    assertThat(result.summary().total()).isEqualTo(2);

    var no =
        BenefitJudge.judge(
            List.of(
                c("AGE", Map.of("min", 19, "max", 39), 1),
                c("MARITAL", Map.of("in", List.of("MARRIED")), 2)),
            false,
            single,
            TODAY,
            MEDIAN);
    assertThat(no.overall()).isEqualTo(Overall.INELIGIBLE);
  }

  @Test
  void 값_없는_조건과_입력_없는_조건은_확인_필요_중복_수혜는_안내만() {
    var result =
        BenefitJudge.judge(
            List.of(
                c("AGE", Map.of("min", 19, "max", 39), 1),
                new Condition("OTHER", Map.of(), 2, false, "공공임대 거주자 제외"),
                c("EMPLOYMENT", Map.of("in", List.of("EMPLOYED")), 3),
                new Condition("EXCLUSIVE", Map.of(), 4, false, "주거급여 수혜 중이면 제외")),
            false,
            a().birthMonth(1997, 3).build(),
            TODAY,
            MEDIAN);
    assertThat(result.overall()).isEqualTo(Overall.NEEDS_INPUT);
    assertThat(result.groups()).hasSize(3);
    assertThat(result.notes()).containsExactly("주거급여 수혜 중이면 제외");
  }

  @Test
  void 사람_확인_전_정책은_지원_가능을_넘지_않는다() {
    var result =
        BenefitJudge.judge(
            List.of(c("AGE", Map.of("min", 19, "max", 39), 1)),
            true,
            a().birthMonth(1997, 3).build(),
            TODAY,
            MEDIAN);
    assertThat(result.overall()).isEqualTo(Overall.NEEDS_INPUT);
    assertThat(result.reason()).isNotNull();
    assertThat(BenefitJudge.judge(List.of(), false, a().build(), TODAY, MEDIAN).overall())
        .isEqualTo(Overall.ELIGIBLE);
  }

  // ── 혜택 지역 (2026-11-02 운영: 고령군·경기 혜택이 서울 사용자에게 지원 가능) ──

  private static final BenefitJudge.Area GORYEONG =
      new BenefitJudge.Area("SIGUNGU", List.of("47830"), List.of("경상북도 고령군"));

  private static BenefitJudge.Result judgeIn(BenefitJudge.Area area, Applicant a, Condition... cs) {
    return BenefitJudge.judge(List.of(cs), false, area, a, TODAY, MEDIAN);
  }

  @Test
  void 지역_한정_혜택은_다른_지역에_살면_지역_그룹이_확인_필요() {
    var result =
        judgeIn(
            GORYEONG,
            a().region("11440").birthMonth(1996, 5).build(),
            c("AGE", Map.of("min", 19, "max", 39), 1));
    assertThat(result.overall()).isEqualTo(Overall.NEEDS_INPUT);
    assertThat(result.groups()).hasSize(2);
    Item region = result.groups().getLast().items().getFirst();
    assertThat(region.key()).isEqualTo(BenefitJudge.REGION);
    assertThat(region.label()).isEqualTo("지역 · 경상북도 고령군");
    assertThat(region.status()).isEqualTo(Status.UNKNOWN);
    assertThat(region.reason()).isEqualTo("혜택 지역이 사는 곳과 달라요. 다른 지역 사람도 받을 수 있는지 원문에서 확인해 주세요");
    assertThat(region.needs()).as("이사로 풀리는 게 아니라 원문을 봐야 해요").isEmpty();
  }

  @Test
  void 지역_이름이_없으면_코드_대신_혜택_시행_지역이라고_쓴다() {
    // 온통청년 혜택은 지역 코드만 있고 이름이 없어요
    BenefitJudge.Area seoulWide =
        new BenefitJudge.Area("SIGUNGU", List.of("11110", "11140", "11170"), List.of());
    Item region =
        judgeIn(seoulWide, a().region("47830").build()).groups().getLast().items().getFirst();
    assertThat(region.label()).isEqualTo("지역 · 혜택 시행 지역");
    assertThat(region.status()).isEqualTo(Status.UNKNOWN);
  }

  @Test
  void 일반구에_살면_그_시의_혜택을_받는다() {
    // 시(41110 수원시)와 그 일반구(41117 영통구)는 같은 지역이에요
    BenefitJudge.Area suwon = new BenefitJudge.Area("SIGUNGU", List.of("41110"), List.of("수원시"));
    Applicant yeongtong = a().region("41117").build();
    assertThat(judgeIn(suwon, yeongtong).overall()).isEqualTo(Overall.ELIGIBLE);
    assertThat(item(c("RESIDENCE", Map.of("regions", List.of("41110")), 1), yeongtong).status())
        .as("거주 조건 줄도 같은 비교")
        .isEqualTo(Status.MET);
    BenefitJudge.Area cheongjuDistricts =
        new BenefitJudge.Area("SIGUNGU", List.of("43111", "43112"), List.of("상당구", "서원구"));
    Item cityOnly =
        judgeIn(cheongjuDistricts, a().region("43110").build())
            .groups()
            .getLast()
            .items()
            .getFirst();
    assertThat(cityOnly.status()).isEqualTo(Status.UNKNOWN);
    assertThat(cityOnly.needs())
        .as("다른 지역이 아니라 구까지 넣으면 판정돼요")
        .containsExactly("applicant.regionCode");
    assertThat(cityOnly.reason()).as("시군구는 이미 골랐으니 구까지").isEqualTo("사는 곳을 구까지 넣으면 판정돼요");
    BenefitJudge.Area yeongdong =
        new BenefitJudge.Area("SIGUNGU", List.of("43740"), List.of("충청북도 영동군"));
    Item jeungpyeong =
        judgeIn(yeongdong, a().region("43745").build()).groups().getLast().items().getFirst();
    assertThat(jeungpyeong.status())
        .as("앞 4자리가 같은 군(증평)은 영동군 혜택 지역이 아니에요")
        .isEqualTo(Status.UNKNOWN);
    assertThat(jeungpyeong.needs()).isEmpty();
  }

  @Test
  void 지역_한정_혜택은_그_지역에_살면_충족() {
    var result =
        judgeIn(
            GORYEONG,
            a().region("47830").birthMonth(1996, 5).build(),
            c("AGE", Map.of("min", 19, "max", 39), 1));
    assertThat(result.overall()).isEqualTo(Overall.ELIGIBLE);
    assertThat(result.groups().getLast().items().getFirst().status()).isEqualTo(Status.MET);
    BenefitJudge.Area gyeonggi = new BenefitJudge.Area("SIDO", List.of("41"), List.of("경기도"));
    assertThat(
            judgeIn(gyeonggi, a().region("41110").build(), c("HOMELESS", Map.of(), 1))
                .groups()
                .getLast()
                .items()
                .getFirst()
                .status())
        .as("시도 혜택은 그 시도의 시군구에 살면 충족")
        .isEqualTo(Status.MET);
  }

  @Test
  void 사는_곳이_없거나_시도만_있으면_입력을_받는다() {
    BenefitJudge.Area suwon = new BenefitJudge.Area("SIGUNGU", List.of("41110"), List.of("수원시"));
    Item none =
        judgeIn(suwon, a().build(), c("AGE", Map.of("min", 19, "max", 39), 1))
            .groups()
            .getLast()
            .items()
            .getFirst();
    assertThat(none.status()).isEqualTo(Status.UNKNOWN);
    assertThat(none.needs()).containsExactly("applicant.regionCode");
    Item sidoOnly =
        judgeIn(suwon, a().region("41000").build(), c("AGE", Map.of("min", 19, "max", 39), 1))
            .groups()
            .getLast()
            .items()
            .getFirst();
    assertThat(sidoOnly.status()).isEqualTo(Status.UNKNOWN);
    assertThat(sidoOnly.reason()).isEqualTo("사는 곳을 시군구까지 넣으면 판정돼요");
  }

  @Test
  void 거주_조건_줄이_있거나_전국이거나_지역을_모르면_지역_그룹을_더하지_않는다() {
    Applicant seoul = a().region("11440").birthMonth(1996, 5).build();
    Condition age = c("AGE", Map.of("min", 19, "max", 39), 1);
    Condition residence = c("RESIDENCE", Map.of("regions", List.of("47830")), 2);
    assertThat(judgeIn(GORYEONG, seoul, age, residence).groups())
        .as("거주 조건 줄이 이미 지역을 봐요")
        .extracting(BenefitJudge.Group::group)
        .containsExactly(1, 2);
    assertThat(
            judgeIn(new BenefitJudge.Area("NATIONWIDE", List.of(), List.of()), seoul, age)
                .overall())
        .isEqualTo(Overall.ELIGIBLE);
    assertThat(
            judgeIn(new BenefitJudge.Area("UNKNOWN", List.of(), List.of()), seoul, age).overall())
        .isEqualTo(Overall.ELIGIBLE);
    assertThat(judgeIn(null, seoul, age).overall()).isEqualTo(Overall.ELIGIBLE);
  }

  @Test
  void 조건_없는_지역_한정_혜택도_지역으로_판정한다() {
    assertThat(judgeIn(GORYEONG, a().region("11440").build()).overall())
        .isEqualTo(Overall.NEEDS_INPUT);
    assertThat(judgeIn(GORYEONG, a().region("47830").build()).overall())
        .isEqualTo(Overall.ELIGIBLE);
  }

  // ── 노숙을 말하는 무주택 줄 (2026-11-02 운영 「행려자 구호비 지급」) ──

  @Test
  void 노숙을_말하는_무주택_줄은_집이_없어도_확인_필요() {
    Condition street =
        new Condition(
            "HOMELESS", Map.of("basis", "PERSON"), 1, true, "일정한 주거 없이 노숙생활을 하는 것으로 확인된 자");
    Item item = item(street, a().homelessYears("3_5").build());
    assertThat(item.status()).isEqualTo(Status.UNKNOWN);
    assertThat(item.reason()).isEqualTo("노숙 여부는 원문을 확인해 주세요");
    assertThat(item.needs()).isEmpty();
    assertThat(item(street, a().homeOwner(true).build()).status())
        .as("집이 있어도 지원 불가로 내리지 않아요 (무주택자 또는 노숙인 같은 글이 섞일 수 있어요)")
        .isEqualTo(Status.UNKNOWN);
  }

  @Test
  void 행려자_구호비_모양은_서울_무주택_청년에게_확인_필요() {
    var result =
        judgeIn(
            GORYEONG,
            a().region("11440").birthMonth(1996, 5).homelessYears("3_5").build(),
            new Condition(
                "HOMELESS", Map.of("basis", "PERSON"), 1, true, "일정한 주거 없이 노숙생활을 하는 것으로 확인된 자"));
    assertThat(result.overall()).isEqualTo(Overall.NEEDS_INPUT);
    assertThat(result.groups())
        .extracting(BenefitJudge.Group::status)
        .containsExactly(Status.UNKNOWN, Status.UNKNOWN);
  }

  @Test
  void 주거_상황_단어는_무주택이나_소유를_함께_말하지_않을_때만_확인_필요() {
    assertThat(BenefitJudge.notOwnership("쪽방 거주자")).isEqualTo("주거 상황 조건은 원문을 확인해 주세요");
    assertThat(BenefitJudge.notOwnership("고시원 등 비주택 거주자")).isNotNull();
    assertThat(BenefitJudge.notOwnership("노숙인 등 주거취약계층 매입임대주택 입주 대상자")).isNotNull();
    assertThat(BenefitJudge.notOwnership("주거취약계층 중 무주택 세대구성원")).isNull();
    assertThat(BenefitJudge.notOwnership("무주택 세대주")).isNull();
    assertThat(BenefitJudge.notOwnership("주택을 소유하지 않은 세대주")).isNull();
    assertThat(BenefitJudge.notOwnership("관내에 일정한 주소를 두고 거주하는 무주택자")).isNull();
    assertThat(BenefitJudge.notOwnership("주택 등 일정한 주거지가 없는 자")).isEqualTo("노숙 여부는 원문을 확인해 주세요");
    assertThat(BenefitJudge.notOwnership("주거지가 일정하지 않은 자")).isEqualTo("노숙 여부는 원문을 확인해 주세요");
    assertThat(BenefitJudge.notOwnership("쪽방·고시원 등 비주택에 3개월 이상 거주한 무주택자"))
        .as("비주택 거주가 요건이면 무주택을 함께 말해도 더 좁아요")
        .isEqualTo("주거 상황 조건은 원문을 확인해 주세요");
    assertThat(BenefitJudge.notOwnership("주택을 소유하지 않은 쪽방 거주자")).isNotNull();
    assertThat(item(c("HOMELESS", Map.of(), 1), a().homelessYears("1_3").build()).status())
        .as("보통 무주택 줄은 그대로 충족")
        .isEqualTo(Status.MET);
  }
}
