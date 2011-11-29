package nu.nerd.SafeBuckets;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockFromToEvent;

public class SafeBucketsBlockListener extends BlockListener
{
    private SafeBuckets plugin;

    SafeBucketsBlockListener(SafeBuckets instance)
    {
        plugin = instance;
    }

    @Override
    public void onBlockPhysics(BlockPhysicsEvent event)
    {
        if (event.isCancelled())
            return;

        Material mat = event.getBlock().getType();
        if (mat == Material.STATIONARY_LAVA || mat == Material.STATIONARY_WATER) {
            if (plugin.bucketBlocks.contains(event.getBlock().hashCode())) {
                event.setCancelled(true);
            }
        }
    }
    
    @Override
    public void onBlockFromTo(BlockFromToEvent event)
    {
        if (event.isCancelled())
            return;

        Block block = event.getBlock();

        if (plugin.bucketBlocks.contains(block.hashCode())) {
            //somehow our blocks got changed to flowing, change them back
            if (block.getType() == Material.WATER)
                block.setTypeId(9, false);
            if (block.getType() == Material.LAVA)
                block.setTypeId(11, false);

            event.setCancelled(true);
        }

        if (plugin.bucketBlocks.contains(event.getToBlock().hashCode())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (event.isCancelled())
            return;

        //if someone placed a block that destroyed our liquid, stop tracking it
        plugin.bucketBlocks.remove(event.getBlockPlaced().hashCode());
        plugin.saveSet();
    }
}
