# PR 보관본

팀 저장소 [NBE11-13-final-Team02](https://github.com/prgrms-be-devcourse/NBE11-13-final-Team02)에서 제가 올려 머지된 PR 101개의 본문과, 팀원 PR에 남긴 리뷰 11건을 옮겨 둔 문서예요.
코드 변경은 이 저장소의 커밋으로 들어와 있고(각 문서에 커밋 링크), PR에 적은 설계 이유·원인 추적·전후 수치·테스트 설명은 여기서 볼 수 있어요.

- 기간: 2026-10-14 ~ 2026-11-02
- 분류: Feat 56 · Fix 20 · Chore 11 · Release 8 · Perf 4 · Main 1 · Refactor 1
- 각 문서 맨 위에 원본 PR 링크가 있어요. 팀원이 단 리뷰·댓글은 옮기지 않았어요.

## 팀원 PR 리뷰 (11건)

| PR | 제목 | 결과 | 코드 줄 코멘트 |
| --- | --- | --- | --- |
| [#15](reviews/15.md) | Fix·Feat(collector): LH 연동을 위한 공통 코드 수정 (예비자 공고문, 출처별 클라이언트·시군구 코드) | DISMISSED | — |
| [#18](reviews/18.md) | Feat(collector-policy): 정책·창업지원 API 6곳 수집기 추가 | CHANGES_REQUESTED, APPROVED | 6 |
| [#20](reviews/20.md) | Feat(contracts): 표준 공고·추출 결과·자격 조건 계약과 검사기 추가 | CHANGES_REQUESTED | 7 |
| [#21](reviews/21.md) | Feat(extraction): 수집 이벤트·첨부 PDF·엑셀을 추출 원문으로 바꾸는 입력 해석 추가 | CHANGES_REQUESTED | 5 |
| [#22](reviews/22.md) | Feat(extraction): AI 출력을 근거·금액·지역 코드가 붙은 추출 결과로 결합 | CHANGES_REQUESTED | 7 |
| [#33](reviews/33.md) | Feat(contracts): 공고 추출 결과·이벤트·자격 조건·판정 틀 계약과 의미 검사기 | APPROVED | — |
| [#34](reviews/34.md) | Feat(extraction): 수집 이벤트·첨부 PDF·엑셀을 추출 원문으로 바꾸는 입력 해석 | APPROVED | — |
| [#35](reviews/35.md) | Feat(extraction): AI 출력 결합과 원문 대조 검증기 | CHANGES_REQUESTED, APPROVED | — |
| [#36](reviews/36.md) | Feat(extraction): AI 모델 호출, 주거 공고 추출 규칙, 공급표, 재확인, 비용 상한 | APPROVED | — |
| [#37](reviews/37.md) | Feat(extraction): 판정 틀 변환과 extraction.completed 내용 만들기 | APPROVED | — |
| [#38](reviews/38.md) | Feat(extraction): 공고 처리 흐름, 저장·아웃박스 발행, worker, 검수 API | APPROVED | — |

## 내 PR (101개, 최신순)

| PR | 머지 | 제목 |
| --- | --- | --- |
| [#206](prs/206.md) | 2026-11-02 | Release: dev → main (v0.2.1 이후 12개 — 지도 500 고침 #205, 커넥션 쥐는 시간 #204, 동시성 #201·#202, 판정 캐시 #200 …) |
| [#205](prs/205.md) | 2026-11-02 | Fix(notice): 지도 API가 공고 종류 없는 공고가 있으면 500 — 임대·분양 거르기에서 종류가 비면 임대로 |
| [#204](prs/204.md) | 2026-11-02 | Perf: 요청이 DB 커넥션을 쥐는 시간 줄이기 — 공고 조회 클래스 전체 트랜잭션 걷기·지도 묶음 이름 한 번에, 혜택 목록 나이 하위 쿼리 빼기 |
| [#202](prs/202.md) | 2026-11-01 | Fix(user): 관심 저장이 동시에 몰리면 200개 제한을 넘던 것 — 회원 단위 잠금 |
| [#201](prs/201.md) | 2026-11-01 | Fix(notice): 같은 추출 완료 이벤트가 동시에 오면 일부가 처리 기록 기본 키에서 부딪혀 예외(→ 재시도·실패 큐) |
| [#200](prs/200.md) | 2026-11-01 | Perf(eligibility): 판정 캐시 — 공고는 읽어 둔 판정 모델(53.5KB → 4.4KB, 2만 개, 미리 채우기), 혜택은 조건 캐시, 적중률 지표 |
| [#199](prs/199.md) | 2026-11-01 | Perf: 로컬 더미 부하에서 찾은 느린 쿼리 4개 — 판정 틀 읽기 줄이기, 혜택 나이 미리 거르기, 시도 필터 인덱스, 관심 목록 N+1 |
| [#194](prs/194.md) | 2026-10-31 | Fix(infra·collector): 사라진 area 적재 요청 — RabbitMQ 호스트 이름 고정, area-upload --resend |
| [#193](prs/193.md) | 2026-10-30 | Release: 운영 알림 소음·채팅 연결 타임아웃, 릴리즈 태그·노트, 성능 테스트 도구 |
| [#192](prs/192.md) | 2026-10-30 | Chore(ci): 손으로 실행한 운영 릴리즈도 태그·릴리즈 노트 남기기 |
| [#191](prs/191.md) | 2026-10-30 | Feat(perf): k6 성능 테스트와 결과 저장·전후 비교 도구 (perf/) |
| [#190](prs/190.md) | 2026-10-30 | Feat(infra): 운영 서버에 성능 테스트 때만 켜는 node-exporter·cAdvisor (profile perf) |
| [#189](prs/189.md) | 2026-10-30 | Feat(api): 성능 테스트용 Prometheus 지표 열기 (api·notification, 서버 안에서만) |
| [#187](prs/187.md) | 2026-10-30 | Fix(api): 개발 서버 슬랙 알림 소음 — 수집기 없는 503, 채팅 연결 끊김 |
| [#186](prs/186.md) | 2026-10-30 | Release: 판정 고침(소득 구간·검수 중 조건·가구 소득·혜택 1년 지원액·사업공간 업력)·공고 유형·검수 대기열 |
| [#176](prs/176.md) | 2026-10-29 | Fix(eligibility): 소득 구간이 대상군으로 들어가 소득만 맞으면 공고 전체가 지원 가능이 되던 판정 |
| [#175](prs/175.md) | 2026-10-29 | Release: 릴리즈 테스트 실패 때 배포 막기, 혜택 검색 테스트 간섭 고치기 (v0.2.0 다시) |
| [#174](prs/174.md) | 2026-10-29 | Fix(ci): 릴리즈 테스트 실패 때 배포하지 않기, 혜택 검색 테스트 간섭 고치기 |
| [#173](prs/173.md) | 2026-10-29 | Release: 내 정보 API, area 적재 대기열·운영 장애 정리, 운영·배포 슬랙 알림 |
| [#172](prs/172.md) | 2026-10-29 | Fix(api): area 적재 수신기 멈춤·멈춘 load_runs 정리, 검수·수집기 연결, DLQ 이유·다시 처리, 운영·배포 슬랙 알림 |
| [#171](prs/171.md) | 2026-10-29 | Main |
| [#170](prs/170.md) | 2026-10-29 | Feat(user): 내 정보 API 맞추기 — 관심 목록 응답, 내 제보, 일반 제보, 사업공간·혜택 제보 |
| [#169](prs/169.md) | 2026-10-29 | Release: 개발 서버 데이터 (shovel 결과 이벤트, copy-prod-to-dev.sh) |
| [#168](prs/168.md) | 2026-10-29 | Feat(infra): 개발 서버가 운영과 같은 데이터를 보게 (shovel 결과 이벤트, copy-prod-to-dev.sh) |
| [#167](prs/167.md) | 2026-10-28 | Release: v0.1.0 운영 배포 변수 수정 (DEPLOY_SERVICE) |
| [#166](prs/166.md) | 2026-10-28 | Fix(ci): 운영 배포 서비스 이름 변수를 DEPLOY_SERVICE로 |
| [#165](prs/165.md) | 2026-10-28 | Release: v0.1.0 (첫 운영 배포) |
| [#164](prs/164.md) | 2026-10-28 | Fix(user): 소셜 로그인 콜백 주소를 OAUTH_REDIRECT_BASE_URL로 고정 |
| [#162](prs/162.md) | 2026-10-28 | Chore(infra): 배포 문제 해결 안내 |
| [#161](prs/161.md) | 2026-10-28 | Chore(ci): 배포 ssh 명령 시간 제한 30분 (command_timeout) |
| [#159](prs/159.md) | 2026-10-28 | Feat(infra): EC2 배포 준비 (이미지 4개, 운영·개발 compose, deploy.sh, 배포 워크플로) |
| [#150](prs/150.md) | 2026-10-27 | Feat(collector): 처음 수집 개수 제한 (LH 주거 10·다른 주거 10·사업공간 10·지원 정책 500) |
| [#149](prs/149.md) | 2026-10-27 | Feat(user): 환경 변수(ADMIN_BOOTSTRAP_USER_ID)로 처음 최상위 관리자 지정 |
| [#142](prs/142.md) | 2026-10-27 | Fix(collector): 출처마다 따로 잠그고 게시판 출처는 동시에 수집 |
| [#141](prs/141.md) | 2026-10-27 | Feat(policy): 정책 출처 안내(policyInfo) — 지원 분야·금액·요약·기관·문의처·접수 방법 |
| [#140](prs/140.md) | 2026-10-27 | Feat(eligibility): 사업공간·사업 지원 목록에 질문 답 반영, 이름이 조금 다른 같은 모집 하나로 |
| [#139](prs/139.md) | 2026-10-27 | Fix(extraction): 제외 사유끼리 「또는」으로 묶인 anyOf는 모두 해당하지 않아야 하는 조건으로 |
| [#138](prs/138.md) | 2026-10-27 | Feat(eligibility): 출생 연월로 나이 판정, 사업공간·사업 지원 판정에 질문 답 (answers) |
| [#134](prs/134.md) | 2026-10-27 | Feat(eligibility): 사업 지원(창업지원) 맞춤 목록·판정 (POST /supports/search, /supports/{id}/evaluate) |
| [#133](prs/133.md) | 2026-10-27 | Fix(collector): 온라인몰·판매전 입점 모집은 사업공간 모집이 아니라 창업지원으로 |
| [#128](prs/128.md) | 2026-10-27 | Feat(eligibility): 사업공간 맞춤 목록·판정 (POST /spaces/search, /spaces/{id}/evaluate) |
| [#122](prs/122.md) | 2026-10-27 | Feat(notice): 목록 생활권 점수를 가장 높은 단지 기준으로 (areaScore) |
| [#118](prs/118.md) | 2026-10-27 | Chore: .claude/ 로컬 작업 폴더 무시, 빈 docs/adr 자리 표시 파일 삭제 |
| [#114](prs/114.md) | 2026-10-26 | Feat(notice): 주택목록에 단지 이름·가격 대상별 보증금·월세 |
| [#113](prs/113.md) | 2026-10-26 | Fix(extraction): 주택목록 공고 추출 정리 (열 제목·층·위치·대상 묶음·서류 중복) |
| [#105](prs/105.md) | 2026-10-26 | Feat(eligibility): 혜택 판정·맞춤 목록·홈 혜택 요약 |
| [#104](prs/104.md) | 2026-10-26 | Feat(benefit): 정책 추출 결과 계약·혜택 저장·상세 API |
| [#103](prs/103.md) | 2026-10-26 | Feat(notice): 공고 상세에 선정 방식·안내, 공고 유형에 공공임대 |
| [#102](prs/102.md) | 2026-10-26 | Feat(eligibility): 홈 요약 POST /home/summary (판정 개수·새 공고·마감 임박·생활권) |
| [#101](prs/101.md) | 2026-10-26 | Feat(eligibility): 판정 입력을 구간 값으로 (출생 연월·무주택·청약·소득·자산) |
| [#100](prs/100.md) | 2026-10-26 | Perf(eligibility): 맞춤 목록 판정 틀 한 번에 읽기·캐시, 추출이 덜 된 공고는 확인 필요 |
| [#99](prs/99.md) | 2026-10-25 | Feat(eligibility): 맞춤 목록에 생활권 총점 순서 (9단계) |
| [#98](prs/98.md) | 2026-10-25 | Feat(notice): 공고 목록·상세·지도에 area 위치 값 (8단계) |
| [#97](prs/97.md) | 2026-10-25 | Feat(area): 위치 생활권 정보·요청 때 총점·점수 기준표 API (8단계 area 쪽) |
| [#96](prs/96.md) | 2026-10-25 | Feat(area): 공공 상가 상권 점수 — GET /areas/commercial/{siteId} (7단계) |
| [#95](prs/95.md) | 2026-10-25 | Feat(area): 요청 때 생활권 총점과 통근 거리 (5단계) |
| [#94](prs/94.md) | 2026-10-25 | Feat(area): 공고 위치를 받아 좌표·세권·생활권 점수를 계산하고 공고 상세로 점수 보내기 (연결) |
| [#93](prs/93.md) | 2026-10-25 | Feat(area): 공고 위치 좌표 찾기와 저장 — 경계 검사·같은 주소 재사용·샘플 명령 (3-5) |
| [#92](prs/92.md) | 2026-10-25 | Feat(area): 사전에 없는 새 건물은 좌표제공 API로 (3-3) |
| [#91](prs/91.md) | 2026-10-25 | Feat(area): 공고 주소 하나를 좌표로 — 사전 → 검색 API → 대표점 (3-2 마무리) |
| [#90](prs/90.md) | 2026-10-25 | Feat(area): 도로명주소 사전에서 공고 주소 찾기 (3-1) |
| [#89](prs/89.md) | 2026-10-25 | Feat(user): 로그인 뒤 프론트 /auth/callback으로, 처음 가입이면 ?new=1 |
| [#88](prs/88.md) | 2026-10-25 | Feat(user): CORS 허용 주소를 여러 개 받기 (app.cors-origins) |
| [#83](prs/83.md) | 2026-10-25 | Feat(area): 시군구·법정읍면동 경계 적재와 좌표 경계 검사 |
| [#82](prs/82.md) | 2026-10-25 | Feat(area): 도로명주소 검색 API 클라이언트 |
| [#81](prs/81.md) | 2026-10-24 | Feat(area): 생활권 항목 점수와 요청 때 총점 (6단계) |
| [#78](prs/78.md) | 2026-10-24 | Feat(area): 공고 위치 세권 지표 계산 (4단계) |
| [#75](prs/75.md) | 2026-10-24 | Feat(common): 이름으로 지역 코드 찾기와 2026 개편 대응 |
| [#74](prs/74.md) | 2026-10-24 | Feat(area): 공고 주소 정리와 종류 판별 |
| [#60](prs/60.md) | 2026-10-24 | Feat(area): 도로명주소 출입구로 주소 → 좌표 사전 |
| [#57](prs/57.md) | 2026-10-24 | Chore(area): 저장·조회 방식 비교 도구와 --skip-upload 테스트 |
| [#56](prs/56.md) | 2026-10-24 | Fix(area): 다시 넣을 때 내용이 같은 줄은 기준일도 건드리지 않기 |
| [#53](prs/53.md) | 2026-10-24 | Feat(eligibility): 공고 판정·준비할 돈·맞춤 목록 API |
| [#52](prs/52.md) | 2026-10-24 | Feat(eligibility): 판정 기준값과 GET /criteria · 청약 가점 |
| [#51](prs/51.md) | 2026-10-24 | Feat(eligibility): 판정 엔진 (판정 입력·조건 판정·대상별 결과) |
| [#50](prs/50.md) | 2026-10-24 | Feat(notice): 공고 공개 조회 API (목록·상세·주택목록·정정 이력) |
| [#49](prs/49.md) | 2026-10-23 | Feat(notice): extraction.completed를 받아 공고 버전 저장 |
| [#48](prs/48.md) | 2026-10-23 | Feat(common): 시군구 코드표와 GET /regions |
| [#47](prs/47.md) | 2026-10-23 | Feat(common): API 공통 틀 (오류 형식·커서 목록·민감 값 가림·이벤트 수신) |
| [#46](prs/46.md) | 2026-10-23 | Feat(collector-area): 원천 파일을 S3에 올리고 area.source.ready 발행 (area-upload) |
| [#45](prs/45.md) | 2026-10-23 | Feat(area): area.source.ready 계약과 수신해서 적재 시작 |
| [#44](prs/44.md) | 2026-10-23 | Feat(area): S3 원천 파일을 읽어 적재하는 서비스와 AreaBaseDataLoaded |
| [#43](prs/43.md) | 2026-10-23 | Feat(area): 생활권 시설·상가 기초 데이터 테이블과 적재기 |
| [#42](prs/42.md) | 2026-10-23 | Fix(platform): validate_event가 다른 스키마 파일을 가리키는 $ref를 풀도록 |
| [#41](prs/41.md) | 2026-10-22 | Chore(infra): extraction DB 마이그레이션 명령 추가 (make migrate-extraction) |
| [#40](prs/40.md) | 2026-10-21 | Fix(collector): 정정공고를 원래 공고에 정확히 연결 (같은 회차 등록 순서·뒤 회차 오연결) |
| [#32](prs/32.md) | 2026-10-21 | Fix(collector-lh): 포털에서 복사한 인코딩된 인증키도 받기 |
| [#31](prs/31.md) | 2026-10-21 | Feat(collector-soco): 서울시 청년안심주택 모집공고 수집기 추가 |
| [#30](prs/30.md) | 2026-10-21 | Fix(collector): 선정·입주·일정 안내와 모집 종료·확정 알림을 후속 안내로 분류 |
| [#19](prs/19.md) | 2026-10-19 | Refactor(collector): 호별 표 읽기 제거 — 표 해석은 extraction이 원본에서 |
| [#17](prs/17.md) | 2026-10-19 | Feat(contracts,collector): 이벤트 files에 S3 위치·역할·형식 추가, 지방 공사 8곳 실제 수집 확인 |
| [#14](prs/14.md) | 2026-10-19 | Chore: 키 유출 방지 — gitignore 보강·gitleaks pre-commit 훅 |
| [#13](prs/13.md) | 2026-10-19 | Fix(collector): 공고문 고르기·호별 표 읽기 보강, .xls 주택목록 지원 |
| [#10](prs/10.md) | 2026-10-16 | Feat(collector-dcco): 대전 실제 화면으로 완료 기준 확인 |
| [#9](prs/9.md) | 2026-10-16 | Feat(collector): 출처 규격·파이프라인·기관 9곳 수집기·실행 명령·Docker 추가 |
| [#8](prs/8.md) | 2026-10-16 | Feat(collector): 수집기 공통 코드·DB 마이그레이션·공통 테스트 추가 |
| [#7](prs/7.md) | 2026-10-16 | Chore(infra): 로컬 MinIO 이미지 교체·Flyway 마이그레이션·Makefile 추가 |
| [#6](prs/6.md) | 2026-10-16 | Feat(libs): 파이썬 공통 라이브러리와 raw.notice.collected 계약 추가 |
| [#3](prs/3.md) | 2026-10-14 | Chore(ci): main 머지는 dev에서만 받도록 체크 추가 |
| [#2](prs/2.md) | 2026-10-14 | Chore(ci): CODEOWNERS 담당자 아이디 지정 |
| [#1](prs/1.md) | 2026-10-14 | Chore(ci): JaCoCo 커버리지 측정과 80% 기준 추가 |
