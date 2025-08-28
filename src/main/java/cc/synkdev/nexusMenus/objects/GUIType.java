package cc.synkdev.nexusMenus.objects;

import dev.triumphteam.gui.components.GuiType;
import org.bukkit.Material;

public enum GUIType {
    CHEST(Material.CHEST, GuiType.CHEST, 9),
    CRAFT(Material.CRAFTING_TABLE, GuiType.WORKBENCH, 9),
    HOPPER(Material.HOPPER, GuiType.HOPPER, 5),
    DISPENSER(Material.DISPENSER, GuiType.DISPENSER, 9),
    BREWING(Material.BREWING_STAND, GuiType.BREWING, 5);
    public Material item;
    public GuiType guiType;
    public int limit;
    GUIType(Material item, GuiType guiType, int limit) {
        this.item = item;
        this.guiType = guiType;
        this.limit = limit;
    }
}
