package cc.synkdev.nexusMenus;

import cc.synkdev.nexusMenus.objects.CooldownProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    static Map<UUID, CooldownProfile> cooldownMap;
    public static void setup(FileConfiguration config) {
        cooldownMap = new HashMap<>();
        for (String s : config.getKeys(false)) {
            ConfigurationSection user = config.getConfigurationSection(s);
            if (user == null) continue;

            for (String ss : user.getKeys(false)) {
                ConfigurationSection item = user.getConfigurationSection(ss);
                if (item == null) continue;

                if (item.contains("used")) {
                    if (item.getBoolean("used")) {
                        addCooldown(UUID.fromString(s), ss, 0);
                    }
                    continue;
                }

                if (!item.contains("expires-at")) continue;
                long expiry = item.getLong("expires-at");

                addCooldown(UUID.fromString(s), ss, expiry);
            }
        }
    }

    public static void save(NexusMenus core) {
        for (Map.Entry<UUID, CooldownProfile> entry : cooldownMap.entrySet()) {
            for (Map.Entry<String, Long> entryy : entry.getValue().cooldownMap.entrySet()) {
                String field = entryy.getValue() == 0 ? "used" : "expires-at";
                Object value = entryy.getValue() == 0 ? true : entryy.getValue();
                core.cooldownConfig.set(entry.getKey()+"."+entryy.getKey()+"."+field, value);
            }
        }

        try {
            core.cooldownConfig.save(core.cooldownFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setup(core.cooldownConfig);
    }

    public static CooldownProfile getProfile(UUID uuid) {
        return cooldownMap.getOrDefault(uuid, new CooldownProfile(new HashMap<>()));
    }

    public static void addCooldown(UUID uuid, String id, long time) {
        CooldownProfile profile = getProfile(uuid);
        profile.cooldownMap.put(id, time);
        cooldownMap.put(uuid, profile);
    }

    public static void removeCooldown(UUID uuid, String id) {
        CooldownProfile profile = getProfile(uuid);
        profile.cooldownMap.remove(id);
        cooldownMap.put(uuid, profile);
    }
    public static boolean hasCooldown(Player p, String id) {
        if (p.hasPermission("nexusmenus.cooldown.bypass")) return false;
        return hasCooldown(p.getUniqueId(), id);
    }
    public static boolean hasCooldown(UUID uuid, String id) {
        CooldownProfile profile = getProfile(uuid);
        if (!profile.cooldownMap.containsKey(id)) return false;
        long value = profile.cooldownMap.get(id);
        if (value == 0) return true;
        return System.currentTimeMillis() < value;
    }
    public static void clearCooldown(String id) {
        for (Map.Entry<UUID, CooldownProfile> entry : cooldownMap.entrySet()) {
            CooldownProfile profile = entry.getValue();
            profile.cooldownMap.remove(id);
            cooldownMap.put(entry.getKey(), profile);
        }
    }
    public static void clearCooldown(UUID uuid, String id) {
        CooldownProfile profile = cooldownMap.getOrDefault(uuid, new CooldownProfile(new HashMap<>()));
        profile.cooldownMap.remove(id);
        cooldownMap.put(uuid, profile);
    }
    public static void clearCooldown(UUID uuid) {
        cooldownMap.remove(uuid);
    }
}
