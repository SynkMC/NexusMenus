package cc.synkdev.nexusMenus.commands;

import cc.synkdev.nexusMenus.*;
import cc.synkdev.nexusMenus.gui.MenusListGui;
import cc.synkdev.nexusMenus.objects.GUIType;
import cc.synkdev.nexusMenus.objects.PluginGui;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("nexusmenus|nm")
public class MainCmd extends BaseCommand {
    private final NexusMenus core = NexusMenus.getInstance();
    @Default
    public void onDefault(Player pl) {
        if (ConfigManager.isRequireOp()) {
            if (!pl.isOp()) pl.sendMessage(Util.translate("comands.no-perm"));
        } else {
            if (!pl.hasPermission("nexusmenus.command.nexusmenus")) pl.sendMessage(Util.translate("commands.no-perm"));
        }
        new MenusListGui().gui(1).open(pl);
    }

    @Subcommand("create")
    public void onCreate(Player pl, String[] args) {
        if (ConfigManager.isRequireOp()) {
            if (!pl.isOp()) pl.sendMessage(Util.translate("comands.no-perm"));
        } else {
            if (!pl.hasPermission(ConfigManager.getCreationPerm())) pl.sendMessage(Util.translate("commands.no-perm"));
        }

        if (args.length == 0) {
            pl.sendMessage(Util.translate("commands.require-name"));
            return;
        }

        String name = args[0];

        if (core.guiMap.containsKey(name)) {
            pl.sendMessage(Util.translate("menus.double-name"));
            return;
        }

        if (args.length < 2) {
            core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), ConfigManager.getDefRows(), ConfigManager.getDefType()));
            name = Util.sanitize(name);
            core.registerMenuCommand(name, name);
            new MenusListGui().gui(1).open(pl);
            return;
        }

        String type = args[1];
        GUIType gType;
        try {
            gType = GUIType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            pl.sendMessage(Util.translate("commands.gui-types"));
            return;
        }

        if (args.length == 2) {
            if (gType == GUIType.CHEST) {
                core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), ConfigManager.getDefRows(), GUIType.CHEST));
            } else {
                core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), 0, gType));
            }
            name = Util.sanitize(name);
            core.registerMenuCommand(name, name);
            new MenusListGui().gui(1).open(pl);
            return;
        }

        if (gType != GUIType.CHEST) {
            core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), 0, gType));
            name = Util.sanitize(name);
            core.registerMenuCommand(name, name);
            new MenusListGui().gui(1).open(pl);
            return;
        }

        int rows;
        try {
            rows = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            pl.sendMessage(Util.translate("commands.require-rows"));
            return;
        }

        if (rows >= 1 && rows <= 6) {
            core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), rows, GUIType.CHEST));
            name = Util.sanitize(name);
            core.registerMenuCommand(name, name);
            new MenusListGui().gui(1).open(pl);
            return;
        }

        pl.sendMessage(Util.translate("commands.require-rows"));
    }

    @Subcommand("edit")
    @CommandPermission("nexusmenus.command.edit")
    public void onEdit(Player pl, String[] args) {
        if (args.length == 0) {
            new MenusListGui().gui(1).open(pl);
            return;
        }

        String name = args[0];
        if (!core.guiMap.containsKey(name)) {
            pl.sendMessage(Util.translate("menus.does-not-exist"));
            return;
        }

        core.guiMap.get(name).openEditor(pl, false, 1);
    }

    @Subcommand("clearcooldown")
    @CommandCompletion("@players")
    @CommandPermission("nexusmenus.command.clearcooldown")
    public void onClearCd(Player pl, String[] args) {
        if (args.length == 0) {
            pl.sendMessage(Util.translate("commands.require-player"));
            return;
        }

        String target = args[0];
        OfflinePlayer player = Bukkit.getOfflinePlayer(target);
        if (!(player.hasPlayedBefore() || player.isOnline())) {
            pl.sendMessage(Util.translate("commands.cannot-find-player"));
            return;
        } else if (args.length == 1) {
            CooldownManager.clearCooldown(player.getUniqueId());
            return;
        }

        if (args.length != 2) {
            pl.sendMessage(Util.translate("commands.require-id"));
            return;
        }

        String id = args[1];
        CooldownManager.clearCooldown(pl.getUniqueId(), id);
        pl.sendMessage(Util.translate("cooldown-reset"));
    }

    @Subcommand("reload")
    @CommandPermission("nexusmenus.command.reload")
    public void onReload(CommandSender sender) {
        long time = System.currentTimeMillis();
        ConfigManager.setup(core.getConfig());
        core.reloadLang();
        core.loadMenus();

        sender.sendMessage(Util.color("&aSuccessfully reloaded the plugin in "+(System.currentTimeMillis()-time)+" ms!"));
    }
}
