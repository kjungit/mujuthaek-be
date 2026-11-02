package kr.mujuthaek.api.eligibility.rule.frame;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.mujuthaek.api.eligibility.rule.Verdict;

/**
 * 법정동코드 비교. 코드는 시도 2 + 시군구 3 + 읍면동 3 + 리 2자리이고, 모르는 하위 단계는 0이에요.
 *
 * <p>조건이 "서울(1100000000)"이면 서울의 모든 시군구가 해당돼요. 사용자가 시도만 넣었는데 조건이 시군구면 알 수 없어서 확인 필요예요.
 */
public final class RegionCodes {

  private static final int[] LEVEL_ENDS = {2, 5, 8, 10};

  /**
   * 일반구를 둔 시 (법정동코드 2026-09-29 기준 현존 코드). 빠진 시가 있으면 그 시의 구 주민이 시 조건에서 다른 지역(틀 조건이면 지원 불가)으로 나와서,
   * 빌드에 든 코드표와 다르면 시험(RulesTest)이 실패해요.
   */
  static final Set<String> CITIES_WITH_DISTRICTS =
      Set.of(
          "41110", // 수원시
          "41130", // 성남시
          "41170", // 안양시
          "41190", // 부천시
          "41270", // 안산시
          "41280", // 고양시
          "41460", // 용인시
          "41590", // 화성시
          "43110", // 청주시
          "44130", // 천안시
          "47110", // 포항시
          "48120", // 창원시
          "52110"); // 전주시

  private RegionCodes() {}

  /** 2·5·8·10자리 숫자인지. */
  public static boolean valid(String code) {
    return code != null && code.matches("\\d{2}|\\d{5}|\\d{8}|\\d{10}");
  }

  /** 사용자 지역이 조건 지역 목록 중 하나 안에 있는지 (혜택 판정도 같은 비교를 써요). */
  public static Verdict withinAny(String userCode, List<String> conditionCodes) {
    List<Verdict> verdicts = new ArrayList<>();
    for (String code : conditionCodes) {
      verdicts.add(within(userCode, code));
    }
    return Verdict.any(verdicts);
  }

  /** 사용자 지역이 조건 지역 안에 있는지. */
  static Verdict within(String userCode, String conditionCode) {
    String user = significant(userCode);
    String condition = significant(conditionCode);
    if (user.isEmpty() || condition.isEmpty()) {
      return Verdict.UNKNOWN;
    }
    if (user.startsWith(condition) || inCity(user, condition)) {
      return Verdict.MET;
    }
    // 사용자 코드가 더 짧고 조건이 그 안의 지역이면 (예: 사용자 "경기", 조건 "수원", 또는 사용자 "수원시", 조건 "영통구") 더 자세한 주소가 있어야 알 수
    // 있어요
    return condition.startsWith(user) || inCity(condition, user)
        ? Verdict.UNKNOWN
        : Verdict.NOT_MET;
  }

  /**
   * code가 일반구를 둔 시 city의 구(또는 그 아래)인지. 시와 그 일반구는 앞 4자리가 같고 시만 5번째 자리가 0이에요 (41110 수원시 · 41117 영통구).
   * 앞 4자리만 보면 43740 영동군 · 43745 증평군처럼 다른 군끼리도 같아서, 시는 아래 목록으로만 봐요.
   */
  private static boolean inCity(String code, String city) {
    return CITIES_WITH_DISTRICTS.contains(city)
        && code.length() >= 5
        && code.charAt(4) != '0'
        && code.startsWith(city.substring(0, 4));
  }

  /**
   * 사용자 코드가 조건과 견줘 확인 필요일 때 더 넣을 단계: 시도만 넣었으면 「시군구」, 일반구를 둔 시를 넣었으면 「구」, 그 밖(조건이 읍·면·동 단위)은 화면에서 넣을
   * 수 없어서 null.
   */
  public static String finerLevel(String userCode) {
    String user = significant(userCode);
    if (user.length() == 2) {
      return "시군구";
    }
    return CITIES_WITH_DISTRICTS.contains(user) ? "구" : null;
  }

  /** 0으로 채운 하위 단계를 뺀 앞부분. 예: 1100000000 → 11, 4311100000 → 43111, 52730 → 52730. */
  static String significant(String code) {
    if (!valid(code)) {
      return "";
    }
    int end = 2;
    int start = 0;
    for (int levelEnd : LEVEL_ENDS) {
      if (levelEnd > code.length()) {
        break;
      }
      if (!code.substring(start, levelEnd).chars().allMatch(c -> c == '0')) {
        end = levelEnd;
      }
      start = levelEnd;
    }
    return code.substring(0, end);
  }
}
