package cc.synkdev.nexusMenus;

import cc.synkdev.nexusMenus.objects.GUIType;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class ConfigManager {
    @Getter private static ItemStack globalFallback;
    @Getter private static ItemStack globalCdFallback;
    @Getter private static Boolean midClone;
    @Getter private static GUIType defType;
    @Getter private static int defRows;
    @Getter private static boolean requireOp;
    @Getter private static boolean requireOpCreation;
    @Getter private static String creationPerm;
    @Getter private static boolean autoReloadMenus;
    
    public static void setup(FileConfiguration config) {
        globalFallback = Util.restoreItem(config.getConfigurationSection("global-fallback-item"));
        globalCdFallback = Util.restoreItem(config.getConfigurationSection("global-cooldown-fallback-item"));
        midClone = config.getBoolean("editing.allow-middle-click-clone");
        defType = GUIType.valueOf(config.getString("editing.default-container-type"));
        defRows = config.getInt("editing.default-rows");
        requireOp = config.getBoolean("commands.require-operator");
        requireOpCreation = config.getBoolean("permissions.require-op-for-menu-creation");
        creationPerm = config.getString("menu-creation-permission");
        autoReloadMenus = config.getBoolean("behavior.auto-reload-menus");
    }
}
