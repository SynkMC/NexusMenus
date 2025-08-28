package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.ConfigManager;
import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import cc.synkdev.nexusMenus.objects.GUIType;
import cc.synkdev.nexusMenus.objects.PluginGui;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CreationGui {
    private final NexusMenus core = NexusMenus.getInstance();
    public Gui typePicker(String name) {
        Gui gui = Gui.gui().type(GuiType.HOPPER)
                .title(Util.componentify(Util.translate("editing.creation.type-title", name)))
                .disableAllInteractions()
                .create();

        AtomicBoolean picked = new AtomicBoolean(false);

        Map<Integer, GUIType> types = Map.of(0, GUIType.CHEST, 1, GUIType.HOPPER, 3, GUIType.DISPENSER, 4, GUIType.BREWING);
        for (Map.Entry<Integer, GUIType> entry : types.entrySet()) {
            gui.setItem(entry.getKey(), ItemBuilder.from(entry.getValue().item)
                    .name(Util.componentify("&r&e"+entry.getValue().name()))
                    .asGuiItem(event -> {
                        if (entry.getValue() == GUIType.CHEST) {
                            picked.set(true);
                            rowsPicker(name).open((Player) event.getWhoClicked());
                        } else {
                            picked.set(true);
                            core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), 0, entry.getValue()));
                            core.registerMenuCommand(Util.sanitize(name), Util.sanitize(name));
                            new MenusListGui().gui(1).open((Player) event.getWhoClicked());
                        }
                    }));
        }
        gui.setItem(2, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());

        gui.setCloseGuiAction(event -> {
            if (picked.get()) return;
            GUIType type = ConfigManager.getDefType();
            if (type == GUIType.CHEST) {
                rowsPicker(name).open((Player) event.getPlayer());
            } else {
                core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), 0, type));
                            core.registerMenuCommand(Util.sanitize(name), Util.sanitize(name));
                new MenusListGui().gui(1).open((Player) event.getPlayer());
            }
        });
        return gui;
    }

    public Gui rowsPicker(String name) {
        Gui gui = Gui.gui()
                .rows(1)
                .title(Util.componentify(Util.translate("editing.creation.rows-title", name)))
                .disableAllInteractions()
                .create();

        AtomicBoolean picked = new AtomicBoolean(false);
        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());

        for (int i = 1; i <= 6; i++) {
            int slot = i > 3 ? i+1 : i;
            int finalI = i;
            gui.setItem(slot, ItemBuilder.from(Material.PAPER)
                    .name(Util.componentify(Util.translate("editing.creation.x-rows", i+"")))
                    .asGuiItem(event -> {
                        picked.set(true);
                        core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), finalI, GUIType.CHEST));
                        core.registerMenuCommand(Util.sanitize(name), Util.sanitize(name));
                        new MenusListGui().gui(1).open((Player) event.getWhoClicked());
                    }));
        }

        gui.setCloseGuiAction(event -> {
            if (picked.get()) return;
            core.guiMap.put(Util.sanitize(name), new PluginGui(Util.sanitize(name), Util.color(name), ConfigManager.getDefRows(), GUIType.CHEST));
            core.registerMenuCommand(Util.sanitize(name), Util.sanitize(name));
            new MenusListGui().gui(1).open((Player) event.getPlayer());
        });
        return gui;
    }
    public Gui rowsPicker(String name, Consumer<Integer> callback) {
        AtomicBoolean picked = new AtomicBoolean(false);
        Gui gui = Gui.gui()
                .rows(1)
                .title(Util.componentify(Util.translate("editing.creation.rows-title", name)))
                .disableAllInteractions()
                .create();

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());

        for (int i = 1; i <= 6; i++) {
            int slot = i > 3 ? i+1 : i;
            int finalI = i;
            gui.setItem(slot, ItemBuilder.from(Material.PAPER)
                    .name(Util.componentify("&e"+i+" rows"))
                    .asGuiItem(event -> {
                        picked.set(true);
                        callback.accept(finalI);
                    }));
        }

        gui.setCloseGuiAction(event -> {
            if (!picked.get()) callback.accept(ConfigManager.getDefRows());
        });
        return gui;
    }
}
