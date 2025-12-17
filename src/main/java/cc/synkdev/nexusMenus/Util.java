package cc.synkdev.nexusMenus;

import cc.synkdev.nexusCore.bukkit.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    public static Component componentify(String s) {
        return Component.text(ChatColor.RESET+color(s));
    }

    public static List<Material> getFilteredMaterials() {
        List<Material> result = new ArrayList<>();
        try {
            for (Field field : Material.class.getDeclaredFields()) {
                if (field.getName().contains("AIR")) continue;

                if (field.isEnumConstant() && !field.isAnnotationPresent(Deprecated.class)) {
                    Material value = Material.valueOf(field.getName());
                    result.add(value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
    public static List<Material> getFilteredMaterials(String search) {
        if (search == null) return getFilteredMaterials();
        List<Material> result = new ArrayList<>();
        try {
            for (Field field : Material.class.getDeclaredFields()) {
                if (field.getName().contains("AIR")) continue;

                if (field.isEnumConstant() && !field.isAnnotationPresent(Deprecated.class) && field.getName().contains(search.toUpperCase())) {
                    Material value = Material.valueOf(field.getName());
                    result.add(value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
    public static void log(String msg) {
        Bukkit.getConsoleSender().sendMessage(color(msg));
    }
    public static long parseDurationToMillieconds(String input) throws NumberFormatException {
        Map<String, Long> timeUnits = Map.of(
                "s", 1000L,
                "m", 60000L,
                "h", 3600000L,
                "d", 86400000L,
                "w", 604800000L,
                "mo", 2592000000L,
                "y", 31536000000L
        );

        Matcher matcher = Pattern.compile("(?i)(\\d+)([a-z]+)").matcher(input.trim());

        if (matcher.matches()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            Long multiplier = timeUnits.get(unit);
            if (multiplier != null) {
                return value * multiplier;
            } else {
                throw new IllegalArgumentException("Unknown time unit: " + unit);
            }
        } else {
            long ret;
            try {
                ret = Long.parseLong(input);
                return ret;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid time string", e);
            }
        }
    }

    public static String formatMillisecondsToDuration(long millis) {
        LinkedHashMap<String, Long> timeUnits = new LinkedHashMap<>();
        timeUnits.put("y", 31536000000L);
        timeUnits.put("mo", 2592000000L);
        timeUnits.put("w", 604800000L);
        timeUnits.put("d", 86400000L);
        timeUnits.put("h", 3600000L);
        timeUnits.put("m", 60000L);
        timeUnits.put("s", 1000L);

        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, Long> entry : timeUnits.entrySet()) {
            String unit = entry.getKey();
            long unitMillis = entry.getValue();

            if (millis >= unitMillis) {
                long count = millis / unitMillis;
                millis %= unitMillis;
                result.append(count).append(unit);
            }
        }

        if (result.length() == 0) {
            result.append("0s");
        }

        return result.toString();
    }

    public static String formatTimestamp(long timestampInSeconds) {
        long timestampInMilliseconds = timestampInSeconds * 1000;
        Date date = new Date(timestampInMilliseconds);

        SimpleDateFormat sdf = new SimpleDateFormat(NexusMenus.getInstance().getDateFormat());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }

    public static FileConfiguration saveItem(FileConfiguration config, ItemStack item, String path) {
        if (item == null) return config;

        config.set(path+".material", item.getType().name());
        config.set(path+".amount", item.getAmount());
        boolean meta = item.getItemMeta() != null;
        config.set(path+".name", unSetColors(meta ? item.getItemMeta().getDisplayName() : ""));

        List<String> lore = new ArrayList<>();
        if (meta && item.getItemMeta().getLore() != null) {
            for (String s : item.getItemMeta().getLore()) {
                lore.add(unSetColors(s));
            }
        }
        config.set(path+".lore", lore);
        Object model;
        if (!meta) model = "";
        else if (!item.getItemMeta().hasCustomModelData() && !item.getItemMeta().hasItemModel()) {
            model = "";
        } else {
            model = !item.getItemMeta().hasItemModel() ? (item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : "") : item.getItemMeta().getItemModel().getNamespace()+":"+item.getItemMeta().getItemModel().getKey();
        }
        config.set(path+".model-data", model);
        return config;
    }

    public static ItemStack restoreItem(ConfigurationSection section) {
        if (section == null) return null;
        ItemStack item;
        Material material = Material.valueOf(section.getString("material").toUpperCase());
        String name = setColors(section.getString("name"));
        List<String> lore = new ArrayList<>();
        for (String s : section.getStringList("lore")) {
            lore.add(setColors(s));
        }
        Object mData = section.get("model-data");
        item = new ItemStack(material);
        item.setAmount(section.contains("amount") ? section.getInt("amount") : 1);
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(material);
        try {
            meta.setDisplayName(name);
            meta.setLore(lore);
            if (mData instanceof String) {
                if (!((String) mData).isEmpty())
                    meta.setItemModel(new NamespacedKey(((String) mData).split(":")[0], ((String) mData).split(":")[1]));
            } else if (mData instanceof Integer) {
                meta.setCustomModelData((Integer) mData);
            }
        } catch (NullPointerException ignored) {

        }
        item.setItemMeta(meta);
        return item;
    }

    public static String setColors(String s) {
        Map<String, ChatColor> colorMap = new HashMap<>(Map.of(
                "&0", ChatColor.BLACK,
                "&1", ChatColor.DARK_BLUE,
                "&2", ChatColor.DARK_GREEN,
                "&3", ChatColor.DARK_AQUA,
                "&4", ChatColor.DARK_RED,
                "&5", ChatColor.DARK_PURPLE,
                "&6", ChatColor.GOLD,
                "&7", ChatColor.GRAY,
                "&8", ChatColor.DARK_GRAY
        ));
        colorMap.putAll(Map.of(
                "&9", ChatColor.BLUE,
                "&a", ChatColor.GREEN,
                "&b", ChatColor.AQUA,
                "&c", ChatColor.RED,
                "&d", ChatColor.LIGHT_PURPLE,
                "&e", ChatColor.YELLOW,
                "&f", ChatColor.WHITE,
                "&k", ChatColor.MAGIC,
                "&l", ChatColor.BOLD,
                "&m", ChatColor.STRIKETHROUGH));
        colorMap.putAll(Map.of(
                "&n", ChatColor.UNDERLINE,
                "&o", ChatColor.ITALIC,
                "&r", ChatColor.RESET));

        for (Map.Entry<String, ChatColor> entry : colorMap.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue()+"");
        }
        return s;
    }
    public static String unSetColors(String s) {
        Map<String, ChatColor> colorMap = new HashMap<>(Map.of(
                "&0", ChatColor.BLACK,
                "&1", ChatColor.DARK_BLUE,
                "&2", ChatColor.DARK_GREEN,
                "&3", ChatColor.DARK_AQUA,
                "&4", ChatColor.DARK_RED,
                "&5", ChatColor.DARK_PURPLE,
                "&6", ChatColor.GOLD,
                "&7", ChatColor.GRAY,
                "&8", ChatColor.DARK_GRAY
        ));
        colorMap.putAll(Map.of(
                "&9", ChatColor.BLUE,
                "&a", ChatColor.GREEN,
                "&b", ChatColor.AQUA,
                "&c", ChatColor.RED,
                "&d", ChatColor.LIGHT_PURPLE,
                "&e", ChatColor.YELLOW,
                "&f", ChatColor.WHITE,
                "&k", ChatColor.MAGIC,
                "&l", ChatColor.BOLD,
                "&m", ChatColor.STRIKETHROUGH));
        colorMap.putAll(Map.of(
                "&n", ChatColor.UNDERLINE,
                "&o", ChatColor.ITALIC,
                "&r", ChatColor.RESET));

        for (Map.Entry<String, ChatColor> entry : colorMap.entrySet()) {
            s = s.replaceAll(entry.getValue()+"", entry.getKey());
        }
        return s;
    }

    public static String sanitize(String s) {
        for (ChatColor value : ChatColor.values()) {
            s = s.replaceAll("&"+value.getChar(), "");
        }
        s = s.replaceAll(" ", "_");
        return s;
    }

    public static String translate(String key, String... args) {
        return Lang.translate(key, NexusMenus.getInstance(), args);
    }
}

