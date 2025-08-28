package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import cc.synkdev.nexusMenus.objects.PluginGui;
import cc.synkdev.nexusMenus.objects.PluginItem;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemCreationGui {
    public Gui gui(PluginGui pGui, int slot) {
        Gui gui = Gui.gui()
                .rows(3)
                .title(Util.componentify(Util.translate("editing.item-creation.title")))
                .disableAllInteractions()
                .create();

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());
        gui.setItem(2, 3, ItemBuilder.from(Material.PAPER).name(Util.componentify(Util.translate("editing.item-creation.create-scratch"))).asGuiItem(event -> {
            new MaterialPickerGUI().gui(1, null, () -> pGui.openEditor((Player) event.getWhoClicked(), false, 1), material -> {
                PluginItem item = new PluginItem(new ItemStack(material, 1), pGui);
                pGui.itemsMap.put(slot, item);
                pGui.save();
                PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
                item.openEditor((Player) event.getWhoClicked(), pGui1, slot);
            }).open((Player) event.getWhoClicked());
        }));
        gui.setItem(2, 7, ItemBuilder.from(Material.CHEST).name(Util.componentify(Util.translate("editing.item-creation.copy-item"))).asGuiItem());

        gui.setPlayerInventoryAction(event -> {
            PluginItem item = new PluginItem(event.getCurrentItem(), pGui);
            pGui.itemsMap.put(slot, item);
            pGui.save();
            PluginGui pGui1 = NexusMenus.getInstance().guiMap.get(pGui.getName());
            item.openEditor((Player) event.getWhoClicked(), pGui1, slot);
        });
        return gui;
    }
}
