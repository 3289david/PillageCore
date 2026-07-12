# PillageCore

팀 기반 약탈(Raid) PvP 서버를 위한 올인원 Paper 플러그인입니다. 팀, 텔레포트, 물물교환, 레이드, PvP 부가기능, 개인 통계, 서버 관리, QoL, 보상, 에메랄드 경제, 아이템 교환 상점, 채팅 확장, GUI 메뉴, 널널한 안티치트까지 전부 포함되어 있습니다. 전투/레이드 중이라는 이유로 텔레포트를 막거나 로그아웃 시 처형하는 기능은 없습니다 — 텔레포트는 언제나 가능합니다.

- **대상 서버**: Paper `26.1.2` (stable dev-bundle) — 실제 호스팅사(FeatherMC 등)에서 돌아가는 최신 안정 버전 기준입니다.
- **Java**: 25 (툴체인)
- **저장소**: SQLite (`plugins/PillageCore/pillage.db`)
- **빌드**: Gradle 9.6.1 (wrapper 포함, `com.gradleup.shadow`로 sqlite-jdbc 셰이딩)

## 빌드 방법

```
./gradlew clean build
```

결과물은 `build/libs/PillageCore-1.0.0.jar` 하나입니다 (Mojang 매핑, sqlite-jdbc 셰이딩 포함, 서버에 바로 넣으면 됨). `release/PillageCore-1.0.0.jar` 에도 동일한 최신 빌드가 복사되어 있습니다.

## 설치

1. `release/PillageCore-1.0.0.jar` (또는 `build/libs/PillageCore-1.0.0.jar`) 를 Paper **26.1.x** 서버의 `plugins/` 폴더에 복사
2. 서버 시작 → `plugins/PillageCore/config.yml` 생성됨
3. 필요하면 `config.yml` 수정 후 서버 재시작
4. `spawn.world`가 실제 월드 이름과 다르면 `/spawn`이 기본 월드로 대체 이동하니 config에서 맞춰주세요.
5. `/spawn`의 Y좌표는 config에 없습니다 — `spawn.x`/`spawn.z` 지점의 가장 높은 블록 위로 자동으로 맞춰져서, 하늘에 뜨거나 땅에 파묻히지 않습니다.

> **버전 주의**: `api-version`은 `26.1`로 선언되어 있습니다. 26.2는 아직 알파/스냅샷 단계라 실제로 배포 가능한 서버가 없어 26.1.2(현재 안정 버전)로 빌드했습니다. 서버가 26.1.x 계열이 아니라면 명령어가 등록되지 않을 수 있으니, 그 경우 `build.gradle.kts`의 `paperDevBundle` 버전과 `plugin.yml`의 `api-version`을 서버 버전에 맞게 바꾼 뒤 다시 빌드하세요.

## 명령어

게임 내에서 `/pillagehelp [페이지]` (별칭 `/phelp`, `/도움말`, 총 4페이지) 를 치면 아래 내용이 그대로 출력됩니다.

### GUI

| 명령어 | 설명 |
|---|---|
| `/menu` (별칭 `/pillage`, `/메뉴`) | 팀/TP/거래/설정/통계 GUI 메인 메뉴 |

텍스트 입력이 필요한 것(팀 이름 짓기 등)만 명령어로 남아 있고, 나머지(멤버 추방, 채팅·FF 토글, 홈 이동/설정, 랭킹, 초대·요청 수락/거절 등)는 전부 GUI 클릭으로 처리됩니다.

### 팀

| 명령어 | 설명 |
|---|---|
| `/team create <이름>` | 팀 생성 |
| `/team invite <player>` | 초대 (팀장) |
| `/team join` | 받은 초대 수락 |
| `/team leave` | 탈퇴 |
| `/team kick <player>` | 추방 (팀장) |
| `/team disband` | 해체 (팀장) |
| `/team chat` | 팀 채팅 모드 토글 |
| `/tc <메시지>` | 팀 채팅 한 줄 전송 |
| `/team ff [on\|off]` | Friendly Fire 토글 (팀장) |
| `/team sethome` / `/team home` | 팀 홈 설정(팀장) / 이동 |
| `/team setmax <숫자>` | 최대 인원 설정 (팀장) |
| `/team list` / `/team info [팀이름]` | 팀원·정보 확인 |
| `/team top [loot]` | 킬 랭킹 / 약탈 점수 랭킹 |

### 텔레포트

| 명령어 | 설명 |
|---|---|
| `/tpa <player>` / `/tpaccept` / `/tpdeny` | 텔레포트 요청 |
| `/spawn` | 스폰 이동 |
| `/back` | 마지막 위치로 |
| `/home [이름]` / `/sethome [이름]` / `/delhome [이름]` | 개인 홈 |
| `/death` | 마지막 사망 위치로 이동 |

모든 텔레포트는 5초 카운트다운 + 이동 시 취소 + 쿨타임이 적용되며, 전투 중이거나 소속 팀이 레이드당하는 중이라도 언제나 사용할 수 있습니다.

### 거래

| 명령어 | 설명 |
|---|---|
| `/trade <player>` | 거래 요청 |
| `/tradeaccept` / `/tradedeny` | 요청 수락/거절 |

수락하면 두 플레이어에게 같은 거래 GUI가 열립니다. 각자 자신의 칸에만 아이템을 넣을 수 있고, 확인 버튼을 누르면 자신의 칸이 잠깁니다(취소해야 다시 수정 가능). 둘 다 확인하면 아이템이 교환되고 `trade_log` 테이블에 기록됩니다. 창을 닫으면 자동으로 취소되고 아이템은 그대로 돌려받습니다.

### 통계 / QoL

| 명령어 | 설명 |
|---|---|
| `/stats [player]` | 킬/데스/K-D/플레이시간/채굴량 |
| `/coords` | 좌표 공유 (팀이 있으면 팀 채팅으로) |
| `/ping [player]` | 핑 확인 |
| `/tps` | 서버 TPS 확인 |
| `/clock` | 액션바 시계 토글 |

> "자동 재접속"은 서버 플러그인이 아니라 클라이언트/런처의 기능이라 서버 쪽에서 구현할 수 없습니다 (미구현, 의도적으로 제외).

### 경제 / 보상

| 명령어 | 설명 |
|---|---|
| `/balance [player]` (별칭 `/bal`) | 잔액 확인 |
| `/pay <player> <금액>` | 송금 |
| `/deposit [수량]` | 인벤토리의 에메랄드를 잔액으로 입금 |
| `/withdraw <수량>` | 잔액을 에메랄드 아이템으로 출금 |
| `/dailyreward` (별칭 `/일일보상`) | 24시간마다 1회, 스테이크(COOKED_BEEF) 수령 |
| `/eventbox give <player> [수량]` (관리자) | 이벤트 상자 지급, 우클릭하면 랜덤 보상 |
| `/shop` | 아이템 교환 상점 GUI 열기 |
| `/shop add <내는아이템> <내는수량> <받는아이템> <받는수량>` (관리자) | 상점 항목 등록 |
| `/shop remove <id>` / `/shop list` (관리자) | 상점 항목 삭제 / 조회 |

플레이타임이 설정된 시간(기본 1시간) 단위로 쌓일 때마다 자동으로 에메랄드 보상이 지급됩니다. 상점은 관리자가 등록한 "아이템 A n개 -> 아이템 B m개" 교환 목록을 GUI로 보여주며, 플레이어가 클릭하면 즉시 자동으로 교환됩니다(재료가 부족하면 실패).

### 채팅

- 전체 채팅에는 소속 팀 태그가 자동으로 붙습니다 (`[팀이름] 닉네임: 메시지`).
- `@닉네임`을 메시지에 포함하면 해당 플레이어에게 알림 + 효과음이 갑니다.
- 채팅 쿨타임(기본 2초)과 선택적 욕설 필터가 적용됩니다 (`config.yml`의 `chat` 섹션).
- `/msg <player> <메시지>` (별칭 `/tell`, `/w`, `/귓말`), `/r <메시지>` - 마지막 귓속말 상대에게 답장

### 관리자 (`pillage.admin` 권한 필요)

| 명령어 | 설명 |
|---|---|
| `/report <player> <사유>` | 신고 (누구나 사용 가능, 접수 시 관리자에게 알림) |
| `/staff` | 투명화(관리자 모드) 토글 |
| `/inspect <player>` | 인벤토리 읽기 전용 검사 |
| `/logs <trade\|kill\|ban\|tp> [개수]` | 최근 로그 조회 |
| `/pillageban <player> [사유]` | 차단 + 로그 기록 |

## 자동 시스템 (명령어 없음)

- **레이드 타이머**: 신규 팀 보호 없이 언제든 팀이 공격받으면 팀 전체에 경고 메시지가 뜹니다(텔레포트 제한이나 로그아웃 처형은 없음). 레이드 종료 시 공격 측 킬 수가 `raid.win-kill-threshold`(기본 3) 이상이면 공격 팀 "약탈 성공"(+약탈 점수), 아니면 수비 팀 "레이드 방어"로 기록됩니다.
- **킬 로그 / 킬 스트릭 / 사망 상자**: 모든 PvP 킬이 킬 피드로 브로드캐스트되고 DB에 기록됩니다. 5/10/20 연킬은 전체 공지됩니다. 사망 시 드롭 아이템은 바닥에 흩어지는 대신 사망 지점에 상자로 영구히 보관되며(자동으로 사라지지 않음), 팀 소속 여부와 상관없이 누구나 열어서 약탈할 수 있습니다. 상자는 부술 수 있으며(부수면 일반 상자처럼 내용물이 바닥에 드롭됩니다), 아이템이 상자 한 칸(27슬롯)을 넘으면 바로 옆에 상자를 추가로 생성해 전부 담습니다 — 어떤 경우에도 아이템이 소실되지 않습니다.
- **안티치트 (매우 널널)**: KillAura / Reach / Speed / Fly / AutoClick / Scaffold / FastBreak 7종. 판정 임계값을 넉넉하게 잡았고, 여러 번(기본 10회) 반복되어야 `pillage.admin` 권한자에게 채팅 경고만 보냅니다. 자동 제재(킥)는 `config.yml`의 `anticheat.punish.enabled`를 켜야만 동작하며 기본은 꺼져 있습니다.

## 권한

| 권한 | 기본값 | 설명 |
|---|---|---|
| `pillage.team.*` | true | 팀 명령어 |
| `pillage.tp.*` | true | TP 명령어 |
| `pillage.trade.*` | true | 거래 명령어 |
| `pillage.admin` | op | 안티치트 경고 수신, 관리자 명령어(`/staff`, `/inspect`, `/logs`, `/pillageban`, `/eventbox`, `/shop add\|remove\|list`), 인원수 제한 우회 등 |

## 주요 설정 (config.yml)

`database`, `team`, `tp`, `raid`, `spawn`, `anticheat`, `reward`, `chat` 섹션으로 나뉘어 있으며 각 값에 한글 주석이 달려 있습니다. 특히:

- `raid.win-kill-threshold`: 레이드 중 이 킬 수 이상이면 공격 측 승리로 기록
- `anticheat.punish.enabled`: 기본 `false` (경고만). 자동 킥을 원하면 `true`로 변경
- `anticheat.*.enabled`: 검사별로 개별 on/off 가능
- `chat.profanity-filter-enabled` / `chat.banned-words`: 욕설 필터 on/off와 단어 목록

## 데이터베이스 (SQLite)

`teams`, `team_members`, `homes`, `last_locations`, `trade_log`, `kill_log`, `report_log`, `ban_log`, `tp_log`, `player_stats`, `death_locations`, `daily_rewards`, `playtime_rewards`, `economy`, `shop_offers` 테이블로 구성됩니다. 전부 `plugins/PillageCore/pillage.db` 한 파일에 저장됩니다.

## 구현 현황

**전부 완료** — 최초 기획서(팀/TP/거래/약탈/전투/안티치트/랜드(의도적으로 없음)/PvP/GUI/통계/서버관리/QoL/보상/경제/채팅) 16개 대분류를 모두 구현했습니다.

의도적으로 제외한 것: **랜드 기능**(레이드 서버 특성상 팀 홈만 존재하도록 기획에 명시됨), **자동 재접속**(서버 플러그인이 구현할 수 없는 클라이언트 기능).
