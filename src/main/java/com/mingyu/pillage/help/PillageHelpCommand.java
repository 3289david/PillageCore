package com.mingyu.pillage.help;

import com.mingyu.pillage.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PillageHelpCommand implements CommandExecutor {

    private static final List<List<String>> PAGES = List.of(
            List.of(
                    "&e/menu &f- 팀/TP/거래/설정/통계를 GUI로 열기 (별칭: /pillage, /메뉴)",
                    "",
                    "&6[팀]",
                    "&e/team create <이름> &f- 팀 생성",
                    "&e/team invite <player> &f- 초대 (팀장)",
                    "&e/team join &f- 초대 수락 후 가입",
                    "&e/team leave &f- 탈퇴 / &e/team disband &f- 해체 (팀장)",
                    "&e/team kick <player> &f- 추방 (팀장)",
                    "&e/team chat &f- 팀 채팅 모드 토글, &e/tc <메시지> &f- 팀 채팅 한 줄 전송",
                    "&e/team ff [on|off] &f- Friendly Fire 토글 (팀장)",
                    "&e/team sethome &f/&e/team home &f- 팀 홈 설정/이동 (설정은 팀장)",
                    "&e/team setmax <숫자> &f- 최대 인원 설정 (팀장)",
                    "&e/team list &f/&e/team info [팀이름] &f- 팀원/정보 확인",
                    "&e/team top [loot] &f- 킬/약탈 점수 랭킹"
            ),
            List.of(
                    "&6[텔레포트]",
                    "&e/tpa <player> &f, &e/tpaccept&f, &e/tpdeny",
                    "&e/spawn&f, &e/back&f, &e/home [이름]&f, &e/sethome [이름]&f, &e/delhome [이름]",
                    "&7(비행 중이거나 방금 다른 플레이어와 맞붙어 전투 중일 때는 텔레포트를 쓸 수 없습니다.",
                    "&7 엔더펄/코러스프룻 같은 바닐라 이동수단은 그대로 가능합니다)",
                    "",
                    "&6[거래]",
                    "&e/trade <player> &f- 거래 요청, &e/tradeaccept&f, &e/tradedeny",
                    "",
                    "&6[약탈/전투]",
                    "&7- 팀은 생성 직후부터 언제든 공격받을 수 있습니다.",
                    "&7- 팀이 공격받으면 팀 전체에 경고가 뜨고, 레이드 중 공격 측이 일정 킬 이상 달성하면 '약탈 성공', 아니면 팀이 '방어 성공'으로 기록됩니다.",
                    "&7- 죽으면 드롭 아이템이 바닥에 흩어집니다(커스텀 사망 상자 없음).",
                    "&7- 5/10/20 연킬은 전체 공지됩니다."
            ),
            List.of(
                    "&6[통계/QoL]",
                    "&e/stats [player] &f- 킬/데스/K-D/플레이시간/채굴량",
                    "&e/death &f- 마지막 사망 위치로 이동",
                    "&e/coords &f- 좌표 공유(팀이 있으면 팀 채팅으로)",
                    "&e/ping [player]&f, &e/tps&f, &e/clock &f(액션바 시계 토글)",
                    "",
                    "&6[경제/보상]",
                    "&e/balance [player]&f, &e/pay <player> <금액>",
                    "&e/deposit [수량]&f - 에메랄드를 잔액으로, &e/withdraw <수량>&f - 잔액을 에메랄드로",
                    "&e/dailyreward &f- 24시간마다 1회, 스테이크 수령 가능",
                    "&e/shop &f- 아이템 교환 상점 GUI 열기",
                    "&7- 누적 플레이타임이 일정 시간을 넘길 때마다 자동으로 에메랄드 보상이 지급됩니다.",
                    "&7- 관리자가 지급하는 이벤트 상자를 우클릭하면 랜덤 보상을 받습니다. 대부분(약 81%)은",
                    "&7 스테이크/빵/석탄 같은 평범한 보상이고, 가끔(항목당 약 1.9%) OP 네더라이트 장비가 나옵니다."
            ),
            List.of(
                    "&6[채팅]",
                    "&f전체 채팅에 팀 태그가 자동으로 붙습니다. &e@닉네임&f으로 멘션 가능, 도배 방지 쿨타임 적용",
                    "&e/msg <player> <메시지>&f (별칭 /tell, /w), &e/r <메시지>&f - 마지막 상대에게 답장",
                    "",
                    "&6[관리자] &7(pillage.admin 권한 필요)",
                    "&e/report <player> <사유> &f- 누구나 신고 가능, 접수되면 관리자에게 알림",
                    "&e/staff &f- 투명화(관리자 모드) 토글",
                    "&e/inspect <player> &f- 인벤토리 읽기 전용 검사",
                    "&e/logs <kill|ban|tp|trade> [개수] &f- 최근 로그 조회",
                    "&e/pillageban <player> [사유] &f- 차단 + 로그 기록",
                    "&e/eventbox give <player> [수량] &f- 이벤트 상자 지급",
                    "&e/shop add <내는아이템> <내는수량> <받는아이템> <받는수량> &f- 상점 항목 추가",
                    "&e/shop remove <id> &f, &e/shop list &f- 상점 항목 삭제/조회",
                    "&e/anticheat &f설정은 config.yml 의 anticheat 섹션에서 조절 (기본: 경고만, 매우 널널)"
            ),
            List.of(
                    "&6[후원자] &7(결제 연동 없음 - 관리자가 수동으로 등록)",
                    "&e/donor add <player> [배지] &f/ &e/donor remove <player> &f/ &e/donor list &7(관리자)",
                    "&e/statue &f- 자기 얼굴 동상 설치 (후원자 전용)",
                    "&e/pet name <이름> &f/ &e/pet variant <종류> &f- 고양이 펫 이름·무늬 설정 (후원자 전용)",
                    "&7- 후원자로 등록되면 채팅/킬피드/이름표가 꾸며지고, 입장 시 폭죽·파티클이 터지며,",
                    "&7 고양이 펫이 자동으로 따라다니고, 허브의 '명예의 전당'에 동상이 자동으로 세워집니다.",
                    "&7 후원자가 해제되면 동상과 펫도 자동으로 사라집니다.",
                    "&7- 전투 중에는 이름표 색과 펫만 잠시 사라져 일반 플레이어처럼 보입니다",
                    "&7 (채팅/킬피드/탭리스트 표시는 항상 그대로 유지됩니다)."
            ),
            List.of(
                    "&6[미니서버] &7(친구들끼리 노는 독립된 약탈 서버)",
                    "&e/hub &f- 허브(로비)로 이동, &e/main &f- 전체 약탈 서버로 이동",
                    "&e/mini create <이름> &f- 새 미니서버 생성 (만든 사람이 그 서버의 관리자가 됨)",
                    "&e/mini join <이름> &f- 기존 미니서버에 입장",
                    "&e/mini list &f- 미니서버 목록, &e/mini info &f- 지금 있는 서버 정보",
                    "&e/mini delete [이름] &f- 삭제 (관리자만, 이름 생략시 지금 있는 서버)",
                    "&7- 미니서버는 팀/경제(잔액)/통계(킬뎃)/홈/상점이 전체 서버·다른 미니서버와",
                    "&7 완전히 분리된 새 세계입니다. 이 서버의 모든 기능을 그대로 사용할 수 있습니다.",
                    "&7- 접속하면 마지막으로 있던 서버(전체/미니서버)에 자동으로 이어서 스폰됩니다.",
                    "&7 처음 접속이거나 있던 미니서버가 삭제됐다면 허브에서 시작합니다.",
                    "&7- 허브의 스폰 구역(명예의 전당)은 아무도 부수거나 블록을 놓을 수 없습니다.",
                    "&7- 허브에 있는 주민 NPC 3명을 우클릭해도 같은 기능을 사용할 수 있습니다",
                    "&7 (전체 약탈 서버 / 미니서버 만들기 / 미니서버 참가)."
            )
    );

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        page = Math.max(1, Math.min(PAGES.size(), page));

        sender.sendMessage(Msg.of("&6&l===== PillageCore 도움말 (" + page + "/" + PAGES.size() + ") ====="));
        for (String line : PAGES.get(page - 1)) {
            sender.sendMessage(line.isEmpty() ? Component.empty() : Msg.of(line));
        }
        sender.sendMessage(Msg.of("&8/pillagehelp <페이지> 로 다른 페이지를 볼 수 있습니다."));
        return true;
    }
}
