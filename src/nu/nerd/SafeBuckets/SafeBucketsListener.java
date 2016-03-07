package nu.nerd.SafeBuckets;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import me.sothatsit.usefulsnippets.EnchantGlow;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dispenser;  //> Material because we need the getFacing method (DirectionalContainer.class)

import java.util.Date;

public class SafeBucketsListener implements Listener {

    private final SafeBuckets plugin;

    // Cache tool item and block materials since they are accessed every
    // PlayerInteractEvent.
    private final Material TOOL_ITEM_MATERIAL;
    private final Material TOOL_BLOCK_MATERIAL;
    private final boolean PLAYERFLOW_OWNER_MODE;

    public SafeBucketsListener(SafeBuckets instance) {
        plugin = instance;
        TOOL_ITEM_MATERIAL = Material.getMaterial(plugin.getConfig().getString("tool.item"));
        TOOL_BLOCK_MATERIAL = Material.getMaterial(plugin.getConfig().getString("tool.block"));
        PLAYERFLOW_OWNER_MODE = plugin.getConfig().getBoolean("playerflow.ownermode");
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
            Block blockDispensed = blockDispenser.getRelative(dispenser.getFacing());

	        if (mat == Material.LAVA_BUCKET || mat == Material.WATER_BUCKET) {
                if (plugin.getConfig().getBoolean("dispenser.enabled")) {
                    if (plugin.getConfig().getBoolean("dispenser.safe") && plugin.isSafeLiquid(blockDispenser)) {
                        plugin.addBlockToCacheAndDB(blockDispensed);
                    }

                    plugin.debug("SafeBuckets: Dispense " + Util.formatCoords(event.getBlock()) + (plugin.isSafeLiquid(event.getBlock()) ? " safe" : " unsafe"));
	        	} else {
	        		event.setCancelled(true);
	        	}
	        } else if (mat == Material.BUCKET) {
                if (blockDispensed.getType() == Material.WATER || blockDispensed.getType() == Material.STATIONARY_WATER ||
                    blockDispensed.getType() == Material.LAVA  || blockDispensed.getType() == Material.STATIONARY_LAVA) {
                    // Empty bucket taking liquid.
                    if (plugin.getConfig().getBoolean("dispenser.enabled")) {
                        // Regardless of whether the dispenser is safe or unsafe, when liquid in front of it
                        // is removed from the world, it is removed from the DB (i.e. made to flow) too.
                        plugin.removeSafeLiquidFromCacheAndDB(blockDispensed);

                        plugin.debug("SafeBuckets: Un-Dispense " + Util.formatCoords(event.getBlock()) + (plugin.isSafeLiquid(event.getBlock()) ? " safe" : " unsafe"));
                    } else {
                        event.setCancelled(true);
                    }
                }
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
				ItemStack itemInHand = event.getPlayer().getItemInHand();
				if (!EnchantGlow.hasGlow(itemInHand)) {
					plugin.addBlockToCacheAndDB(block);
				} else {
					plugin.removeSafeLiquidFromCacheAndDB(block);
					if (event.getPlayer().hasPermission("safebuckets.tools.norefill")) {
						event.setItemStack(itemInHand);
					}
				}
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().hasMetadata("safebuckets.playerflow")) {
            event.getPlayer().removeMetadata("safebuckets.playerflow", plugin);
            plugin.playerFlowTimes.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.getPlayer().hasMetadata("safebuckets.playerflow")) {
            event.getPlayer().removeMetadata("safebuckets.playerflow", plugin);
            plugin.playerFlowTimes.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.hasItem() && event.getItem().getType() == TOOL_ITEM_MATERIAL && player.hasPermission("safebuckets.tools.item")) {
                useTool(event, event.getClickedBlock());
            } else if (event.isBlockInHand() && event.getItem().getType() == TOOL_BLOCK_MATERIAL && player.hasPermission("safebuckets.tools.block")) {
                useTool(event, event.getClickedBlock().getRelative(event.getBlockFace()));
            } else if (player.hasPermission("safebuckets.playerflow") && player.hasMetadata("safebuckets.playerflow")) {
                handlePlayerFlow(event);
            }
        }
    }

    private void useTool(PlayerInteractEvent event, Block block) {
        event.setCancelled(true);

        boolean isSafe = plugin.isSafeLiquid(block);
        boolean toggleFlow = (event.getAction() == Action.LEFT_CLICK_BLOCK);
        if (toggleFlow) {
            isSafe = !isSafe;
            if (isSafe) {
                plugin.addBlockToCacheAndDB(block);
            } else {
                plugin.removeSafeLiquidFromCacheAndDB(block);
            }
        }
        String format = toggleFlow ? "&dSafeBuckets toggle: %s is now %s&d."
                                   : "&dSafeBuckets query: %s is %s&d.";
        String coords = Util.formatCoords(block);
        String safeness = isSafe ? "&asafe" : "&cunsafe";
        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', String.format(format, coords, safeness)));
    }

    private void handlePlayerFlow(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock().getRelative(event.getBlockFace());
        boolean isLiquid = block.getType().equals(Material.STATIONARY_WATER) || block.getType().equals(Material.STATIONARY_LAVA);
        boolean isSafe = plugin.isSafeLiquid(block);

        // UI niceness to enable some actions in flow mode
        if (event.hasItem() && event.getItem().getType().isEdible()) return; // Eat
        if (event.hasBlock() && !player.getItemInHand().getType().equals(Material.AIR) && !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return; // Place blocks

        if (isLiquid && isSafe) {
            event.setCancelled(true);

            if (!isPlayerFlowPermitted(player, block)) {
                String term = (PLAYERFLOW_OWNER_MODE) ? "own" : "are a member of";
                player.sendMessage(String.format("%sYou can only flow liquids in regions you %s!", ChatColor.RED, term));
                return;
            }

            plugin.removeSafeLiquidFromCacheAndDB(block);
            plugin.playerFlowTimes.put(player.getUniqueId(), new Date());
            if (block.getType() == Material.STATIONARY_WATER) {
                block.setType(Material.WATER);
            } else {
                block.setType(Material.LAVA);
            }

            String type = block.getType().toString().toLowerCase();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.sendMessage(String.format("%sFlowed %s block at %d,%d,%d.", ChatColor.DARK_AQUA, type, block.getX(), block.getY(), block.getZ()));
            plugin.getLogger().info(String.format("%s flowed %s block at %d,%d,%d.", player.getName(), type, block.getX(), block.getY(), block.getZ()));
        }

    }

    private boolean isPlayerFlowPermitted(Player player, Block block) {
        if (plugin.getWG() == null) return false;
        RegionManager regions = plugin.getWG().getRegionManager(block.getWorld());
        LocalPlayer wgPlayer = plugin.getWG().wrapPlayer(player);
        if (regions != null) {
            ApplicableRegionSet applicable = regions.getApplicableRegions(block.getLocation());
            if (PLAYERFLOW_OWNER_MODE) {
                return applicable.isOwnerOfAll(wgPlayer) && applicable.size() > 0;
            } else {
                return applicable.isMemberOfAll(wgPlayer) && applicable.size() > 0;
            }
        }
        return false;
    }

}
