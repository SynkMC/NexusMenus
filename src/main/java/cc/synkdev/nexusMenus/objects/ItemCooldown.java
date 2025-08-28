package cc.synkdev.nexusMenus.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Getter @Setter @AllArgsConstructor
public class ItemCooldown {
    private Boolean disabled;
    private Boolean oneTime;
    private Long time;
    private ItemStack fallbackItem;
}

