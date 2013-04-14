package nu.nerd.SafeBuckets;

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
    	if (event.getBlock().getState().getData() instanceof Dispenser) {
	        Material mat = event.getItem().getType();
	        Dispenser dispenser = (Dispenser)event.getBlock().getState().getData();
	    	Block blockDispenser = event.getBlock();
	    	Block blockDispense = blockDispenser.getRelative(dispenser.getFacing());

	        if ((mat == Material.LAVA_BUCKET || mat == Material.WATER_BUCKET)) {
	        	if (plugin.getConfig().getBoolean("dispenser.enabled")) {
		        	if (plugin.getConfig().getBoolean("bucket.safe") && plugin.isSafeLiquid(blockDispenser))
		        		plugin.addBlockToCacheAndDB(blockDispense);

		        	String message = "SafeBuckets: Dispensing (" + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + ") ";
		        	if (!plugin.isSafeLiquid(blockDispenser))
		        		message += "un";
		        	message += "safe";

	        		if (plugin.getConfig().getBoolean("debug.players")) {
	        			plugin.getServer().broadcast(message, "safebuckets.debug");
	        		}

	        		if (plugin.getConfig().getBoolean("debug.console")) {
	        			SafeBuckets.log.info(message);
	        		}
	        	} else {
	        		event.setCancelled(true);
	        	}
	        }
		}
    	else if (event.getBlock().getType() == Material.WATER || event.getBlock().getType() == Material.STATIONARY_WATER || event.getBlock().getType() == Material.LAVA || event.getBlock().getType() == Material.STATIONARY_LAVA) {
        	if (plugin.isSafeLiquid(event.getBlock())) {
                plugin.removeSafeLiquidFromCacheAndDB(event.getBlock());
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
        Block block = event.getBlock();
        if (block.getType() == Material.ICE) {
            if (!event.getPlayer().getItemInHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
                // If we are breaking the block with an enchanted pick then don't replace it with air, we want it to drop as an item
                //event.getBlock().setTypeId(0);
                plugin.addBlockToCacheAndDB(block);
            }
        }
        else if (block.getType() == Material.DISPENSER) {
            plugin.removeSafeLiquidFromCacheAndDB(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        // Someone is using liquid to replace this block, staff making it flow
        if (block.isLiquid() || (!block.isLiquid() && plugin.isSafeLiquid(block))) {
            plugin.removeSafeLiquidFromCacheAndDB(block);
        }
        else if (block.getType() == Material.DISPENSER) {
            plugin.addBlockToCacheAndDB(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

    	if (plugin.getConfig().getBoolean("bucket.enabled")) {
        	if (plugin.getConfig().getBoolean("bucket.safe")) {
        		plugin.addBlockToCacheAndDB(block);
        	}
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
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
    	Player player = event.getPlayer();

    	if (event.isBlockInHand() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.block")) && player.hasPermission("safebuckets.tools.block") && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") safe");
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") unsafe");
    		}
    		event.setCancelled(true);
    	}
    	else if (event.isBlockInHand() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.block")) && player.hasPermission("safebuckets.tools.block") && event.getAction() == Action.LEFT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") removed safe");
                plugin.removeSafeLiquidFromCacheAndDB(block);
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") set safe");
                plugin.addBlockToCacheAndDB(block);
    		}
    		event.setCancelled(true);
    	}
    	else if (event.hasItem() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.item")) && player.hasPermission("safebuckets.tools.item") && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") safe");
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") unsafe");
    		}
    		event.setCancelled(true);
    	}
    	else if (event.hasItem() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.item")) && player.hasPermission("safebuckets.tools.item") && event.getAction() == Action.LEFT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") removed safe");
                plugin.removeSafeLiquidFromCacheAndDB(block);
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") set safe");
                plugin.addBlockToCacheAndDB(block);
    		}
    		event.setCancelled(true);
    	}
    }
}
