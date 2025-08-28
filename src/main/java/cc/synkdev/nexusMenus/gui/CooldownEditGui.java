package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.*;
import cc.synkdev.nexusMenus.objects.ItemCooldown;
import cc.synkdev.nexusMenus.objects.PluginGui;
import cc.synkdev.nexusMenus.objects.PluginItem;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CooldownEditGui {
    public Gui gui(PluginItem pItem, PluginGui pGui, Consumer<PluginItem> callback, Consumer<ItemStack> save) {
        ItemStack defaultFallback = ConfigManager.getGlobalCdFallback();
        AtomicBoolean picked = new AtomicBoolean(false);
        Gui gui = Gui.gui().rows(5).title(Util.componentify(Util.translate("editing.cooldown.title"))).disableAllInteractions().create();

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Util.componentify(" ")).asGuiItem());

        gui.setItem(1, 5, ItemBuilder.from(pItem.getItem()).asGuiItem());

        gui.setItem(3, 3, ItemBuilder.from(Material.CLOCK).name(Util.componentify("&eCooldown type"))
                .lore(Util.componentify(Util.translate("editing.cooldown.click-scroll")),
                        Util.componentify(""),
                        Util.componentify((pItem.getCooldown() == null || pItem.getCooldown().getDisabled() ? ChatColor.BOLD : "") + Util.translate("editing.cooldown.disabled")),
                        Util.componentify((pItem.getCooldown() != null && pItem.getCooldown().getOneTime() ? ChatColor.BOLD : "") + Util.translate("editing.cooldown.one-time")),
                        Util.componentify((pItem.getCooldown() != null && !pItem.getCooldown().getOneTime() ? ChatColor.BOLD : "") + Util.translate("editing.cooldown.time-limit")))
                .asGuiItem(event -> {
                    picked.set(true);
                    if (pItem.getCooldown() == null || pItem.getCooldown().getDisabled()) {
                        ItemCooldown cd;
                        if (pItem.getCooldown() == null) cd = new ItemCooldown(false,true, 3600000L, defaultFallback);
                        else cd = new ItemCooldown(false, true, pItem.getCooldown().getTime(), pItem.getCooldown().getFallbackItem());
                        pItem.setCooldown(cd);
                    } else if (pItem.getCooldown().getOneTime()) {
                        ItemCooldown cd;
                        cd = new ItemCooldown(false, false, pItem.getCooldown().getTime(), pItem.getCooldown().getFallbackItem());
                        pItem.setCooldown(cd);
                    } else {
                        ItemCooldown cd;
                        cd = new ItemCooldown(true, false, pItem.getCooldown().getTime(), pItem.getCooldown().getFallbackItem());
                        pItem.setCooldown(cd);
                    }
                    gui(pItem, pGui, callback, save).open((Player) event.getWhoClicked());
                }));

        gui.setItem(3, 4, ItemBuilder.from(Material.HOPPER)
                .name(Util.componentify(Util.translate("editing.cooldown.reset-all")))
                .asGuiItem(event -> {
                    CooldownManager.clearCooldown(pItem.getId());
                    event.getWhoClicked().sendMessage(Util.translate("editing.cooldown.reset-all-confirm"));
                }));

        if (pItem.getCooldown() != null) {
            gui.setItem(3, 6, ItemBuilder.from(Material.ITEM_FRAME).name(Util.componentify(Util.translate("editing.cooldown.cd-item"))).asGuiItem(event -> {
                picked.set(true);
                ItemStack itemStack = pItem.getCooldown().getFallbackItem() == null ? ConfigManager.getGlobalCdFallback().clone() : pItem.getCooldown().getFallbackItem();
                PluginItem pItem1 = new PluginItem(itemStack, pGui);

                pItem1.openEditor((Player) event.getWhoClicked(), item -> {
                    pItem.getCooldown().setFallbackItem(item.getItem());
                    gui(pItem, pGui, callback, save).open((Player) event.getWhoClicked());
                }, save);

            }));
        }

        if (pItem.getCooldown() != null && !pItem.getCooldown().getOneTime()) {
            gui.setItem(3, 7, ItemBuilder.from(Material.CLOCK).name(Util.componentify(Util.translate("editing.cooldown.duration")))
                    .lore(Util.componentify(Util.translate("editing.cooldown.current-duration", Util.formatMillisecondsToDuration(pItem.getCooldown().getTime()))))
                    .asGuiItem(event -> {
                        picked.set(true);
                        AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                        anvil.title(Util.translate("editing.cooldown.set-duration-anvil"));
                        anvil.itemLeft(ItemBuilder.from(Material.CLOCK).name(Component.text(Util.translate("editing.cooldown.duration-anvil"))).asGuiItem().getItemStack());
                        anvil.plugin(NexusMenus.getInstance());
                        anvil.onClick((integer, stateSnapshot) -> {
                            if (integer != 2) return List.of();
                            try {
                                pItem.getCooldown().setTime(Util.parseDurationToMillieconds(stateSnapshot.getText()));
                            } catch (NumberFormatException e) {
                                stateSnapshot.getPlayer().sendMessage(Util.translate("editing.cooldown.invalid-format"));
                            }
                            gui(pItem, pGui, callback, save).open((Player) event.getWhoClicked());
                            return List.of();
                        });
                        anvil.open((Player) event.getWhoClicked());
                    }));
        }

        gui.setItem(5, 5, ItemBuilder.from(Material.BARRIER)
                .name(Util.componentify(Util.translate("editing.back")))
                .asGuiItem(event -> callback.accept(pItem)));

        gui.setCloseGuiAction(event -> {
            if (!picked.get()) Bukkit.getScheduler().runTaskLater(NexusMenus.getInstance(), () -> callback.accept(pItem), 1L);
        });
        return gui;
    }
}
