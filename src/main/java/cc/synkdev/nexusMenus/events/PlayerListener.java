package cc.synkdev.nexusMenus.events;

import cc.synkdev.nexusMenus.NexusMenus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final NexusMenus core = NexusMenus.getInstance();
    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (core.guiEditors.containsValue(event.getPlayer().getUniqueId())) {
            core.guiEditors.entrySet().stream().filter(entry -> entry.getValue().equals(event.getPlayer().getUniqueId())).forEach(entry -> core.guiEditors.remove(entry.getKey()));
        }

        core.guiViewers.remove(event.getPlayer().getUniqueId());
    }
}
