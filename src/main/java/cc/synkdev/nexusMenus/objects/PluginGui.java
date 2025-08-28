package cc.synkdev.nexusMenus.objects;

import cc.synkdev.nexusMenus.ConfigManager;
import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import cc.synkdev.nexusMenus.gui.*;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter @Setter @AllArgsConstructor
public class PluginGui {
    public String name;
    public String title;
    public String permission;
    public int rows;
    public GUIType type;
    public int pages;
    public Map<Integer, PluginItem> itemsMap;
    public ItemStack prevPageItem;
    public int prevPageSlot;
    public ItemStack nextPageItem;
    public int nextPageSlot;
    public PluginGui(String name, String title, int rows, GUIType type) {
        this.name = name;
        this.title = title;
        this.permission = "nexusmenus."+name;
        this.rows = rows;
        this.type = type;
        this.itemsMap = new HashMap<>();
        this.setPages(1);
    }

    public PluginGui(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        setName(config.getString("name"));
        setTitle(config.getString("title"));
        setType(GUIType.valueOf(config.getString("type")));
        setRows(config.getInt("rows"));
        setPermission(config.getString("permission"));
        setPages(config.getInt("pages-count"));
        itemsMap = new HashMap<>();

        if (getType() == GUIType.CHEST) {
            try {
                for (String page : config.getConfigurationSection("pages").getKeys(false)) {
                    for (String item : config.getConfigurationSection("pages").getConfigurationSection(page).getConfigurationSection("items").getKeys(false)) {
                        ConfigurationSection section = config.getConfigurationSection("pages." + page + ".items." + item);


                        int pageInt = Integer.parseInt(page);
                        int slot = (pageInt - 1) * getRows() * 9 + (Integer.parseInt(item));
                        itemsMap.put(slot, loadItem(section));
                    }
                }
            } catch (Exception ignored) {}
        } else {
            if (config.getConfigurationSection("items") != null) {
                for (String s : config.getConfigurationSection("items").getKeys(false)) {
                    int slot = Integer.parseInt(s);
                    itemsMap.put(slot, loadItem(config.getConfigurationSection("items").getConfigurationSection(s)));
                }
            }
        }

        setPrevPageItem(Util.restoreItem(config.getConfigurationSection("switchers.previous.item")));
        setPrevPageSlot(config.getInt("switchers.previous.slot"));

        setNextPageItem(Util.restoreItem(config.getConfigurationSection("switchers.next.item")));
        setNextPageSlot(config.getInt("switchers.next.slot"));
    }

    private PluginItem loadItem(ConfigurationSection section) {
        String id = section.getString("id");
        ItemStack itemStack = Util.restoreItem(section);
        Map<String, ItemActionType> actionMap = new HashMap<>();

        for (String action : section.getStringList("actions")) {
            Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]\\s*(.*)");
            Matcher matcher = pattern.matcher(action);

            if (matcher.matches()) {
                ItemActionType actionType = ItemActionType.valueOf(matcher.group(1).toUpperCase());
                actionMap.put(matcher.group(2), actionType);
            }
        }
        ItemCooldown cooldown = new ItemCooldown(!section.getBoolean("cooldown.disabled"), section.getBoolean("cooldown.one-time-use"),
                section.getLong("cooldown.duration"),
                Util.restoreItem(section.getConfigurationSection("cooldown-fallback-item")));
        String permission = section.getBoolean("permission.enabled") ? section.getString("permission.node") : null;
        ItemStack permissionFallback = Util.restoreItem(section.getConfigurationSection("permission.fallback-item"));
        return new PluginItem(id, itemStack, actionMap, cooldown, permission, permissionFallback);
    }

    public void openEditor(Player p, boolean clone, int page) {
        final NexusMenus core = NexusMenus.getInstance();
        if (core.guiEditors.containsKey(this.title) && core.guiEditors.get(this.title) != p.getUniqueId()) {
            p.sendMessage(Util.translate("editing.double-editing"));
            return;
        }
        core.guiEditors.put(getName(), p.getUniqueId());
        Gui gui;
        if (type == GUIType.CHEST) {
            gui = Gui.gui()
                    .rows(rows)
                    .title(Util.componentify(title))
                    .disableAllInteractions()
                    .create();
        } else {
            gui = Gui.gui()
                    .type(this.type.guiType)
                    .title(Util.componentify(title))
                    .disableAllInteractions()
                    .create();
        }

        try {
            gui.getFiller().fill(ItemBuilder.skull()
                    .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19")
                    .name(Util.componentify(clone ? Util.translate("editing.paste-item") : Util.translate("editing.add-item")))
                    .asGuiItem(event -> {
                        int slot = getType() == GUIType.CHEST ? event.getSlot() + ((page - 1) * getRows() * 9) : event.getSlot();
                        if (clone) {
                            PluginItem item = core.cloneMap.remove(event.getWhoClicked().getUniqueId());
                            itemsMap.put(slot, item);
                            save();
                            PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(getName());
                            pGui1.openEditor(p, false, page);
                            return;
                        }

                        new ItemCreationGui().gui(this, slot).open((Player) event.getWhoClicked());
                    }));
        } catch (IndexOutOfBoundsException ignored) {}

        int min = (page-1)*getRows()*9;
        int max = getType() == GUIType.CHEST ? (this.rows*9*page)-1 : type.guiType.getLimit();
        for (int i = min; i <= max; i++) {
            PluginItem pItem = itemsMap.get(i);
            if (pItem == null) continue;
            ItemStack item = pItem.getItem().clone();
            int finalI = i;
            gui.setItem(i-(rows*(page-1)*9), ItemBuilder.from(item)
                    .lore(Util.componentify(Util.translate("editing.click-edit")), ConfigManager.getMidClone() ? Util.componentify(Util.translate("editing.click-clone")) : null, Util.componentify(Util.translate("editing.click-delete")))
                    .asGuiItem(event -> {
                        if (event.isLeftClick()) {
                            pItem.openEditor(p, this, finalI);
                        } else if (event.getClick() == ClickType.MIDDLE) {
                            core.cloneMap.put(event.getWhoClicked().getUniqueId(), pItem);
                            openEditor(p, true, page);
                        } else {
                            itemsMap.remove(finalI);
                            save();
                            PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(getName());
                            pGui1.openEditor(p, false, page);
                        }
                    }));
        }
        /*for (Map.Entry<Integer, PluginItem> entry : itemsMap.entrySet()) {
            if (entry.getKey() > max) break;
            ItemStack item = entry.getValue().getItem().clone();
            gui.setItem(entry.getKey()-(rows*(page-1)*9), ItemBuilder.from(item)
                    .lore(Util.componentify(Util.translate("editing.click-edit")), ConfigManager.getMidClone() ? Util.componentify(Util.translate("editing.click-clone")) : null, Util.componentify(Util.translate("editing.click-delete")))
                    .asGuiItem(event -> {
                        if (event.isLeftClick()) {
                            entry.getValue().openEditor(p, this, entry.getKey());
                        } else if (event.getClick() == ClickType.MIDDLE) {
                            core.cloneMap.put(event.getWhoClicked().getUniqueId(), entry.getValue());
                            openEditor(p, true, page);
                        } else {
                            itemsMap.remove(entry.getKey());
                            save();
                            openEditor(p, false, page);
                        }
                    }));
        }*/

        if (page > 1) {
            if (prevPageItem == null) {
                setPrevPageItem(ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.pages.prev-page"))).asGuiItem().getItemStack());
                save();
            }
            gui.setItem(prevPageSlot, ItemBuilder.from(getPrevPageItem()).asGuiItem(event -> openEditor(p, clone, page-1)));
        }

        if (page < pages) {
            if (nextPageItem == null) {
                setNextPageItem(ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.pages.prev-page"))).asGuiItem().getItemStack());
                save();
            }
            gui.setItem(nextPageSlot, ItemBuilder.from(getNextPageItem()).asGuiItem(event -> openEditor(p, clone, page+1)));
        }
        gui.setCloseGuiAction(event -> {
            core.guiEditors.remove(getName());
        });
        gui.open(p);
    }

    public void save() {
        saveToFile();
        NexusMenus.getInstance().guiMap.put(getName(), this);
        if (!ConfigManager.isAutoReloadMenus()) return;
        for (Map.Entry<UUID, String> entry : NexusMenus.getInstance().guiViewers.entrySet()) {
            if (entry.getValue().equals(getTitle())) {
                Player pp = Bukkit.getPlayer(entry.getKey());
                assert pp != null;
                pp.closeInventory();
                pp.sendMessage(Util.translate("management.viewing-edited"));
            }
        }
    }

    public void open(Player p, int page) {
        if (getPermission() != null && !p.hasPermission(getPermission())) {
            p.sendMessage(Util.translate("menus.no-perm"));
            return;
        }

        NexusMenus.getInstance().guiViewers.put(p.getUniqueId(), getName());
        Gui gui;
        if (type == GUIType.CHEST) {
            gui = Gui.gui()
                    .rows(rows)
                    .title(Util.componentify(Util.translate("menus.title", title, page+"", ""+(pages == 0 ? 1 : pages))))
                    .disableAllInteractions()
                    .create();
        }
        else {
            gui = Gui.gui()
                    .type(this.type.guiType)
                    .title(Util.componentify(title))
                    .disableAllInteractions()
                    .create();
        }

        if (page > 1) {
            if (prevPageItem == null) {
                setPrevPageItem(ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.pages.prev-page"))).asGuiItem().getItemStack());
                save();
            }
            gui.setItem(prevPageSlot, ItemBuilder.from(getPrevPageItem()).asGuiItem(event -> open(p, page-1)));
        }

        if (page < pages) {
            if (nextPageItem == null) {
                setNextPageItem(ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("menus.pages.next-page"))).asGuiItem().getItemStack());
                save();
            }
            gui.setItem(nextPageSlot, ItemBuilder.from(getNextPageItem()).asGuiItem(event -> open(p, page+1)));
        }

        int minSlot = getType() == GUIType.CHEST ? (page-1)*getRows()*9 : 0;
        int maxSlot = getType() == GUIType.CHEST ? page*getRows()*9 : getType().limit;
        for (int i = minSlot; i < maxSlot; i++) {
            if (itemsMap.containsKey(i)) {
                gui.setItem(i-minSlot, itemsMap.get(i).getGuiItem(p));
            }
        }
        gui.setCloseGuiAction(event -> NexusMenus.getInstance().guiViewers.remove(event.getPlayer().getUniqueId(), getName()));
        gui.open(p);
        NexusMenus.getInstance().guiViewers.put(p.getUniqueId(), getName());
    }

    public void openManager(Player p) {
        final NexusMenus core = NexusMenus.getInstance();
        if (core.guiEditors.containsKey(this.title) && core.guiEditors.get(this.title) != p.getUniqueId()) {
            p.sendMessage(Util.translate("editing.double-editing"));
            return;
        }

        core.guiEditors.put(getName(), p.getUniqueId());
        Gui gui = Gui.gui()
                .rows(5)
                .disableAllInteractions()
                .title(Component.text(Util.translate("management.title", getTitle())))
                .create();

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());

        gui.setItem(1, 5, ItemBuilder.from(type.item)
                .name(Util.componentify(getTitle())).asGuiItem());
        gui.setItem(3, 2, ItemBuilder.from(Material.NAME_TAG).name(Util.componentify(Util.translate("editing.rename"))).asGuiItem(event -> {
            AnvilGUI.Builder anvil = new AnvilGUI.Builder();
            anvil.title(Util.translate("editing.rename-anvil"));
            anvil.itemLeft(ItemBuilder.from(type.item).name(Component.text(Util.color(getTitle()))).asGuiItem().getItemStack());
            anvil.plugin(core);
            anvil.onClick((integer, stateSnapshot) -> {
                if (integer != 2) return List.of();
                core.guiMap.remove(getName());
                Bukkit.getScheduler().runTask(core, () -> {
                    core.unregisterCommand(getName());
                });
                core.deleteMenuFile(getName());
                setName(Util.sanitize(stateSnapshot.getText()));
                setTitle(Util.color(stateSnapshot.getText()));
                save();
                PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(getName());
                core.registerMenuCommand(getName(), getName());
                pGui1.openManager(p);
                return List.of();
            });
            anvil.open(p);
        }));

        gui.setItem(3, 3, ItemBuilder.from(Material.GRASS_BLOCK).name(Util.componentify(Util.translate("management.change-rows"))).lore(Util.componentify((type == GUIType.CHEST ? Util.translate("editing.click-edit") : Util.translate("management.chest-only")))).asGuiItem(event -> {
            if (type != GUIType.CHEST) return;
            new CreationGui().rowsPicker(getTitle(), rows -> {
                setRows(rows);
                save();
                PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(getName());
                pGui1.openManager(p);
            }).open(p);

        }));

        gui.setItem(3, 4, ItemBuilder.from(Material.WRITABLE_BOOK).name(Util.componentify(Util.translate("management.edit-pages"))).lore(Util.componentify((type == GUIType.CHEST ? Util.translate("editing.click-edit") : Util.translate("management.chest-only")))).asGuiItem(event -> {
            if (type != GUIType.CHEST) return;
            new PagesEditGui().gui(this, pGui -> {
                pGui.save();
                PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(getName());
                pGui1.openManager(p);
            }).open(p);
        }));

        gui.setItem(3, 5, ItemBuilder.from(Material.TIPPED_ARROW).name(Util.componentify(Util.translate("management.edit-switchers"))).lore(Util.componentify((type == GUIType.CHEST ? Util.translate("editing.click-edit") : Util.translate("management.chest-only")))).asGuiItem(event -> {
            if (type != GUIType.CHEST) return;
            new PageSwitchersEditGui().gui(this, pGui -> {
                pGui.save();
                PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(getName());
                pGui1.openManager(p);
            }).open(p);
        }));

        gui.setItem(3, 8, ItemBuilder.from(Material.BARRIER).name(Util.componentify(Util.translate("management.delete-menu"))).asGuiItem(event -> {
            core.guiMap.remove(getName());
            Bukkit.getScheduler().runTask(core, () -> {
                core.unregisterCommand(getName());
            });
            core.deleteMenuFile(getName());
            new MenusListGui().gui(1).open(p);
            UUID uuid = core.guiEditors.remove(getName());
            if (uuid != null) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    new MenusListGui().gui(1).open(player);
                    player.sendMessage(Util.translate("management.editing-deleted"));
                }
            }

            if (core.guiViewers.containsValue(getTitle())) {
                core.guiViewers.entrySet().stream().filter(entry -> entry.getValue().equals(getTitle())).forEach(entry -> {
                    core.guiViewers.remove(entry.getKey());
                    if (entry.getKey() != null) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            player.closeInventory();
                            player.sendMessage(Util.translate("management.viewing-deleted"));
                        }
                    }
                });
            }
        }));

        gui.setItem(5, 5, ItemBuilder.from(Material.ARROW).name(Util.componentify(Util.translate("editing.back"))).asGuiItem(event -> {
            new MenusListGui().gui(1).open(p);
            if (!ConfigManager.isAutoReloadMenus()) return;
            for (Map.Entry<UUID, String> entry : core.guiViewers.entrySet()) {
                if (entry.getValue().equals(getTitle())) {
                    Player pp = Bukkit.getPlayer(entry.getKey());
                    assert pp != null;
                    pp.closeInventory();
                    pp.sendMessage(Util.translate("management.viewing-edited"));
                }
            }
        }));

        gui.open(p);
    }
    
    public void saveToFile() {
        NexusMenus core = NexusMenus.getInstance();
        File file = new File(core.menusFolder, getTitle()+".yml");
        try {
            if (!file.exists()) file.createNewFile();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("name", name);
            config.set("title", Util.unSetColors(title));
            config.set("type", getType().name());
            config.set("rows", getRows());
            config.set("permission", getPermission());
            if (getType() == GUIType.CHEST) config.set("pages-count", getPages());

            for (Map.Entry<Integer, PluginItem> entry : itemsMap.entrySet()) {
                String prefix = "";
                int page = getType() == GUIType.CHEST ? (entry.getKey() / (getRows()*9)) + 1 : 1;
                if (getType() == GUIType.CHEST) prefix = "pages."+page+".";

                prefix = prefix+"items."+(getType() == GUIType.CHEST ? entry.getKey() - ((page-1)*getRows()*9) : entry.getKey())+".";

                PluginItem pItem = entry.getValue();
                config.set(prefix+"id", pItem.getId());
                Util.saveItem(config, pItem.getItem(), prefix);

                List<String> actions = new ArrayList<>();
                for (Map.Entry<String, ItemActionType> action : pItem.getActions().entrySet()) {
                    actions.add("["+action.getValue().name()+"] "+action.getKey());
                }
                config.set(prefix+"actions", actions);

                config.set(prefix+"cooldown.enabled", !pItem.getCooldown().getDisabled());
                config.set(prefix+"cooldown.one-time-use", pItem.getCooldown().getOneTime());
                config.set(prefix+"cooldown.duration", Util.formatMillisecondsToDuration(pItem.getCooldown().getTime()));
                Util.saveItem(config, pItem.getCooldown().getFallbackItem(), prefix+"cooldown.fallback-item");

                config.set(prefix+"permission.enabled", pItem.getPermission() != null);
                config.set(prefix+"permission.node", pItem.getPermission() == null ? "" : pItem.getPermission());
                Util.saveItem(config, pItem.getPermissionFallback(), prefix+"permission.fallback-item");
            }

            config.set("switchers.previous.slot", getPrevPageSlot());
            Util.saveItem(config, getPrevPageItem(), "switchers.previous.item");

            config.set("switchers.next.slot", getNextPageSlot());
            Util.saveItem(config, getNextPageItem(), "switchers.next.item");

            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
