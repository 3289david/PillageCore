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
                    "&7(전투 중·소속 팀 레이드 중에는 텔레포트가 제한됩니다)",
                    "",
                    "&6[거래]",
                    "&e/trade <player> &f- 거래 요청, &e/tradeaccept&f, &e/tradedeny",
                    "",
                    "&6[약탈/전투]",
                    "&7- 신규 팀은 생성 후 설정된 시간(기본 24시간) 동안 공격받지 않습니다.",
                    "&7- 보호가 끝난 팀이 공격받으면 팀 전체에 경고가 뜨고 15분간 텔레포트가 막힙니다.",
                    "&7- 레이드 중 공격 측이 일정 킬 이상 달성하면 '약탈 성공', 아니면 팀이 '방어 성공'으로 기록됩니다.",
                    "&7- PvP 피격 시 30초간 전투 상태가 되며, 전투/레이드 중 로그아웃하면 처형됩니다.",
                    "&7- 죽으면 30초간 사망 상자가 생기며 팀원만 열 수 있습니다. 5/10/20 연킬은 전체 공지됩니다."
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
                    "&e/dailyreward &f- 24시간마다 1회 수령 가능",
                    "&7- 누적 플레이타임이 일정 시간을 넘길 때마다 자동으로 보상이 지급됩니다.",
                    "&7- 관리자가 지급하는 이벤트 상자를 우클릭하면 랜덤 보상을 받습니다."
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
                    "&e/logs <trade|kill|ban|tp> [개수] &f- 최근 로그 조회",
                    "&e/pillageban <player> [사유] &f- 차단 + 로그 기록",
                    "&e/eventbox give <player> [수량] &f- 이벤트 상자 지급",
                    "&e/anticheat &f설정은 config.yml 의 anticheat 섹션에서 조절 (기본: 경고만, 매우 널널)"
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
