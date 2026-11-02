package kr.mujuthaek.api.eligibility.rule.frame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import kr.mujuthaek.api.common.region.RegionLoader;
import kr.mujuthaek.api.eligibility.rule.BaseValues;
import kr.mujuthaek.api.eligibility.rule.Eligibility;
import kr.mujuthaek.api.eligibility.rule.Field;
import kr.mujuthaek.api.eligibility.rule.Verdict;
import kr.mujuthaek.api.eligibility.rule.frame.Applicant.BusinessRecord;
import kr.mujuthaek.api.eligibility.rule.frame.Applicant.Household;
import kr.mujuthaek.api.eligibility.rule.frame.Applicant.MaritalStatus;
import kr.mujuthaek.api.eligibility.rule.frame.Criterion.Meta;
import org.junit.jupiter.api.Test;

/** 정답 케이스가 덜 건드리는 규칙의 경계: 조건 종류마다 충족·불충족·확인 필요가 갈리는 지점을 봐요. */
class RulesTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

  private static Meta meta(String id) {
    return new Meta(id, null, null, false, null, null);
  }

  private static Applicant applicant(
      LocalDate birth,
      String residence,
      MaritalStatus marital,
      LocalDate marriedOn,
      List<LocalDate> children,
      Boolean expecting,
      Household household,
      Applicant.Subscription subscription,
      Applicant.Business business) {
    return new Applicant(
        birth,
        null,
        residence,
        null,
        marital,
        marriedOn,
        children,
        expecting,
        household,
        subscription,
        business,
        null,
        null,
        Map.of());
  }

  private static Applicant withBusiness(Applicant.Business business) {
    return applicant(null, null, null, null, null, null, null, null, business);
  }

  private static Applicant.Business business(
      Boolean hasSite, String site, List<BusinessRecord> history, String industry) {
    return new Applicant.Business(null, hasSite, site, history, industry, null);
  }

  private static BusinessRecord record(String opened, String closed) {
    return new BusinessRecord(
        LocalDate.parse(opened), closed == null ? null : LocalDate.parse(closed));
  }

  private static Household household(Integer size, Boolean homeless, Integer homelessYears) {
    return new Household(
        size, null, homeless, null, homelessYears, null, null, null, null, null, null, null, null);
  }

  // ── 사람 ──

  private static Applicant korean(Boolean korean) {
    return new Applicant(
        null, korean, null, null, null, null, null, null, null, null, null, null, null, Map.of());
  }

  @Test
  void nationalityOnlyAsksWhetherTheApplicantIsKorean() {
    Criterion.Nationality koreansOnly = new Criterion.Nationality(meta("n"), List.of("KR"));
    assertThat(PersonRules.nationality(koreansOnly, korean(true)).verdict()).isEqualTo(Verdict.MET);
    assertThat(PersonRules.nationality(koreansOnly, korean(false)).verdict())
        .isEqualTo(Verdict.NOT_MET);
    Evaluation unanswered = PersonRules.nationality(koreansOnly, korean(null));
    assertThat(unanswered.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(unanswered.missing()).containsExactly(Input.KOREAN);

    // 다른 나라 국적도 받는 틀: 국민이 아니면 어느 나라인지 몰라서 확인 필요, KR이 없으면 국민은 불충족
    Criterion.Nationality alsoUs = new Criterion.Nationality(meta("n"), List.of("KR", "US"));
    assertThat(PersonRules.nationality(alsoUs, korean(false)).verdict()).isEqualTo(Verdict.UNKNOWN);
    Criterion.Nationality usOnly = new Criterion.Nationality(meta("n"), List.of("US"));
    assertThat(PersonRules.nationality(usOnly, korean(true)).verdict()).isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void residenceOrBusinessSiteNeedsOnlyOneToMatch() {
    Criterion.Region either =
        new Criterion.Region(
            meta("r"), Criterion.RegionBasis.RESIDENCE_OR_BUSINESS_SITE, List.of("2700000000"));
    Applicant livesInDaegu =
        applicant(
            null,
            "2729000000",
            null,
            null,
            null,
            null,
            null,
            null,
            business(false, null, null, null));
    assertThat(PersonRules.region(either, livesInDaegu).verdict()).isEqualTo(Verdict.MET);

    Applicant noSiteElsewhere =
        applicant(
            null,
            "1100000000",
            null,
            null,
            null,
            null,
            null,
            null,
            business(false, null, null, null));
    assertThat(PersonRules.region(either, noSiteElsewhere).verdict()).isEqualTo(Verdict.NOT_MET);

    Evaluation unknown = PersonRules.region(either, Applicant.empty());
    assertThat(unknown.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(unknown.missing()).containsExactly(Input.RESIDENCE_REGION, Input.BUSINESS_SITE);
  }

  @Test
  void residenceMonthsAreComparedOnlyAfterThePlaceMatches() {
    Criterion.Residence c = new Criterion.Residence(meta("r"), List.of("1100000000"), 12);
    Applicant seoulNoMonths =
        applicant(null, "1168000000", null, null, null, null, null, null, null);
    assertThat(PersonRules.residence(c, seoulNoMonths).missing())
        .containsExactly(Input.RESIDENCE_MONTHS);
    Applicant busan = applicant(null, "2600000000", null, null, null, null, null, null, null);
    assertThat(PersonRules.residence(c, busan).verdict()).isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void notMarriedMeansNotCurrentlyMarried() {
    Criterion.Marriage c =
        new Criterion.Marriage(meta("m"), Criterion.MarriageRequirement.NOT_MARRIED, null, false);
    assertThat(
            PersonRules.marriage(
                    c,
                    applicant(
                        null, null, MaritalStatus.DIVORCED, null, null, null, null, null, null),
                    DAY)
                .verdict())
        .isEqualTo(Verdict.MET);
    assertThat(
            PersonRules.marriage(
                    c,
                    applicant(
                        null, null, MaritalStatus.MARRIED, null, null, null, null, null, null),
                    DAY)
                .verdict())
        .isEqualTo(Verdict.NOT_MET);
  }

  private static Applicant bornIn(String month) {
    return new Applicant(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Map.of(),
        YearMonth.parse(month));
  }

  @Test
  void ageFromBirthMonthIsDecidedUnlessTheBirthdayFallsInTheReferenceMonth() {
    Criterion.Age youth = new Criterion.Age(meta("youth-age"), 19, 39);
    // 기준일 2026-10-01: 1987-09생은 그 달 어느 날이든 만 39세 → 충족
    assertThat(PersonRules.age(youth, bornIn("1987-09"), DAY).verdict()).isEqualTo(Verdict.MET);
    // 1986-09생은 그 달 어느 날이든 만 40세 → 불충족
    assertThat(PersonRules.age(youth, bornIn("1986-09"), DAY).verdict()).isEqualTo(Verdict.NOT_MET);
    // 1986-10생은 10월 1일생이면 만 40세, 2일 이후면 만 39세 → 확인 필요. 화면은 출생 연월만 보내서 생년월일을 묻지 않아요
    var edge = PersonRules.age(youth, bornIn("1986-10"), DAY);
    assertThat(edge.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(edge.reason()).isEqualTo("생일이 지났는지에 따라 달라져요. 직접 확인해 주세요");
    assertThat(edge.missing()).isEmpty();
  }

  @Test
  void marriageWithinYearsNeedsTheRegistrationDate() {
    Criterion.Marriage c =
        new Criterion.Marriage(meta("m"), Criterion.MarriageRequirement.MARRIED, 7, false);
    Applicant noDate =
        applicant(null, null, MaritalStatus.MARRIED, null, null, null, null, null, null);
    assertThat(PersonRules.marriage(c, noDate, DAY).missing()).containsExactly(Input.MARRIED_ON);
    // 2019-10-01 + 7년 = 2026-10-01 (기준일과 같은 날) → 충족
    Applicant edge =
        applicant(
            null,
            null,
            MaritalStatus.MARRIED,
            LocalDate.of(2019, 10, 1),
            null,
            null,
            null,
            null,
            null);
    assertThat(PersonRules.marriage(c, edge, DAY).verdict()).isEqualTo(Verdict.MET);
    Applicant engaged =
        applicant(null, null, MaritalStatus.ENGAGED, null, null, null, null, null, null);
    assertThat(PersonRules.marriage(c, engaged, DAY).verdict())
        .as("예비신혼 불포함")
        .isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void anUnknownPregnancyMattersOnlyWhenItChangesTheResult() {
    Criterion.Children twoKids = new Criterion.Children(meta("c"), 2, null, true);
    Applicant oneChild =
        applicant(
            null, null, null, null, List.of(LocalDate.of(2020, 1, 1)), null, null, null, null);
    Evaluation e = PersonRules.children(twoKids, oneChild, DAY);
    assertThat(e.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(e.missing()).containsExactly(Input.EXPECTING_CHILD);

    Applicant threeChildren =
        applicant(
            null,
            null,
            null,
            null,
            List.of(LocalDate.of(2015, 1, 1), LocalDate.of(2018, 1, 1), LocalDate.of(2020, 1, 1)),
            null,
            null,
            null,
            null);
    assertThat(PersonRules.children(twoKids, threeChildren, DAY).verdict()).isEqualTo(Verdict.MET);

    Applicant adultChildren =
        applicant(
            null, null, null, null, List.of(LocalDate.of(2000, 1, 1)), false, null, null, null);
    assertThat(PersonRules.children(twoKids, adultChildren, DAY).verdict())
        .as("성인 자녀는 미성년 자녀로 세지 않아요")
        .isEqualTo(Verdict.NOT_MET);
  }

  // ── 주거 ──

  @Test
  void homelessYearsAreNeededOnlyWhenHomeless() {
    Criterion.Homeless c = new Criterion.Homeless(meta("h"), Criterion.HomelessScope.HOUSEHOLD, 2);
    assertThat(
            HousingRules.homeless(
                    c,
                    applicant(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        household(null, true, null),
                        null,
                        null))
                .missing())
        .containsExactly(Input.HOMELESS_YEARS);
    assertThat(
            HousingRules.homeless(
                    c,
                    applicant(
                        null, null, null, null, null, null, household(null, true, 3), null, null))
                .verdict())
        .isEqualTo(Verdict.MET);
    assertThat(
            HousingRules.homeless(
                    c,
                    applicant(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        household(null, false, null),
                        null,
                        null))
                .verdict())
        .isEqualTo(Verdict.NOT_MET);
  }

  /** 기준값 표가 비어 있는 주거 규칙 (청약통장처럼 표가 필요 없는 조건을 볼 때). */
  private static final HousingRules NO_TABLES =
      new HousingRules(
          (k, y, p, s) -> OptionalLong.empty(), (d, region, area) -> OptionalLong.empty());

  private static final LocalDate SAVINGS_DAY = LocalDate.of(2026, 10, 16);

  @Test
  void savingsWithoutMinimumsOnlyNeedAnAccount() {
    Criterion.Savings c = new Criterion.Savings(meta("s"), null, null, null, null);
    assertThat(NO_TABLES.savings(c, Applicant.empty(), SAVINGS_DAY).missing())
        .containsExactly(Input.SUBSCRIPTION_MONTHS);
    Applicant none =
        applicant(
            null, null, null, null, null, null, null, new Applicant.Subscription(0, 0, 0L), null);
    assertThat(NO_TABLES.savings(c, none, SAVINGS_DAY).verdict()).isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void aKnownFailingSavingsRuleDecidesEvenIfOthersAreUnknown() {
    Criterion.Savings c = new Criterion.Savings(meta("s"), 24, 24, 3_000_000L, null);
    Applicant shortAccount =
        applicant(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new Applicant.Subscription(6, null, null),
            null);
    assertThat(NO_TABLES.savings(c, shortAccount, SAVINGS_DAY).verdict())
        .isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void applicantAndParentsIncomeUsesTheirOwnHouseholdSize() {
    // 기준값 4인 600만원 × 100% = 600만원. 가산은 MAX: 맞벌이 미입력이라 가산 없이 판정해 봐요
    BaseValues table =
        (kind, year, program, size) ->
            size == 4 ? OptionalLong.of(6_000_000) : OptionalLong.empty();
    HousingRules rules = new HousingRules(table, (d, region, area) -> OptionalLong.empty());
    Criterion.Income c =
        new Criterion.Income(
            meta("i"),
            Criterion.IncomeBasis.APPLICANT_AND_PARENTS,
            Criterion.IncomeStandard.MEDIAN_INCOME,
            2026,
            BigDecimal.valueOf(100),
            List.of(
                new Criterion.Bonus(
                    new Criterion.Factor(Criterion.FactorKind.DUAL_INCOME, null, null, null, null),
                    BigDecimal.TEN)),
            Criterion.BonusCombine.MAX,
            List.of());
    Household parentsMissing =
        new Household(
            null, null, null, null, null, null, 2_000_000L, null, 4, null, null, null, null);
    Evaluation missing =
        rules.income(
            c, applicant(null, null, null, null, null, null, parentsMissing, null, null), DAY);
    assertThat(missing.missing()).containsExactly(Input.PARENTS_INCOME);

    Household both =
        new Household(
            null, null, null, null, null, null, 2_000_000L, 3_500_000L, 4, null, null, null, null);
    Evaluation within =
        rules.income(c, applicant(null, null, null, null, null, null, both, null, null), DAY);
    assertThat(within.verdict()).as("550만원 ≤ 600만원").isEqualTo(Verdict.MET);
  }

  // ── 사업 ──

  @Test
  void closureAnyNeedsAClosureInThePeriod() {
    Criterion.Closure any =
        new Criterion.Closure(meta("c"), Criterion.ClosureMode.ANY, LocalDate.of(2020, 1, 1), null);
    assertThat(
            BusinessRules.closure(
                    any,
                    withBusiness(
                        business(null, null, List.of(record("2015-01-01", "2021-05-05")), null)),
                    DAY)
                .verdict())
        .isEqualTo(Verdict.MET);
    assertThat(
            BusinessRules.closure(any, withBusiness(business(null, null, List.of(), null)), DAY)
                .verdict())
        .isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void closedWithoutAPeriodMeansAnyPastClosure() {
    Criterion.BusinessRegistration closed =
        new Criterion.BusinessRegistration(
            meta("b"), Criterion.RegistrationRequirement.CLOSED, null);
    assertThat(
            BusinessRules.registration(
                    closed,
                    withBusiness(
                        business(null, null, List.of(record("2001-01-01", "2003-01-01")), null)),
                    DAY)
                .verdict())
        .isEqualTo(Verdict.MET);
    assertThat(
            BusinessRules.registration(
                    closed,
                    withBusiness(
                        business(
                            null,
                            null,
                            List.of(record("2001-01-01", "2003-01-01"), record("2024-01-01", null)),
                            null)),
                    DAY)
                .verdict())
        .as("지금 영업 중이면 불충족")
        .isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void revenueWithOnlyAMinimum() {
    Criterion.Revenue c = new Criterion.Revenue(meta("r"), 2025, 100_000_000L, null);
    Applicant.Business enough =
        new Applicant.Business(null, null, null, null, null, Map.of(2025, 150_000_000L));
    assertThat(BusinessRules.revenue(c, withBusiness(enough)).verdict()).isEqualTo(Verdict.MET);
    Applicant.Business otherYear =
        new Applicant.Business(null, null, null, null, null, Map.of(2024, 150_000_000L));
    assertThat(BusinessRules.revenue(c, withBusiness(otherYear)).missing())
        .containsExactly(Input.REVENUE);
  }

  @Test
  void onlyIndustriesHaveThreeOutcomes() {
    Criterion.Industry only =
        new Criterion.Industry(
            meta("i"), Criterion.IndustryMode.ONLY, 11, List.of("58"), List.of("62"));
    assertThat(
            BusinessRules.industry(only, withBusiness(business(null, null, null, "58222")))
                .verdict())
        .isEqualTo(Verdict.MET);
    assertThat(
            BusinessRules.industry(only, withBusiness(business(null, null, null, "62010")))
                .verdict())
        .isEqualTo(Verdict.UNKNOWN);
    assertThat(
            BusinessRules.industry(only, withBusiness(business(null, null, null, "56111")))
                .verdict())
        .isEqualTo(Verdict.NOT_MET);
    Criterion.Industry oldRevision =
        new Criterion.Industry(
            meta("o"), Criterion.IndustryMode.EXCLUDE, 10, List.of("56211"), List.of());
    assertThat(
            BusinessRules.industry(oldRevision, withBusiness(business(null, null, null, "58222")))
                .verdict())
        .as("10차 공고는 11차 코드와 바로 비교하지 않아요")
        .isEqualTo(Verdict.UNKNOWN);
  }

  @Test
  void applicantTypeComparesIndividualOrCorporation() {
    Criterion.ApplicantKind corporation =
        new Criterion.ApplicantKind(meta("a"), Applicant.ApplicantType.CORPORATION);
    assertThat(BusinessRules.applicantKind(corporation, Applicant.empty()).missing())
        .containsExactly(Input.APPLICANT_TYPE);
  }

  @Test
  void inReviewHoldsBackMetButKeepsNotMet() {
    Criterion.Age reviewing =
        new Criterion.Age(new Meta("youth-age", null, null, true, "나이 표기가 두 가지", null), 19, 39);
    FrameJudge judge =
        new FrameJudge(
            (k, y, p, s) -> OptionalLong.empty(), (d, region, area) -> OptionalLong.empty());
    var met = judge.evaluate(reviewing, bornIn("1987-09"), DAY);
    assertThat(met.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(met.reason()).isNull();
    assertThat(met.missing()).isEmpty();
    // 만 40세: 검수 중이어도 불충족 그대로
    assertThat(judge.evaluate(reviewing, bornIn("1986-09"), DAY).verdict())
        .isEqualTo(Verdict.NOT_MET);
  }

  @Test
  void businessAgeYearsSetsTheOpenedRangeFromTheReferenceDay() {
    // 기준일 2026-10-28. 추출의 opened_range와 같아요: 이내는 N년 전 같은 날부터, 미만은 그다음 날부터, 이상은 그날까지, 초과는 그 전날까지
    LocalDate day = LocalDate.of(2026, 10, 28);
    FrameJudge judge =
        new FrameJudge(
            (k, y, p, s) -> OptionalLong.empty(), (d, region, area) -> OptionalLong.empty());
    var within = new Criterion.BusinessAgeYears(meta("within"), 7, Criterion.YearsRule.WITHIN);
    var under = new Criterion.BusinessAgeYears(meta("under"), 3, Criterion.YearsRule.UNDER);
    var atLeast = new Criterion.BusinessAgeYears(meta("at-least"), 3, Criterion.YearsRule.AT_LEAST);
    var over = new Criterion.BusinessAgeYears(meta("over"), 3, Criterion.YearsRule.OVER);

    assertThat(judge.evaluate(within, openedOn("2019-10-28"), day).verdict())
        .isEqualTo(Verdict.MET);
    assertThat(judge.evaluate(within, openedOn("2019-10-27"), day).verdict())
        .isEqualTo(Verdict.NOT_MET);
    assertThat(judge.evaluate(under, openedOn("2023-10-28"), day).verdict())
        .isEqualTo(Verdict.NOT_MET);
    assertThat(judge.evaluate(under, openedOn("2023-10-29"), day).verdict()).isEqualTo(Verdict.MET);
    assertThat(judge.evaluate(atLeast, openedOn("2023-10-28"), day).verdict())
        .isEqualTo(Verdict.MET);
    assertThat(judge.evaluate(atLeast, openedOn("2023-10-29"), day).verdict())
        .isEqualTo(Verdict.NOT_MET);
    assertThat(judge.evaluate(over, openedOn("2023-10-27"), day).verdict()).isEqualTo(Verdict.MET);
    assertThat(judge.evaluate(over, openedOn("2023-10-28"), day).verdict())
        .isEqualTo(Verdict.NOT_MET);

    var met = judge.evaluate(within, openedOn("2024-03-01"), day);
    assertThat(met.criterion().kind()).isEqualTo("BUSINESS_AGE");
    assertThat(met.summary()).isEqualTo("창업 7년 이내 · 개업일 2019-10-28 ~ 2026-10-28 (둘 다 포함)");
    // 사업 정보를 안 넣었으면 사업 이력을 넣어 달라고 해요
    var empty = judge.evaluate(within, withBusiness(business(null, null, null, null)), day);
    assertThat(empty.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(empty.missing()).containsExactly(Input.BUSINESS_HISTORY);
  }

  private static Applicant openedOn(String day) {
    return withBusiness(business(null, null, List.of(record(day, null)), null));
  }

  // ── 공고 단위 ──

  @Test
  void missingInputsComeOnlyFromUndecidedGroups() {
    Criterion.Age youth = new Criterion.Age(meta("youth-age"), 19, 39);
    Criterion.Status merit = new Criterion.Status(meta("merit"), List.of("NATIONAL_MERIT"));
    Frame frame =
        new Frame(
            Field.HOUSING,
            "p",
            1,
            DAY,
            List.of(),
            List.of(
                new Frame.Group("youth", "청년", null, List.of(youth)),
                new Frame.Group("merit", "보훈", null, List.of(merit))),
            List.of(),
            List.of());
    FrameJudge judge =
        new FrameJudge(
            (k, y, p, s) -> OptionalLong.empty(), (d, region, area) -> OptionalLong.empty());
    // 50세라 청년은 불충족 → 보훈 대상군만 남아요
    Applicant old =
        new Applicant(
            LocalDate.of(1976, 1, 1),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Map.of());
    FrameJudgment j = judge.judge(frame, old);
    assertThat(j.result()).isEqualTo(Eligibility.NEEDS_INPUT);
    assertThat(j.missingInputs()).containsExactly(Input.STATUSES);

    Applicant merited =
        new Applicant(
            LocalDate.of(1976, 1, 1),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Set.of("NATIONAL_MERIT"),
            null,
            Map.of());
    assertThat(judge.judge(frame, merited).missingInputs()).as("지원 가능이면 비어 있어요").isEmpty();
  }

  // ── 지원 가능으로 두지 않는 틀 (2026-11-02 운영 창업지원 사업이 서울 사용자에게 지원 가능) ──

  private static final FrameJudge PLAIN =
      new FrameJudge(
          (k, y, p, s) -> OptionalLong.empty(), (d, region, area) -> OptionalLong.empty());

  private static Frame support(List<Criterion> common, Frame.Group... groups) {
    return new Frame(
        Field.STARTUP_SUPPORT, "p", 1, null, common, List.of(groups), List.of(), List.of());
  }

  /** 마포구에 사는 1996년생 예비창업자 (사업장 없음). */
  private static Applicant mapoPreFounder() {
    return applicant(
        LocalDate.of(1996, 5, 20),
        "1144000000",
        null,
        null,
        null,
        null,
        null,
        null,
        business(false, null, List.of(), null));
  }

  @Test
  void groupWithNoCriteriaAndNoCommonIsNotEligible() {
    // 2026-11-02 운영 「미디어 청년창업 지원」: 「전체」 대상군에 조건이 하나도 없어 누구에게나 지원 가능이었어요
    Frame frame = support(List.of(), new Frame.Group("all", "전체", null, List.of()));
    FrameJudgment j = PLAIN.judge(frame, mapoPreFounder(), DAY);
    assertThat(j.result()).isEqualTo(Eligibility.NEEDS_INPUT);
    Evaluation unread = j.groups().getFirst().criteria().getFirst();
    assertThat(unread.criterion().id()).isEqualTo("all" + FrameJudge.UNREAD_GROUP);
    assertThat(unread.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(unread.summary()).isEqualTo("이 대상에는 정리된 자격 조건이 없어요");
    assertThat(unread.reason()).isEqualTo("원문에서 이 대상의 자격 조건을 확인해 주세요");
    assertThat(j.missingInputs()).as("입력으로 풀리지 않아요").isEmpty();
    assertThat(j.questions()).as("사용자에게 묻는 질문으로 내보내지 않아요").isEmpty();
  }

  @Test
  void emptyGroupKeepsCommonCriteriaAndOtherGroupsDecide() {
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    assertThat(
            PLAIN
                .judge(
                    support(List.of(youth), new Frame.Group("all", "전체", null, List.of())),
                    mapoPreFounder(),
                    DAY)
                .result())
        .as("공통 조건이 있으면 그 조건으로 판정해요")
        .isEqualTo(Eligibility.ELIGIBLE);
    FrameJudgment j =
        PLAIN.judge(
            support(
                List.of(),
                new Frame.Group("all", "전체", null, List.of()),
                new Frame.Group("youth", "청년", null, List.of(youth))),
            mapoPreFounder(),
            DAY);
    assertThat(j.result()).as("조건을 읽은 다른 대상군이 충족이면 지원 가능").isEqualTo(Eligibility.ELIGIBLE);
    assertThat(j.groups().get(1).criteria()).hasSize(1);
  }

  @Test
  void programOutsideTheRegionIsUnknownNotIneligible() {
    // 2026-11-02 운영 「충북 창업인재 지원사업」: 틀에 지역 조건이 없어 서울 사용자에게 지원 가능이었어요
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    Frame frame = support(List.of(youth), new Frame.Group("all", "전체", null, List.of()));
    FrameJudgment j = PLAIN.judge(frame, mapoPreFounder(), DAY, List.of("4311100000"));
    assertThat(j.result()).isEqualTo(Eligibility.NEEDS_INPUT);
    Evaluation region = j.common().getLast();
    assertThat(region.criterion().id()).isEqualTo(FrameJudge.PROGRAM_REGION);
    assertThat(region.criterion().kind()).isEqualTo("REGION");
    assertThat(region.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(region.reason()).isEqualTo("지원 지역이 사는 곳과 달라요. 다른 지역 사람도 신청할 수 있는지 원문에서 확인해 주세요");
    assertThat(region.missing()).isEmpty();
  }

  @Test
  void programRegionGoesToGroupsWithoutTheirOwnRegion() {
    // 추출이 지역을 일부 대상군에만 읽은 틀: 지역 조건이 없는 대상군이 지원 지역 없이 지원 가능이 되지 않아요
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    Criterion.Region busanSite =
        new Criterion.Region(
            meta("region"), Criterion.RegionBasis.BUSINESS_SITE, List.of("2600000000"));
    Frame frame =
        support(
            List.of(youth),
            new Frame.Group("pre", "예비창업자", null, List.of(new Criterion.Age(meta("age2"), 19, 39))),
            new Frame.Group("startup", "창업기업", null, List.of(busanSite)));
    FrameJudgment j = PLAIN.judge(frame, mapoPreFounder(), DAY, List.of("2611000000"));
    assertThat(j.result()).isEqualTo(Eligibility.NEEDS_INPUT);
    assertThat(j.common()).as("공통에는 더하지 않아요").hasSize(1);
    assertThat(j.groups().get(0).criteria().getLast().criterion().id())
        .isEqualTo(FrameJudge.PROGRAM_REGION);
    assertThat(j.groups().get(1).criteria())
        .extracting(e -> e.criterion().id())
        .doesNotContain(FrameJudge.PROGRAM_REGION);
  }

  @Test
  void citiesWithDistrictsMatchTheBundledCodeTable() throws IOException {
    // 코드표가 바뀌어 일반구를 둔 시가 늘거나 줄면 목록도 같이 고쳐요 (빠지면 그 시의 구 주민이 시 조건에서 다른 지역이 돼요)
    Set<String> cities =
        RegionLoader.bundled()
            .text()
            .lines()
            .map(line -> line.split(","))
            .filter(row -> row.length >= 3 && "존재".equals(row[2].trim()))
            .filter(row -> row[0].matches("\\d{5}00000"))
            .filter(
                row -> {
                  String[] names = row[1].trim().split("\\s+");
                  return names.length == 3 && names[1].endsWith("시") && names[2].endsWith("구");
                })
            .map(row -> row[0].substring(0, 4) + "0")
            .collect(Collectors.toSet());
    assertThat(cities).isEqualTo(RegionCodes.CITIES_WITH_DISTRICTS);
  }

  @Test
  void finerLevelAsksOnlyWhatTheScreenCanTake() {
    assertThat(RegionCodes.finerLevel("4100000000")).isEqualTo("시군구");
    assertThat(RegionCodes.finerLevel("43")).as("사업 정보의 시·도 「전체」").isEqualTo("시군구");
    assertThat(RegionCodes.finerLevel("43110")).isEqualTo("구");
    assertThat(RegionCodes.finerLevel("11440")).as("화면은 시군구까지만 받아요").isNull();
    Applicant mapo =
        applicant(
            LocalDate.of(1996, 5, 20), "1144000000", null, null, null, null, null, null, null);
    Evaluation dong =
        PersonRules.region(
            new Criterion.Region(
                meta("dong"), Criterion.RegionBasis.RESIDENCE, List.of("1144012000")),
            mapo);
    assertThat(dong.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(dong.reason()).isEqualTo("읍·면·동까지는 원문에서 확인해 주세요");
    assertThat(dong.missing()).as("넣을 수 없는 단계는 묻지 않아요").isEmpty();
    Applicant chungbukSite =
        applicant(
            LocalDate.of(1996, 5, 20),
            "1144000000",
            null,
            null,
            null,
            null,
            null,
            null,
            business(true, "43", List.of(), null));
    assertThat(
            PersonRules.region(
                    new Criterion.Region(
                        meta("site"), Criterion.RegionBasis.BUSINESS_SITE, List.of("4311100000")),
                    chungbukSite)
                .reason())
        .isEqualTo("사업장 소재지를 더 자세히(시군구까지) 넣어야 알 수 있어요");
  }

  @Test
  void listInputsSkipGroupsThatInputsCannotSettle() {
    // 정리된 조건이 없는 대상군만 남으면 주소지를 넣어도 확인 필요라 목록에서 묻지 않아요 (2026-11-02 운영 「충북 창업인재 지원사업」 모양)
    Frame empty = support(List.of(), new Frame.Group("all", "전체", null, List.of()));
    Applicant noAddress =
        applicant(
            LocalDate.of(1996, 5, 20),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            business(false, null, List.of(), null));
    FrameJudgment j = PLAIN.judge(empty, noAddress, DAY, List.of("4311100000"));
    assertThat(j.result()).isEqualTo(Eligibility.NEEDS_INPUT);
    assertThat(j.common().getLast().missing())
        .as("상세에는 남아요")
        .containsExactly(Input.RESIDENCE_REGION);
    assertThat(j.missingInputs()).isEmpty();
  }

  @Test
  void enteredButCoarseSiteStaysInTheList() {
    // 사업장을 「경북 전체」로 넣었으면 시군구까지 넣으면 판정되니 목록에서도 물어요 (문경 사업)
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 45);
    Frame frame = support(List.of(youth), new Frame.Group("all", "전체", null, List.of()));
    Applicant coarseSite =
        applicant(
            LocalDate.of(1996, 5, 20),
            "1144000000",
            null,
            null,
            null,
            null,
            null,
            null,
            business(true, "47", List.of(), null));
    FrameJudgment j = PLAIN.judge(frame, coarseSite, DAY, List.of("4728000000"));
    assertThat(j.common().getLast().reason()).isEqualTo("사업장 소재지를 더 자세히(시군구까지) 넣어야 알 수 있어요");
    assertThat(j.missingInputs()).containsExactly(Input.BUSINESS_SITE);
  }

  @Test
  void outsideProgramDoesNotAskForTheSiteInTheList() {
    // 사업 중인데 사업장을 비운 서울 사용자: 지역 밖 사업마다 「사업장 소재지를 넣으면 판정돼요」가 붙어 배너 숫자가 부풀지 않게
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    Frame frame = support(List.of(youth), new Frame.Group("all", "전체", null, List.of()));
    Applicant noSite =
        applicant(
            LocalDate.of(1996, 5, 20),
            "1144000000",
            null,
            null,
            null,
            null,
            null,
            null,
            business(null, null, List.of(), null));
    FrameJudgment j = PLAIN.judge(frame, noSite, DAY, List.of("4311100000"));
    assertThat(j.result()).isEqualTo(Eligibility.NEEDS_INPUT);
    assertThat(j.common().getLast().missing()).as("상세 항목에는 남아요").contains(Input.BUSINESS_SITE);
    assertThat(j.missingInputs()).isEmpty();
  }

  @Test
  void cityAndItsDistrictsAreTheSameRegion() {
    // 일반구를 둔 시(41110 수원시 · 41117 영통구)는 앞 4자리가 같고 시만 5번째 자리가 0이에요
    assertThat(RegionCodes.within("41117", "41110")).isEqualTo(Verdict.MET);
    assertThat(RegionCodes.within("4111710100", "4111000000")).isEqualTo(Verdict.MET);
    // 앞 4자리가 같아도 군끼리는 다른 지역이에요 (43740 영동군 · 43745 증평군)
    assertThat(RegionCodes.within("43745", "43740")).isEqualTo(Verdict.NOT_MET);
    assertThat(RegionCodes.within("4374500000", "4374000000")).isEqualTo(Verdict.NOT_MET);
    assertThat(RegionCodes.within("43740", "43745")).isEqualTo(Verdict.NOT_MET);
    assertThat(RegionCodes.within("43110", "4311100000"))
        .as("시만 넣으면 구까지 넣어야 알아요")
        .isEqualTo(Verdict.UNKNOWN);
    assertThat(RegionCodes.within("41110", "41117")).isEqualTo(Verdict.UNKNOWN);
    assertThat(RegionCodes.within("41130", "41110")).as("다른 시").isEqualTo(Verdict.NOT_MET);
    assertThat(RegionCodes.within("11140", "11110")).as("특별시의 자치구끼리").isEqualTo(Verdict.NOT_MET);
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    Frame frame = support(List.of(youth), new Frame.Group("all", "전체", null, List.of()));
    Applicant cheongjuCity =
        applicant(
            LocalDate.of(1996, 5, 20),
            "4311000000",
            null,
            null,
            null,
            null,
            null,
            null,
            business(false, null, List.of(), null));
    Evaluation region =
        PLAIN
            .judge(frame, cheongjuCity, DAY, List.of("4311100000", "4311200000"))
            .common()
            .getLast();
    assertThat(region.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(region.missing())
        .as("다른 지역이 아니라 구까지 넣으면 판정돼요")
        .containsExactly(Input.RESIDENCE_REGION);
    assertThat(region.reason()).as("사업장이 없다고 했으니 주소지 안내만").isEqualTo("주소지를 더 자세히(구까지) 넣어야 알 수 있어요");
    assertThat(
            PersonRules.region(
                    new Criterion.Region(
                        meta("home"), Criterion.RegionBasis.RESIDENCE, List.of("4311100000")),
                    cheongjuCity)
                .reason())
        .as("시군구는 이미 골랐으니 구까지")
        .isEqualTo("주소지를 더 자세히(구까지) 넣어야 알 수 있어요");
  }

  @Test
  void programRegionIsMetWhenTheApplicantLivesThere() {
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    Frame frame = support(List.of(youth), new Frame.Group("all", "전체", null, List.of()));
    Applicant cheongju =
        applicant(
            LocalDate.of(1996, 5, 20),
            "4311100000",
            null,
            null,
            null,
            null,
            null,
            null,
            business(false, null, List.of(), null));
    FrameJudgment j = PLAIN.judge(frame, cheongju, DAY, List.of("4300000000"));
    assertThat(j.result()).isEqualTo(Eligibility.ELIGIBLE);
    assertThat(j.common().getLast().verdict()).isEqualTo(Verdict.MET);
  }

  @Test
  void programRegionAsksForAddressOrSiteWhenUnknown() {
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    Frame frame = support(List.of(youth), new Frame.Group("all", "전체", null, List.of()));
    Applicant noAddress =
        applicant(LocalDate.of(1996, 5, 20), null, null, null, null, null, null, null, null);
    FrameJudgment j = PLAIN.judge(frame, noAddress, DAY, List.of("4300000000"));
    Evaluation region = j.common().getLast();
    assertThat(region.verdict()).isEqualTo(Verdict.UNKNOWN);
    assertThat(region.missing()).containsExactly(Input.RESIDENCE_REGION, Input.BUSINESS_SITE);
    assertThat(j.missingInputs())
        .as("주소지는 넣으면 판정되니 목록에도 남기고, 사업장은 상세에만 물어요")
        .containsExactly(Input.RESIDENCE_REGION);
  }

  @Test
  void nationwideUnknownOrAlreadyRegionalProgramsAddNothing() {
    Criterion.Age youth = new Criterion.Age(meta("age"), 19, 39);
    Frame plain = support(List.of(youth), new Frame.Group("all", "전체", null, List.of()));
    assertThat(PLAIN.judge(plain, mapoPreFounder(), DAY, List.of()).common()).hasSize(1);
    assertThat(PLAIN.judge(plain, mapoPreFounder(), DAY, null).common()).hasSize(1);
    Criterion.Region chungbuk =
        new Criterion.Region(
            meta("region"), Criterion.RegionBasis.RESIDENCE, List.of("4300000000"));
    Frame regional =
        support(List.of(youth, chungbuk), new Frame.Group("all", "전체", null, List.of()));
    FrameJudgment j = PLAIN.judge(regional, mapoPreFounder(), DAY, List.of("4300000000"));
    assertThat(j.common()).as("틀의 지역 조건이 이미 지역을 봐요").hasSize(2);
    assertThat(j.result()).as("틀의 지역 조건은 그대로 지원 불가").isEqualTo(Eligibility.INELIGIBLE);
  }
}
