package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import cc.synkdev.nexusMenus.objects.GUIType;
import cc.synkdev.nexusMenus.objects.PluginGui;
import cc.synkdev.nexusMenus.objects.PluginItem;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PageSwitchersEditGui {
    public Gui gui(PluginGui pGui, Consumer<PluginGui> edited) {
        AtomicBoolean picked = new AtomicBoolean(false);
        Gui gui = Gui.gui()
                .rows(5)
                .title(Util.componentify(Util.translate("menus.switchers.title")))
                .disableAllInteractions()
                .create();

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Util.componentify(" ")).asGuiItem());

        gui.setItem(1, 5, ItemBuilder.from(pGui.getType().item).name(Util.componentify(pGui.getTitle())).asGuiItem());

        gui.setItem(3, 3, ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.switchers.set-prev-slot")))
                .asGuiItem(event -> {
                    picked.set(true);
                    slotPicker(pGui, slot -> {
                        pGui.setPrevPageSlot(slot);
                        gui(pGui, edited).open((Player) event.getWhoClicked());
                    }).open((Player) event.getWhoClicked());
                }));

        gui.setItem(3, 4, ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.switchers.set-next-slot")))
                .asGuiItem(event -> {
                    picked.set(true);
                    slotPicker(pGui, slot -> {
                        pGui.setNextPageSlot(slot);
                        gui(pGui, edited).open((Player) event.getWhoClicked());
                    }).open((Player) event.getWhoClicked());
                }));

        if (pGui.getPrevPageItem() == null) {
            pGui.setPrevPageItem(ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.pages.prev-page"))).asGuiItem().getItemStack());
            pGui.save();
        }
        ItemStack copy = pGui.getPrevPageItem().clone();
        gui.setItem(3, 6, ItemBuilder.from(copy).name(Util.componentify(Util.translate("menus.switchers.set-prev-item")))
                .asGuiItem(event -> {
                    picked.set(true);
                    PluginItem pItem = new PluginItem(pGui.getPrevPageItem(), pGui);
                    pItem.openEditor((Player) event.getWhoClicked(), item -> {
                        pGui.setPrevPageItem(item.getItem());
                        gui(pGui, edited).open((Player) event.getWhoClicked());
                    }, save -> {
                        pGui.setPrevPageItem(save);
                        gui(pGui, edited).open((Player) event.getWhoClicked());
                    });
                }));

        if (pGui.getNextPageItem() == null) {
            pGui.setNextPageItem(ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.pages.next-page"))).asGuiItem().getItemStack());
            pGui.save();
        }
        copy = pGui.getNextPageItem().clone();
        gui.setItem(3, 7, ItemBuilder.from(copy).name(Util.componentify(Util.translate("menus.switchers.set-next-item")))
                .asGuiItem(event -> {
                    picked.set(true);
                    PluginItem pItem = new PluginItem(pGui.getNextPageItem(), pGui);
                    pItem.openEditor((Player) event.getWhoClicked(), item -> {
                        pGui.setNextPageItem(item.getItem());
                        gui(pGui, edited).open((Player) event.getWhoClicked());
                    }, save -> {
                        pGui.setNextPageItem(save);
                        gui(pGui, edited).open((Player) event.getWhoClicked());
                    });
                }));

        gui.setItem(5, 5, ItemBuilder.from(Material.BARRIER)
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

    public Gui slotPicker(PluginGui pGui, Consumer<Integer> slot) {
        Gui gui;
        if (pGui.getType() == GUIType.CHEST) {
            gui = Gui.gui()
                    .rows(pGui.getRows())
                    .title(Util.componentify(Util.translate("menus.switchers.slot-title")))
                    .disableAllInteractions()
                    .create();
        } else {
            gui = Gui.gui()
                    .type(pGui.getType().guiType)
                    .title(Util.componentify(Util.translate("menus.switchers.slot-title")))
                    .disableAllInteractions()
                    .create();
        }

        gui.getFiller().fill(ItemBuilder.skull()
                .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19")
                .name(Util.componentify(Util.translate("menus.switchers.slot-select")))
                .asGuiItem(event -> slot.accept(event.getSlot())));

        return gui;
    }
}
