package cc.synkdev.nexusMenus.commands;

import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MenuCommandHandler implements CommandExecutor {
    private final String menuName;

    public MenuCommandHandler(String menuName) {
        this.menuName = menuName;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            try {
                NexusMenus.getInstance().guiMap.get(menuName).open((Player) commandSender, 1);
            } catch (Exception e) {
                p.sendMessage(Util.translate("menus.does-not-exist"));
            }
        } else {
                Util.log(Util.translate("console-error"));
            }
        return true;
    }
}
