package com.mingyu.pillage.donor;

import com.mingyu.pillage.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class PetCommand implements CommandExecutor, TabCompleter {

    private final DonorManager donorManager;
    private final DonorPetManager petManager;

    public PetCommand(DonorManager donorManager, DonorPetManager petManager) {
        this.donorManager = donorManager;
        this.petManager = petManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (!donorManager.isDonor(player.getUniqueId())) {
            player.sendMessage(Msg.of("&c후원자 전용 명령어입니다."));
            return true;
        }

        if (args.length >= 1 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
            boolean enabled = args[0].equalsIgnoreCase("on");
            petManager.setEnabled(player, enabled);
            player.sendMessage(enabled
                    ? Msg.of("&a펫을 다시 불러왔습니다.")
                    : Msg.of("&7펫을 숨겼습니다. &e/pet on&7 으로 다시 부를 수 있습니다."));
            return true;
        }

        if (args.length < 2 || !(args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("variant"))) {
            player.sendMessage(Msg.of("&c사용법: /pet name <이름> &f/ &c/pet variant <종류> &f/ &c/pet on|off"));
            player.sendMessage(Msg.of("&7종류: " + Arrays.stream(Cat.Type.values()).map(Cat.Type::name).collect(Collectors.joining(", "))));
            return true;
        }

        if (args[0].equalsIgnoreCase("name")) {
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (name.length() > 24) {
                player.sendMessage(Msg.of("&c이름은 24자 이하여야 합니다."));
                return true;
            }
            petManager.setName(player, name);
            player.sendMessage(Msg.of("&a펫 이름을 '" + name + "' (으)로 설정했습니다."));
            return true;
        }

        try {
            Cat.Type type = Cat.Type.valueOf(args[1].toUpperCase());
            petManager.setVariant(player, type);
            player.sendMessage(Msg.of("&a펫 종류를 " + type.name() + " (으)로 설정했습니다."));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Msg.of("&c알 수 없는 종류입니다. 사용 가능: "
                    + Arrays.stream(Cat.Type.values()).map(Cat.Type::name).collect(Collectors.joining(", "))));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("name", "variant", "on", "off");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("variant")) {
            return Arrays.stream(Cat.Type.values()).map(Cat.Type::name).toList();
        }
        return List.of();
    }
}
