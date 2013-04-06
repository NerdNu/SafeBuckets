package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Dispenser;  //> Material because we need the getFacing method (DirectionalContainer.class)

public class SafeBucketsListener implements Listener {

    private final SafeBuckets plugin;

    SafeBucketsListener(SafeBuckets instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Material mat = event.getBlock().getType();
        if (mat == Material.STATIONARY_LAVA || mat == Material.STATIONARY_WATER) {
            if (plugin.isSafeLiquid(event.getBlock())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        Material mat = event.getItem().getType();
        if ((mat == Material.LAVA_BUCKET || mat == Material.WATER_BUCKET)) {
        	if (plugin.getConfig().getBoolean("allow.dispense")) {
	            Dispenser dispenser = (Dispenser)event.getBlock().getState().getData();
	        	Block blockDispenser = event.getBlock();
	        	Block blockDispense = blockDispenser.getRelative(dispenser.getFacing());
	            plugin.addBlockToCacheAndDB(event.getBlock().getRelative(dispenser.getFacing()));
	
	        	if (plugin.getConfig().getBoolean("debug.enabled")) {
	        		if (plugin.getConfig().getBoolean("debug.output.console")) {
	    				SafeBuckets.log.info("SafeBuckets: Dispensing liquid at (" + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + ")");
	        		}
	        		if (plugin.getConfig().getBoolean("debug.output.players")) {
		        		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
		        			if (player.hasPermission("safebuckets.debug")) {
		        				player.sendMessage("SafeBuckets: Dispensing liquid from (" + blockDispenser.getX() + ", " + blockDispenser.getY() + ", " + blockDispenser.getZ() + ") to (" + blockDispense.getX() + ", " + blockDispense.getY() + ", " + blockDispense.getZ() + ")");
		        			}
		        		}
	        		}
	        	}
        	} else {
        		event.setCancelled(true);
        	}
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block block = event.getBlock();
        //if (plugin.table.isSafeLiquid(event.getBlock())) {
        if (plugin.isSafeLiquid(event.getBlock())) {
            //somehow our block got changed to flowing, change it back
            if (block.getType() == Material.WATER) {
                block.setTypeId(9, false);
            }
            if (block.getType() == Material.LAVA) {
                block.setTypeId(11, false);
            }

            event.setCancelled(true);
            return;
        }

        if (plugin.isSafeLiquid(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    
    // Stop all ice melting, putting every melted ice block in the database would very quickly fill it to excessive sizes
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getType() == Material.ICE) {
            event.setCancelled(true);
        } 
   }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.ICE) {
            if (!event.getPlayer().getItemInHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
                // If we are breaking the block with an enchanted pick then don't replace it with air, we want it to drop as an item
                //event.getBlock().setTypeId(0);
                plugin.addBlockToCacheAndDB(event.getBlock());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        // Actually lets not do that
        //// If we are replacing water then lets remove it to stop those annoying no flow areas
        //if (!event.getBlockReplacedState().getBlock().isLiquid()) {
        //	plugin.table.removeSafeLiquid(event.getBlockPlaced());
        //	return;
        //}
        // Someone is using liquid to replace this block, staff making it flow
        if (block.isLiquid() || (!block.isLiquid() && plugin.isSafeLiquid(block))) {
            plugin.removeSafeLiquidFromCacheAndDB(block);
            //plugin.table.removeSafeLiquid(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
//        SafeLiquid stat = new SafeLiquid();
//        stat.setWorld(block.getWorld().getName());
//        stat.setX(block.getX());
//        stat.setY(block.getY());
//        stat.setZ(block.getZ());
//        plugin.table.save(stat);

    	if (plugin.getConfig().getBoolean("allow.bucket")) {
    		plugin.addBlockToCacheAndDB(block);
    	} else {
    		event.setCancelled(true);
    	}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Material mat = event.getItemStack().getType();
        if (mat == Material.LAVA_BUCKET || mat == Material.WATER_BUCKET) {
            Block block = event.getBlockClicked().getRelative(event.getBlockFace());
            plugin.removeSafeLiquidFromCacheAndDB(block);
            //plugin.table.removeSafeLiquid(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
    	Player player = event.getPlayer();

    	if (event.isBlockInHand() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("debug.block")) && player.hasPermission("safebuckets.tools.block") && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: Safe Liquid (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ")");
    		} else {
    			player.sendMessage("SafeBuckets: Unsafe Liquid (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ")");
    		}
    		event.setCancelled(true);
    	}
    	else if (event.hasItem() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("debug.item")) && player.hasPermission("safebuckets.tools.item") && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: Safe Liquid (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ")");
    		} else {
    			player.sendMessage("SafeBuckets: Unsafe Liquid (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ")");
    		}
    		event.setCancelled(true);
    	}
    }
}
