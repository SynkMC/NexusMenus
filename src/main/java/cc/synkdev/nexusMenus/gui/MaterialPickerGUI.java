package cc.synkdev.nexusMenus.gui;

import cc.synkdev.nexusMenus.NexusMenus;
import cc.synkdev.nexusMenus.Util;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MaterialPickerGUI {
    private final NexusMenus core = NexusMenus.getInstance();
    public Gui gui(int page, String search, Runnable onBack, Consumer<Material> onSelect) {
        Gui gui = Gui.gui()
                .rows(6)
                .disableAllInteractions()
                .title(Util.componentify(Util.translate("editing.material.title")))
                .create();
        List<Material> materials = new ArrayList<>(Util.getFilteredMaterials(search));
        int max = (materials.size()+44)/45;
        gui.getFiller().fillBottom(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).asGuiItem());

        if (page > 1) {
            gui.setItem(6, 4, ItemBuilder.from(Material.ARROW)
                    .name(Util.componentify(Util.translate("menus.pages.prev-page")))
                    .asGuiItem(event -> gui(page-1, search, onBack, onSelect).open((Player) event.getWhoClicked())));
        }

        if (page < max) {
            gui.setItem(6, 6, ItemBuilder.from(Material.ARROW)
                    .name(Util.componentify(Util.translate("menus.pages.next-page")))
                    .asGuiItem(event -> gui(page+1, search, onBack, onSelect).open((Player) event.getWhoClicked())));
        }

        gui.setItem(6, 5, ItemBuilder.from(Material.BARRIER)
                .name(Util.componentify(Util.translate("editing.back")))
                .asGuiItem(event -> onBack.run()));

        List<Component> lore = new ArrayList<>();
        if (search != null) {
            lore.add(Component.text(""));
            lore.add(Util.componentify(Util.translate("editing.material.current-search", search)));
            lore.add(Component.text(Util.translate("editing.material.search-reset")));
        }
        lore.add(Component.text(""));
        lore.add(Util.componentify(Util.translate("editing.material.click-search")));
        Material sign;
        try {
            sign = Material.valueOf("SIGN");
        } catch (IllegalArgumentException e) {
            sign = Material.OAK_SIGN;
        }
        gui.setItem(6, 2, ItemBuilder.from(sign)
                .name(Util.componentify(Util.translate("editing.material.search")))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .lore(lore)
                .asGuiItem(event -> {
                    Player p = (Player) event.getWhoClicked();
                    if (event.isRightClick() && search != null) {
                        gui(1, null, onBack, onSelect).open(p);
                        return;
                    }


                    AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                    anvil.plugin(core);
                    anvil.itemLeft(new ItemStack(Material.PAPER));

                    ItemStack out = new ItemStack(Material.PAPER);
                    ItemMeta meta = out.getItemMeta();
                    meta.setDisplayName(Util.translate("editing.material.search"));
                    out.setItemMeta(meta);
                    anvil.itemOutput(out);

                    anvil.text(Util.translate("editing.material.search-anvil"));
                    anvil.onClick((integer, stateSnapshot) -> {
                        if (integer != 2) {
                            return List.of();
                        }
                        p.closeInventory();
                        gui(1, stateSnapshot.getText(), onBack, onSelect).open(p);
                        return List.of();
                    });

                    anvil.onClose(stateSnapshot -> gui(1, stateSnapshot.getText(), onBack, onSelect).open(p));
                    anvil.open(p);
                }));

        int minSlot = 45*(page-1);
        int maxSlot = 45*page;

        for (int i = minSlot; i < maxSlot; i++) {
            if (materials.size() <= i) break;
            Material m = materials.get(i);
            try {
                gui.setItem(i - minSlot, ItemBuilder.from(m)
                        .name(Component.text(ChatColor.GOLD+m.name()))
                        .flags(ItemFlag.HIDE_ATTRIBUTES)
                        .lore(Component.text(""),
                                Util.componentify(Util.translate("editing.material.click-select")))
                        .asGuiItem(event -> onSelect.accept(m)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return gui;
    }
}
