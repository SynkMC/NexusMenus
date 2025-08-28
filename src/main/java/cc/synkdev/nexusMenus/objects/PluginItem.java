package cc.synkdev.nexusMenus.objects;

import cc.synkdev.nexusMenus.*;
import cc.synkdev.nexusMenus.gui.ActionsEditGui;
import cc.synkdev.nexusMenus.gui.CooldownEditGui;
import cc.synkdev.nexusMenus.gui.LoreEditGui;
import cc.synkdev.nexusMenus.gui.MaterialPickerGUI;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("CodeBlock2Expr")
@Getter @Setter @AllArgsConstructor
public class PluginItem {
    private String id;
    private GuiItem guiItem;
    private ItemStack item;
    private Map<String, ItemActionType> actions;
    private ItemCooldown cooldown;
    private String permission;
    private ItemStack permissionFallback;

    public PluginItem(ItemStack item, PluginGui pGui) {
        this.item = item;
        this.actions = new HashMap<>();
        this.cooldown = new ItemCooldown(true, false, 0L, null);
        setId(pGui.getTitle() + "." + Math.toIntExact(Math.round(Math.random() * 10000000)));
        guiItem = ItemBuilder.from(item)
                .asGuiItem(event -> {
                    run((Player) event.getWhoClicked());
                });
    }

    public PluginItem(String id, ItemStack item, Map<String, ItemActionType> actions, ItemCooldown cooldown, String permission, ItemStack permissionFallback) {
        this.id = id;
        this.item = item;
        this.actions = actions;
        this.cooldown = cooldown;
        this.permission = permission;
        this.permissionFallback = permissionFallback;
        guiItem = ItemBuilder.from(item)
                .asGuiItem(event -> {
                    run((Player) event.getWhoClicked());
                });
    }

    private void run(Player pl) {
        if (!cooldown.getDisabled()) {
            if (!pl.hasPermission("nexusmenus.cooldown.bypass")) {
                CooldownManager.addCooldown(pl.getUniqueId(), id, cooldown.getOneTime() ? 0 : System.currentTimeMillis() + cooldown.getTime());
            }
        }
        
        if (getPermission() != null && !pl.hasPermission(getPermission())) {
            pl.sendMessage(Util.translate("items.no-perm"));
        }
        for (Map.Entry<String, ItemActionType> entry : actions.entrySet()) {
            String command = entry.getKey();
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                command = PlaceholderAPI.setPlaceholders(pl, entry.getKey());
            }
            switch (entry.getValue()) {
                case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                case PLAYER -> Bukkit.dispatchCommand(pl, command);
                case PLAYEROP -> {
                    boolean wasOp = pl.isOp();
                    pl.setOp(true);
                    Bukkit.dispatchCommand(pl, command);
                    pl.setOp(wasOp);
                }
            }
        }
    }
    public GuiItem getGuiItem(Player p) {
        if (cooldown.getDisabled()) return guiItem;

        if (getPermission() != null && !p.hasPermission(getPermission())) return ItemBuilder.from(getPermissionFallback() != null ? getPermissionFallback() : ConfigManager.getGlobalFallback()).asGuiItem();
        if (!CooldownManager.hasCooldown(p, id)) return guiItem;
        else return ItemBuilder.from(cooldown.getFallbackItem() == null ? ConfigManager.getGlobalFallback() : cooldown.getFallbackItem()).asGuiItem();
    }

    public void openEditor(Player p, PluginGui pGui, int slot) {
        final NexusMenus core = NexusMenus.getInstance();
        Gui gui = Gui.gui()
                .rows(5)
                .disableAllInteractions()
                .title(Component.text(Util.translate("items.editor.title")))
                .create();

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());

        gui.setItem(1, 5, ItemBuilder.from(item).asGuiItem());

        gui.setItem(3, 1, ItemBuilder.from(Material.BOOK)
                .name(Util.componentify(Util.translate("items.editor.id.set")))
                .lore(Util.componentify(Util.translate("items.editor.id.lore-1")),
                        Util.componentify(Util.translate("items.editor.id.lore-2", getId())))
                .asGuiItem(event -> {
                    AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                    anvil.plugin(core);
                    anvil.title(Util.translate("items.editor.id.anvil"));
                    anvil.itemLeft(ItemBuilder.from(Material.PAPER).name(Component.text("Enter ID")).asGuiItem().getItemStack());
                    anvil.onClick((integer, stateSnapshot) -> {
                        if (integer != 2) return List.of();
                        setId(stateSnapshot.getText());
                        openEditor(p, pGui, slot);
                        return List.of();
                    });
                    anvil.open(p);
                }));
        gui.setItem(3, 2, ItemBuilder.from(Material.NAME_TAG).name(Util.componentify(Util.translate("items.editor.rename.item"))).asGuiItem(event -> {
            AnvilGUI.Builder anvil = new AnvilGUI.Builder();
            anvil.title(Util.translate("items.editor.rename.anvil"));
            anvil.itemLeft(item);
            anvil.plugin(core);
            anvil.onClick((integer, stateSnapshot) -> {
                if (integer != 2) return List.of();
                ItemMeta meta = item.getItemMeta();
                assert meta != null;
                meta.setDisplayName(Util.setColors(stateSnapshot.getText()));
                item.setItemMeta(meta);
                pGui.itemsMap.put(slot, this);
                pGui.save();
                PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                pGui1.itemsMap.get(slot).openEditor(p, pGui1, slot);
                return List.of();
            });
            anvil.open(p);
        }));

        gui.setItem(3, 3, ItemBuilder.from(Material.GRASS_BLOCK).name(Util.componentify(Util.translate("editing.material.item"))).asGuiItem(event -> {
            new MaterialPickerGUI().gui(1, null, () -> pGui.itemsMap.get(slot).openEditor(p, pGui, slot), material -> {
                        item.setType(material);
                        pGui.itemsMap.put(slot, this);
                        pGui.save();
                        PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                        pGui1.itemsMap.get(slot).openEditor(p, pGui1, slot);
                    }
            ).open(p);

        }));

        gui.setItem(3, 4, ItemBuilder.from(Material.WRITABLE_BOOK).name(Util.componentify(Util.translate("editing.lore.item"))).asGuiItem(event -> {
            new LoreEditGui().gui(item, itemStack -> {
                this.setItem(itemStack);
                pGui.itemsMap.put(slot, this);
                pGui.save();
                PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                pGui1.itemsMap.get(slot).openEditor(p, pGui1, slot);
            }).open(p);
        }));

        gui.setItem(3, 5, ItemBuilder.from(Material.COMMAND_BLOCK)
                .name(Util.componentify(Util.translate("editing.actions.item")))
                .asGuiItem(event -> {
                    new ActionsEditGui().gui(this, item -> {
                        pGui.itemsMap.put(slot, item);
                        pGui.save();
                        PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                        pGui1.itemsMap.get(slot).openEditor(p, pGui1, slot);
                    }).open(p);
                }));

        gui.setItem(3, 6, ItemBuilder.from(Material.CLOCK)
                .name(Util.componentify(Util.translate("editing.cooldown.item")))
                .asGuiItem(event -> {
                    AtomicReference<Consumer<ItemStack>> saveRef = new AtomicReference<>();
                    Consumer<PluginItem> edited = item -> {
                        pGui.itemsMap.put(slot, item);
                        pGui.save();
                        PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                        pGui1.itemsMap.get(slot).openEditor(p, pGui1, slot);
                    };

                    Consumer<ItemStack> save = item -> {
                        getCooldown().setFallbackItem(item);
                        pGui.itemsMap.put(slot, this);
                        pGui.save();
                        PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                        PluginItem pItem1 = pGui1.itemsMap.get(slot);
                        new CooldownEditGui().gui(pItem1, pGui1, edited, saveRef.get()).open(p);
                    };

                    saveRef.set(save);

                    new CooldownEditGui().gui(this, pGui, edited, save).open(p);
                }));

        gui.setItem(3, 7, ItemBuilder.from(Material.BOOK)
                .name(Util.componentify(Util.translate("items.editor.permission.item")))
                .lore(Util.componentify(Util.translate("items.editor.permission.lore-1")),
                        Util.componentify(Util.translate("items.editor.permission.lore-2", (getPermission() == null ? "None" : getPermission()))),
                        (getPermission() == null ? null : Util.componentify(Util.translate("items.editor.permission.lore-3"))))
                .asGuiItem(event -> {
                    if (getPermission() != null && event.isRightClick()) {
                        setPermission(null);
                        openEditor(p, pGui,slot);
                        return;
                    }

                    AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                    anvil.plugin(core);
                    anvil.title(Util.translate("items.editor.permission.anvil"));
                    anvil.itemLeft(ItemBuilder.from(Material.PAPER).name(Component.text(Util.translate("items.editor.permission.anvil"))).asGuiItem().getItemStack());
                    anvil.onClick((integer, stateSnapshot) -> {
                       if (integer != 2) return List.of();
                       setPermission("nexusmenus." + pGui.getName() + "." + stateSnapshot.getText());
                       pGui.save();
                       openEditor(p, pGui, slot);
                       return List.of();
                    });
                    anvil.open(p);
                }));

        gui.setItem(4, 7, ItemBuilder.from(Material.BARRIER)
                .name(Util.componentify(Util.translate("items.editor.permission.fallback")))
                .lore(Util.componentify(Util.translate("items.editor.permission.fallback-lore-1")), (getPermissionFallback() == null ? null : Util.componentify(Util.translate("items.editor.permission.fallback-lore-2")))).asGuiItem(event -> {
                    if (getPermissionFallback() != null && event.isRightClick()) {
                        setPermissionFallback(null);
                        pGui.itemsMap.put(slot, this);
                        pGui.save();
                        PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                        pGui1.itemsMap.get(slot).openEditor(p, pGui1, slot);
                        return;
                    }

                    AtomicReference<Consumer<ItemStack>> saveRef = new AtomicReference<>();
                    Consumer<PluginItem> edited = item -> {
                        setPermissionFallback(item.getItem());
                        pGui.itemsMap.put(slot, this);
                        pGui.save();
                        PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                        pGui1.itemsMap.get(slot).openEditor(p, pGui1, slot);
                    };

                    Consumer<ItemStack> save = item -> {
                        setPermissionFallback(item);
                        pGui.itemsMap.put(slot, this);
                        pGui.save();
                        PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                        PluginItem pItem1 = pGui1.itemsMap.get(slot);
                        new PluginItem(
                                (pItem1.getPermissionFallback() == null ? ConfigManager.getGlobalFallback() : pItem1.getPermissionFallback()),
                                pGui1
                        ).openEditor(p, edited, saveRef.get());
                    };

                    saveRef.set(save);

                    new PluginItem(
                            (getPermissionFallback() == null ? ConfigManager.getGlobalFallback().clone() : getPermissionFallback()),
                            pGui
                    ).openEditor(p, edited, save);

                }));

        gui.setItem(3, 9, ItemBuilder.from(Material.BARRIER).name(Util.componentify(Util.translate("items.editor.remove"))).asGuiItem(event -> {
            pGui.itemsMap.remove(slot);
            pGui.save();
            PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
            pGui1.openEditor(p, false, 1);
        }));

        gui.setItem(5, 5, ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("editing.back"))).asGuiItem(event -> pGui.openEditor(p, false, 1)));

        gui.open(p);
    }
    public void openEditor(Player p, Consumer<PluginItem> callback, Consumer<ItemStack> save) {
        final NexusMenus core = NexusMenus.getInstance();
        AtomicBoolean picked = new AtomicBoolean(false);
        Gui gui = Gui.gui()
                .rows(5)
                .disableAllInteractions()
                .title(Component.text(Util.translate("items.editor.title")))
                .create();

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());

        gui.setItem(1, 5, ItemBuilder.from(item).asGuiItem());
        gui.setItem(3, 2, ItemBuilder.from(Material.NAME_TAG).name(Util.componentify(Util.translate("items.editor.rename.item"))).asGuiItem(event -> {
            picked.set(true);
            AnvilGUI.Builder anvil = new AnvilGUI.Builder();
            anvil.title(Util.translate("items.editor.rename.anvil"));
            anvil.itemLeft(item);
            anvil.plugin(core);
            anvil.onClick((integer, stateSnapshot) -> {
                picked.set(true);
                if (integer != 2) return List.of();
                ItemMeta meta = item.getItemMeta();
                assert meta != null;
                meta.setDisplayName(stateSnapshot.getText());
                item.setItemMeta(meta);
                save.accept(item);
                return List.of();
            });
            anvil.open(p);
        }));

        gui.setItem(3, 3, ItemBuilder.from(Material.GRASS_BLOCK).name(Util.componentify(Util.translate("editing.material.item"))).asGuiItem(event -> {
            picked.set(true);
            new MaterialPickerGUI().gui(1, null, () -> openEditor(p, callback, save), material -> {
                        item.setType(material);
                        save.accept(item);
                    }
            ).open(p);

        }));

        gui.setItem(3, 4, ItemBuilder.from(Material.WRITABLE_BOOK).name(Util.componentify(Util.translate("editing.lore.item"))).asGuiItem(event -> {
            picked.set(true);
            new LoreEditGui().gui(item, itemStack -> {
                save.accept(itemStack);
            }).open(p);
        }));


        gui.setItem(5, 5, ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("editing.back"))).asGuiItem(event -> {
            picked.set(true);
            callback.accept(this);
        }));

        gui.setCloseGuiAction(event -> {
            if (!picked.get()) Bukkit.getScheduler().runTaskLater(core, () -> callback.accept(this), 1L);
        });

        gui.open(p);
    }
}
