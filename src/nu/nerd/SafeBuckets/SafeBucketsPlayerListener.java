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
        Long hash = Util.GetHashCode(block.getX(), block.getY(), block.getZ());

        plugin.bucketBlocks.get(block.getWorld().getName()).add(hash);
        plugin.saveSet();
    }
}
