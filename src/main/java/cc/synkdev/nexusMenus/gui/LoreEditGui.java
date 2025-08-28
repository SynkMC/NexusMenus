package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoreEditGui {
    public Gui gui(ItemStack item, Consumer<ItemStack> edited) {
        Gui gui = Gui.gui()
                .rows(3)
                .title(Util.componentify(Util.translate("editing.lore.title")))
                .disableAllInteractions()
                .create();

        boolean lore = false;
        if (item.getItemMeta() != null) {
            lore = item.getItemMeta().getLore() != null;
            if (lore) {
                for (String s : item.getItemMeta().getLore()) {
                    gui.addItem(ItemBuilder.from(Material.PAPER)
                            .name(Util.componentify(s))
                            .lore(Util.componentify(Util.translate("editing.click-edit")),
                                    Util.componentify(Util.translate("editing.click-delete")))
                            .asGuiItem(event -> {
                                ItemMeta meta = item.getItemMeta();
                                if (event.isLeftClick()) {
                                    AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                                    anvil.title(Util.translate("editing.lore.title-anvil"));
                                    ItemStack gItem = ItemBuilder.from(item).name(Util.componentify(item.getItemMeta().getLore().get(event.getSlot()))).asGuiItem().getItemStack();
                                    anvil.itemLeft(gItem);
                                    anvil.plugin(NexusMenus.getInstance());
                                    anvil.onClick((integer, stateSnapshot) -> {
                                        if (integer != 2) return List.of();
                                        List<String> loreList = meta.getLore();
                                        loreList.set(event.getSlot(), stateSnapshot.getText());
                                        meta.setLore(loreList);
                                        item.setItemMeta(meta);
                                        gui(item, edited).open((Player) event.getWhoClicked());
                                        return List.of();
                                    });
                                    anvil.open((Player) event.getWhoClicked());
                                    return;
                                }
                                List<String> loreList = meta.getLore();
                                loreList.remove(s);
                                meta.setLore(loreList);
                                item.setItemMeta(meta);
                                gui(item, edited).open((Player) event.getWhoClicked());
                            }));
                }
            }
        }

        int slot = item.getItemMeta() == null ? 0 : (lore ? item.getItemMeta().getLore().size() : 0);

        gui.setItem(slot, ItemBuilder.skull().texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19")
                .name(Util.componentify(Util.translate("editing.lore.add")))
                .asGuiItem(event -> {
                    AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                    anvil.title(Util.translate("editing.lore.title-anvil"));

                    ItemStack clone = item.clone();
                    ItemMeta meta = item.getItemMeta();
                    ItemStack gItem = ItemBuilder.from(clone).name(Component.text(Util.translate("editing.lore.enter-line"))).asGuiItem().getItemStack();
                    anvil.itemLeft(gItem);
                    anvil.plugin(NexusMenus.getInstance());
                    anvil.onClick((integer, stateSnapshot) -> {
                        if (integer != 2) return List.of();
                        List<String> loreL = new ArrayList<>();
                        if (meta.getLore() != null) {
                            loreL.addAll(meta.getLore());
                        }
                        loreL.add(Util.color(stateSnapshot.getText()));
                        meta.setLore(loreL);
                        item.setItemMeta(meta);
                        gui(item, edited).open((Player) event.getWhoClicked());
                        return List.of();
                    });
                    anvil.open((Player) event.getWhoClicked());
                }));

        gui.setItem(3, 5, ItemBuilder.from(Material.BARRIER)
                .name(Util.componentify(Util.translate("editing.back")))
                .asGuiItem(event -> edited.accept(item)));
        return gui;
    }
}
