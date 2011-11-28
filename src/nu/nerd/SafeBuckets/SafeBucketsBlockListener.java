package nu.nerd.SafeBuckets;

import org.bukkit.Material;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockFromToEvent;

public class SafeBucketsBlockListener extends BlockListener {
    private SafeBuckets plugin;

    SafeBucketsBlockListener(SafeBuckets instance) {
        plugin = instance;
    }

    @Override
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.isCancelled())
            return;

        Material mat = event.getBlock().getType();
            if (mat == Material.STATIONARY_LAVA || mat == Material.STATIONARY_WATER) {
            if (plugin.bucketBlocks.contains(event.getBlock().hashCode())) {
                plugin.log.info("cancelling");
                //event.getBlock().getRelative(BlockFace.UP).setType(Material.BRICK);
                event.setCancelled(true);
            }
        } else if (mat == Material.LAVA || mat == Material.WATER) {
            if (plugin.bucketBlocks.contains(event.getBlock().hashCode())) {
                plugin.log.info("block at " + event.getBlock().getLocation().toString() + " turned into flowing water!");
                //event.getBlock().getRelative(BlockFace.UP).setType(Material.DIAMOND_BLOCK);
                //event.getBlock().setTypeId(event.getBlock().getTypeId() + 1);
            }
        }
    }
    
    @Override
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.isCancelled())
            return;
        
        if (plugin.bucketBlocks.contains(event.getBlock().hashCode())) {
            plugin.log.info("fromto called by " + event.getBlock().getLocation().toString());
            event.setCancelled(true);
        }
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;

        plugin.bucketBlocks.remove(event.getBlockPlaced().hashCode());
        plugin.saveSet();
    }
}
