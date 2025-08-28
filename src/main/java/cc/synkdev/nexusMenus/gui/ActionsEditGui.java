package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import cc.synkdev.nexusMenus.objects.ItemActionType;
import cc.synkdev.nexusMenus.objects.PluginItem;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ActionsEditGui {
    public Gui gui(PluginItem item, Consumer<PluginItem> edited) {
        AtomicBoolean picked = new AtomicBoolean(false);
        Gui gui = Gui.gui()
                .rows(3)
                .title(Util.componentify(Util.translate("editing.actions.title")))
                .disableAllInteractions()
                .create();

        int slot = item.getActions().size();
        for (Map.Entry<String, ItemActionType> entry : item.getActions().entrySet()) {
            gui.addItem(ItemBuilder.skull().texture(entry.getValue().texture)
                    .name(Util.componentify("&r&e"+entry.getValue().name()))
                    .lore(Util.componentify(Util.translate("editing.click-edit")), Util.componentify(Util.translate("editing.click-delete")))
                    .asGuiItem(event -> {
                        picked.set(true);
                        if (event.isLeftClick()) {
                            AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                            anvil.title(Util.translate("editing.actions.anvil-title"));
                            anvil.plugin(NexusMenus.getInstance());

                            anvil.itemLeft(ItemBuilder.from(Material.PAPER)
                                    .name(Component.text(entry.getKey()))
                                    .asGuiItem().getItemStack());

                            anvil.onClick((integer, stateSnapshot) -> {
                                if (integer != 2) return List.of();

                                item.getActions().remove(entry.getKey());
                                item.getActions().put(stateSnapshot.getText(), entry.getValue());
                                gui(item, edited).open((Player) event.getWhoClicked());
                                return List.of();
                            });
                            anvil.open((Player) event.getWhoClicked());
                            return;
                        }
                        item.getActions().remove(entry.getKey());
                        gui(item, edited).open((Player) event.getWhoClicked());
                    }));
        }
        gui.setItem(slot, ItemBuilder.skull().texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19")
                .name(Util.componentify(Util.translate("editing.actions.add")))
                .asGuiItem(event -> {
                    picked.set(true);
                    typePickGui(type -> {
                        AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                        anvil.title(Util.translate("editing.actions.anvil-title"));
                        anvil.plugin(NexusMenus.getInstance());

                        anvil.itemLeft(ItemBuilder.from(Material.PAPER)
                                .name(Component.text(Util.translate("editing.actions.anvil-title")))
                                .asGuiItem().getItemStack());

                        anvil.onClick((integer, stateSnapshot) -> {
                            if (integer != 2) return List.of();

                            item.getActions().put(stateSnapshot.getText(), type);
                            gui(item, edited).open((Player) event.getWhoClicked());
                            return List.of();
                        });
                        anvil.open((Player) event.getWhoClicked());
                    }).open((Player) event.getWhoClicked());
                }));

        gui.setCloseGuiAction(event -> {
            if (!picked.get()) Bukkit.getScheduler().runTaskLater(NexusMenus.getInstance(), () -> edited.accept(item), 1L);
        });

        gui.setItem(3, 5, ItemBuilder.from(Material.BARRIER)
                .name(Util.componentify(Util.translate("editing.back")))
                .asGuiItem(event -> edited.accept(item)));
        return gui;
    }

    public Gui typePickGui(Consumer<ItemActionType> callback) {
        Gui gui = Gui.gui()
                .type(GuiType.BREWING)
                .title(Util.componentify(Util.translate("editing.actions.pick-type")))
                .disableAllInteractions()
                .create();

        gui.setItem(3, ItemBuilder.from(Material.WRITTEN_BOOK)
                .glow(false)
                .name(Util.componentify(Util.translate("editing.actions.click-type")))
                .asGuiItem());

        for (ItemActionType type : ItemActionType.values()) {
            gui.addItem(ItemBuilder.skull()
                    .texture(type.texture)
                    .name(Util.componentify("&r&e"+type.name()))
                    .asGuiItem(event -> callback.accept(type)));
        }
        return gui;
    }
}
