package cc.synkdev.nexusMenus;

import cc.synkdev.nexusCore.bukkit.Analytics;
import cc.synkdev.nexusCore.bukkit.Lang;
import cc.synkdev.nexusCore.components.NexusPlugin;
import cc.synkdev.nexusMenus.commands.DynamicCommand;
import cc.synkdev.nexusMenus.commands.MainCmd;
import cc.synkdev.nexusMenus.commands.MenuCommandHandler;
import cc.synkdev.nexusMenus.events.PlayerListener;
import cc.synkdev.nexusMenus.objects.PluginGui;
import cc.synkdev.nexusMenus.objects.PluginItem;
import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.MessageKeys;
import dev.triumphteam.gui.components.GuiType;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

public final class NexusMenus extends JavaPlugin implements NexusPlugin {
    @Getter private static NexusMenus instance;
    public Map<String, PluginGui> guiMap = new HashMap<>();
    public Map<String, UUID> guiEditors = new HashMap<>();
    public Map<UUID, String> guiViewers = new HashMap<>();
    public Map<UUID, PluginItem> cloneMap = new HashMap<>();
    public File cooldownFile = new File(getDataFolder(), "cooldowns.yml");
    public File configFile = new File(getDataFolder(), "config.yml");
    public File messagesFile = new File(getDataFolder(), "messages.yml");
    public File menusFolder = new File(getDataFolder(), "menus");
    public FileConfiguration cooldownConfig;
    @Getter public FileConfiguration config;
    @Getter private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    public List<String> commandsWaitList = new ArrayList<>();
    public List<String> commandsWaitDelList = new ArrayList<>();
    public Map<String, String> langMap = new HashMap<>();
    private final File langFile = new File(getDataFolder(), "lang.json");

    @Override
    public void onEnable() {
        instance = this;


        new Metrics(this, 27067);
        Analytics.registerSpl(this);

        updateConfig();
        ConfigManager.setup(config);

        reloadLang();

        initCooldown();
        CooldownManager.setup(cooldownConfig);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
        BukkitCommandManager bcm = new BukkitCommandManager(this);
        bcm.registerCommand(new MainCmd());
        bcm.getCommandCompletions().registerCompletion("types", h -> Arrays.stream(GuiType.values())
                .map(GuiType::name)
                .toList());
        bcm.usePerIssuerLocale(false);
        bcm.getLocales().addMessage(bcm.getLocales().getDefaultLocale(), MessageKeys.PERMISSION_DENIED, Lang.translate("commands.no-perm", this));
        bcm.getLocales().addMessage(bcm.getLocales().getDefaultLocale(), MessageKeys.NOT_ALLOWED_ON_CONSOLE, Lang.translate("commands.console-error", this));
        bcm.getLocales().addMessage(bcm.getLocales().getDefaultLocale(), MessageKeys.UNKNOWN_COMMAND, Lang.translate("commands.not-exist", this));

        initFolder();
        loadMenus();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (commandsWaitList.isEmpty()) {
                if (commandsWaitDelList.isEmpty()) return;
                String command = commandsWaitDelList.get(0);
                commandsWaitDelList.remove(0);
                unregisterCommand(command);
                return;
            }
            String command = commandsWaitList.get(0);
            commandsWaitList.remove(0);
            registerCommand(command);
        }, 1L, 1L);
    }

    public void loadMenus() {
        guiMap.clear();
        for (File file : menusFolder.listFiles()) {
            String name = file.getName().replaceAll(".yml", "");
            guiMap.put(name, new PluginGui(file));
            registerMenuCommand(name, name);
        }
    }

    public void reloadLang() {
        langMap.clear();
        langMap.putAll(cc.synkdev.nexusCore.bukkit.Lang.init(this, langFile));
    }

    private void updateConfig() {
        if (!this.getDataFolder().exists()) this.getDataFolder().mkdirs();
        try {
            if (!configFile.exists()) {
                try {
                    Files.copy(getResource("config.yml"), configFile.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                File temp = new File(getDataFolder(), "temp-config-"+System.currentTimeMillis()+".yml");
                try {
                    Files.copy(getResource("config.yml"), temp.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                FileConfiguration tempConfig = YamlConfiguration.loadConfiguration(temp);
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                boolean changed = false;
                for (String key : tempConfig.getKeys(true)) {
                    if (!config.contains(key)) {
                        config.set(key, tempConfig.get(key));
                        changed = true;
                    }
                }

                if (changed) {
                    config.save(configFile);
                }

                temp.delete();
            }
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void deleteMenuFile(String name) {
        new File(menusFolder, name + ".yml").delete();
    }

    private void initFolder() {
        if (!menusFolder.exists()) menusFolder.mkdir();
    }

    private void initCooldown() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();

        try {
            if (!cooldownFile.exists()) cooldownFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cooldownConfig = YamlConfiguration.loadConfiguration(cooldownFile);
    }

    public void registerMenuCommand(String commandName, String menuName) {
        commandsWaitList.add(commandName);
    }

    private void registerCommand(String commandName) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            DynamicCommand command = new DynamicCommand(commandName, new MenuCommandHandler(commandName));
            command.setDescription("Opens the '" + commandName + "' menu.");
            commandMap.register(getDescription().getName(), command);

            final Server server = Bukkit.getServer();
            final Method syncCommandsMethod = server.getClass().getDeclaredMethod("syncCommands");
            syncCommandsMethod.setAccessible(true);
            syncCommandsMethod.invoke(server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unregisterCommand(String command) {
        commandsWaitDelList.add(command);
    }

    private void unregisterMenuCommand(String commandName) {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            Command command = commandMap.getCommand(commandName);
            if (command != null) {
                command.unregister(commandMap);
            }

            Method sync = Bukkit.getServer().getClass().getDeclaredMethod("syncCommands");
            sync.setAccessible(true);
            sync.invoke(Bukkit.getServer());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    @Override
    public void onDisable() {
        CooldownManager.save(this);
        guiMap.values().forEach(PluginGui::saveToFile);
    }

    @Override
    public String name() {
        return "NexusMenus";
    }

    @Override
    public String ver() {
        return "1.0.1";
    }

    @Override
    public String dlLink() {
        return "https://modrinth.com/plugin/nexusmenus";
    }

    @Override
    public String prefix() {
        return Util.translate("prefix");
    }

    @Override
    public String lang() {
        return "https://synkdev.cc/storage/translations/lang-pld/NexusMenus/lang-nm.json";
    }

    @Override
    public Map<String, String> langMap() {
        return langMap;
    }
}
