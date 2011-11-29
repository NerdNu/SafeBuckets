package nu.nerd.SafeBuckets;

import java.util.TreeSet;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

public class SafeBucketsWorldListener extends WorldListener {
    private final SafeBuckets plugin;

    SafeBucketsWorldListener(SafeBuckets instance) {
        plugin = instance;
    }

    @Override
    public void onWorldLoad(WorldLoadEvent event) {
        String name = event.getWorld().getName();
        if (!plugin.bucketBlocks.containsKey(name)) {
            plugin.bucketBlocks.put(name, new TreeSet<Long>());
            plugin.saveSet();
        }
    }
}
