package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.ConfigManager;
import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import cc.synkdev.nexusMenus.objects.PluginGui;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.Map;

public class MenusListGui {
    private final NexusMenus core = NexusMenus.getInstance();
    public Gui gui(int page) {
        Gui gui = Gui.gui()
                .disableAllInteractions()
                .rows(6)
                .title(Util.componentify(Util.translate("menus.list.title")))
                .create();

        int max = (core.guiMap.size()+45)/46;
        if (page > 1) {
            gui.setItem(6, 4, ItemBuilder.from(Material.ARROW)
                    .name(Util.componentify(Util.translate("menus.pages.prev-page")))
                    .asGuiItem(event -> gui(page-1).open((Player) event.getWhoClicked())));
        }
        if (page < max) {
            gui.setItem(6, 6, ItemBuilder.from(Material.ARROW)
                    .name(Util.componentify(Util.translate("menus.pages.next-page")))
                    .asGuiItem(event -> gui(page+1).open((Player) event.getWhoClicked())));
        }
        gui.setItem(6, 5, ItemBuilder.skull().texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19")
                .name(Util.componentify(Util.translate("menus.list.create")))
                .asGuiItem(event -> {
                    Player p = (Player) event.getWhoClicked();
                    if (ConfigManager.isRequireOpCreation()) {
                        if (!p.isOp()) p.sendMessage(Util.translate("commands.no-perm"));
                    } else {
                        if (!p.hasPermission(ConfigManager.getCreationPerm())) p.sendMessage(Util.translate("commands.no-perm"));
                    }

                    AnvilGUI.Builder builder = new AnvilGUI.Builder();
                    builder.title(Util.translate("menus.list.name-anvil"));
                    builder.plugin(core);
                    builder.itemLeft(ItemBuilder.from(Material.PAPER)
                            .name(Component.text(Util.translate("menus.list.name-anvil")))
                            .asGuiItem().getItemStack());

                    builder.onClick((integer, stateSnapshot) -> {
                        if (integer != 2) return List.of();

                        String name = Util.color(stateSnapshot.getText());
                        if (name.isEmpty()) return List.of();
                        new CreationGui().typePicker(name).open(stateSnapshot.getPlayer());

                        return List.of();
                    });

                    builder.open((Player) event.getWhoClicked());
                }));

        for (int i = 0; i < 45; i++) {
            i = i + (page-1)*45;
            if (core.guiMap.size() > i) {
                int index = 0;
                for (Map.Entry<String, PluginGui> entry : core.guiMap.entrySet()) {
                    if (index == i) {
                        gui.addItem(ItemBuilder.from(entry.getValue().getType().item)
                                .name(Util.componentify("&e"+entry.getValue().getTitle()))
                                .lore(Util.componentify(Util.translate("menus.list.click-open")), Util.componentify(Util.translate("menus.list.click-manage")), Util.componentify(Util.translate("menus.list.click-edit")))
                                .asGuiItem(event -> {
                                    Player p = (Player) event.getWhoClicked();
                                    if (event.isLeftClick()) {
                                        entry.getValue().open((Player) event.getWhoClicked(), 1);
                                        return;
                                    }

                                    if (event.getClick() == ClickType.MIDDLE) {
                                        if (!p.hasPermission("nexusmenus.manager")) {
                                            p.sendMessage(Util.translate("commands.no-perm"));
                                        }
                                        entry.getValue().openManager((Player) event.getWhoClicked());
                                        return;
                                    }

                                    if (!p.hasPermission("nexusmenus.command.edit")) {
                                        p.sendMessage(Util.translate("commands.no-perm"));
                                    }
                                    entry.getValue().openEditor((Player) event.getWhoClicked(),false, 1);
                                }));
                        break;
                    }
                    index++;
                }
            }
        }
        return gui;
    }
}
