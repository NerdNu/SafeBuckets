package nu.nerd.SafeBuckets;

import java.util.ArrayList;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled())
            return;

        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        plugin.bucketBlocks.add(block.hashCode());
        plugin.saveSet();
    }
}
