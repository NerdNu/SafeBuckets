package nu.nerd.SafeBuckets;

import nu.nerd.SafeBuckets.database.SafeLiquid;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class SafeBucketsListener implements Listener {
    
    private final SafeBuckets plugin;
    
    SafeBucketsListener(SafeBuckets instance) {
        plugin = instance;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event)
    {
        Material mat = event.getBlock().getType();
        if (mat == Material.STATIONARY_LAVA || mat == Material.STATIONARY_WATER) {
            if (plugin.table.isSafeLiquid(event.getBlock())) { 
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event)
    {
        Block block = event.getBlock();
        if (plugin.table.isSafeLiquid(event.getBlock())) {
            //somehow our block got changed to flowing, change it back
            if (block.getType() == Material.WATER)
                block.setTypeId(9, false);
            if (block.getType() == Material.LAVA)
                block.setTypeId(11, false);

            event.setCancelled(true);
            return;
        }

        if (plugin.table.isSafeLiquid(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        // If we are replacing water then lets remove it to stop those annoying no flow areas
    	if (!event.getBlockReplacedState().getBlock().isLiquid()) {
    		 plugin.table.removeSafeLiquid(event.getBlockPlaced());
    		 return;
    	}
    	
    	// Someone is using liquid to replace this block, staff making it flow
        if (event.getBlockPlaced().isLiquid()) {
        	plugin.table.removeSafeLiquid(event.getBlockPlaced());
   		 return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
    {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        
        SafeLiquid stat = new SafeLiquid();
		stat.setWorld(block.getWorld().getName());
		stat.setHash(Util.GetHashCode(block.getX(), block.getY(), block.getZ()));
        plugin.table.save(stat);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event)
    {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        plugin.table.removeSafeLiquid(block);
    }
}
