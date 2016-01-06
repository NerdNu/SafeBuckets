package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FlowModeDisableTask extends BukkitRunnable {

    SafeBuckets plugin;

    public FlowModeDisableTask(SafeBuckets plugin) {
        this.plugin = plugin;
    }

    public void run() {
        Iterator<Map.Entry<UUID, Date>> iterator = plugin.playerFlowTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Date> entry = iterator.next();
            Date lastFlow = entry.getValue();
            Date now = new Date();
            long diff = (now.getTime() - lastFlow.getTime()) / 1000;
            if (diff >= 300) {
                iterator.remove();
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player.hasMetadata("safebuckets.playerflow")) {
                    player.removeMetadata("safebuckets.playerflow", plugin);
                    player.sendMessage(String.format("%sFlow mode is %soff.", ChatColor.DARK_AQUA, ChatColor.YELLOW));
                }
            }
        }
    }

}
