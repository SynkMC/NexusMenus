package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import cc.synkdev.nexusMenus.objects.PluginGui;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PagesEditGui {
    public Gui gui(PluginGui pGui, Consumer<PluginGui> edited) {
        AtomicBoolean picked = new AtomicBoolean(false);
        Gui gui = Gui.gui()
                .rows(3)
                .title(Util.componentify(Util.translate("menus.pages.title")))
                .disableAllInteractions()
                .create();

        for (int i = 0; i < pGui.pages; i++) {
            int finalI = i;
            gui.addItem(ItemBuilder.from(Material.PAPER)
                    .name(Util.componentify(Util.translate("menus.pages.item-name", (i+1)+"")))
                    .lore(Util.componentify(Util.translate("menus.pages.click-open")),
                            Util.componentify(Util.translate("menus.pages.click-edit")),
                            Util.componentify(Util.translate("menus.pages.click-delete")))
                    .asGuiItem(event -> {
                        picked.set(true);
                        if (event.isLeftClick()) {
                            pGui.open((Player) event.getWhoClicked(), finalI);
                            return;
                        }
                        if (event.getClick() == ClickType.MIDDLE) {
                            pGui.openEditor((Player) event.getWhoClicked(), false, finalI);
                            return;
                        }
                        pGui.setPages(pGui.getPages()-1);
                        gui(pGui, edited).open((Player) event.getWhoClicked());
                    }));
        }

        int slot = pGui.pages;

        gui.setItem(slot, ItemBuilder.skull().texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19")
                .name(Util.componentify(Util.translate("menus.pages.add")))
                .asGuiItem(event -> {
                    picked.set(true);
                    pGui.setPages(pGui.getPages()+1);
                    gui(pGui, edited).open((Player) event.getWhoClicked());
                }));

        gui.setItem(3, 5, ItemBuilder.from(Material.BARRIER)
                .name(Util.componentify(Util.translate("editing.back")))
                .asGuiItem(event -> {
                    picked.set(true);
                    edited.accept(pGui);
                }));

        gui.setCloseGuiAction(event -> {
            if (!picked.get()) Bukkit.getScheduler().runTaskLater(NexusMenus.getInstance(), () -> edited.accept(pGui), 1L);
        });
        return gui;
    }
}
