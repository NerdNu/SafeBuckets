package nu.nerd.SafeBuckets;

import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerListener;


public class SafeBucketsPlayerListener extends PlayerListener
{
    private final SafeBuckets plugin;

    public SafeBucketsPlayerListener(SafeBuckets instance)
    {
        plugin = instance;
    }

    @Override
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
    {
        if (event.isCancelled())
            return;

        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        
        plugin.log.info("Added hashcode: " + block.hashCode());
        plugin.bucketBlocks.add(block.hashCode());
        plugin.saveSet();
    }
}
