package nu.nerd.SafeBuckets;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import me.sothatsit.usefulsnippets.EnchantGlow;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dispenser;
import org.bukkit.plugin.java.JavaPlugin;

// ------------------------------------------------------------------------
/**
 * The event-handling class. See {@link Listener}.
 */
public class SafeBucketsListener implements Listener {

    // ------------------------------------------------------------------------
    /**
     * Constructor. Called once during {@link JavaPlugin#onEnable()}.
     */
    SafeBucketsListener() {
        Bukkit.getPluginManager().registerEvents(this, SafeBuckets.PLUGIN);
        WorldEdit.getInstance().getEventBus().register(this);
    }

    // ------------------------------------------------------------------------
    /**
     * Handles dispensers.
     *
     * When a dispenser fires there exist two cases (excluding no action) which
     * are handled: when the item is
     *      (i) a filled bucket -- the resulting liquid block will be made safe;
     *      (ii) an empty bucket -- the bucket will take the liquid block (if
     *                              present) and make that location unsafe once
     *                              again.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        Material material = event.getItem().getType();
        Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();
        Block dispensed = event.getBlock().getRelative(dispenser.getFacing());

        if (Configuration.BUCKETS.contains(material)) { // filled bucket being dumped
            if (Configuration.DISPENSERS_ENABLED) {
                if (Configuration.DISPENSERS_SAFE && SafeBuckets.isSafe(event.getBlock())) {
                    SafeBuckets.setSafe(dispensed, true);
                }
            } else {
                event.setCancelled(true);
            }
        } else if (material == Material.BUCKET) { // empty bucket picking up liquid block
            if (Configuration.LIQUID_BLOCKS.contains(dispensed.getType())) {
                if (Configuration.DISPENSERS_ENABLED) {
                    SafeBuckets.removeSafe(dispensed);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent liquids from flowing.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (SafeBuckets.isSafe(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevents WorldEdited liquids from flowing.
     */
    @Subscribe
    public void onWorldEdit(EditSessionEvent event) {
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public boolean setBlock(BlockVector3 loc, BlockStateHolder block) throws WorldEditException {
                Material newMaterial = BukkitAdapter.adapt(block.getBlockType());
                if (Configuration.LIQUID_BLOCKS.contains(newMaterial) && event.getWorld() != null) {
                    World world = BukkitAdapter.adapt(event.getWorld());
                    Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
                        Block newBlock = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                        SafeBuckets.setSafe(newBlock, true);
                    }, 1);
                }
                return super.setBlock(loc, block);
            }
        });
    }

    // ------------------------------------------------------------------------
    /**
     * Prevents ice from melting.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (Configuration.PREVENT_ICE_MELT && event.getNewState().getType() == Material.WATER) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a block of ice is broken the water that replaces it will be made safe
     * unless the player is holding a tool enchanted with Silk Touch, in which
     * case no water appears. When a dispenser is broken, remove its safety status.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();

        // broken dispensers should be immediately removed from store
        if (blockType == Material.DISPENSER) {
            SafeBuckets.removeSafe(block);
            return;
        }

        // handle waterloggables
        if (block.getBlockData() instanceof Waterlogged && ((Waterlogged)block.getBlockData()).isWaterlogged()) {
            SafeBuckets.setSafe(block, true);
        }

        // meltable ice broken
        if (Configuration.MELTABLE_ICE.contains(blockType)) {
            if (event.getPlayer() != null) {
                if (!event.getPlayer().getEquipment().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
                    // ice broken without silk touch turns into water and needs
                    // to be made safe
                    SafeBuckets.setSafe(block, true);
                }
            } else {
                // meltable ice somehow broke but player is null. check what it is
                // after a short delay
                final World world = block.getWorld();
                final Location blockLoc = block.getLocation();
                Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
                    Block blockNextTick = world.getBlockAt(blockLoc);
                    if (blockNextTick.getType() == Material.WATER) {
                        SafeBuckets.setSafe(blockNextTick, true);
                    }
                }, 1);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Automatically makes placed dispensers safe.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockReplacedState().getType() == Material.WATER || event.getBlockReplacedState().getType() == Material.LAVA) {
            if (event.getBlockPlaced().getType() != Material.WATER && event.getBlockPlaced().getType() != Material.LAVA) {
                SafeBuckets.removeSafe(event.getBlockPlaced());
            }
        }
        if (event.getBlockPlaced().getType() == Material.DISPENSER) {
            if (!Configuration.DISPENSERS_ENABLED) {
                event.setCancelled(true);
            } else {
                if (Configuration.DISPENSERS_SAFE) {
                    SafeBuckets.setSafe(event.getBlockPlaced(), true);
                }
            }
        }
        // handle waterloggables
        Block block = event.getBlock();
        if (block.getBlockData() instanceof Waterlogged && ((Waterlogged)block.getBlockData()).isWaterlogged()) {
            SafeBuckets.setSafe(block, true);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handles liquids being placed by bucket. Includes logic for staff flowing.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!Configuration.BUCKETS_ENABLED) {
            event.setCancelled(true);
            return;
        } else if (!Configuration.BUCKETS_SAFE) {
            return;
        }

        Player player = event.getPlayer();
        final ItemStack mainHand = player.getEquipment().getItemInMainHand().clone();

        Block clickedBlock = event.getBlockClicked();
        Block relativeBlock = clickedBlock.getRelative(event.getBlockFace());

        // not a staff member trying to flow, and buckets are set to be safe
        if (!EnchantGlow.hasGlow(mainHand)) {
            if (clickedBlock.getBlockData() instanceof Waterlogged && mainHand.getType() == Material.WATER_BUCKET) {
                if (!((Waterlogged) clickedBlock.getBlockData()).isWaterlogged()) {
                    SafeBuckets.setSafe(clickedBlock, true);
                }
            } else if (relativeBlock.getType() == Material.SIGN || relativeBlock.getType() == Material.WALL_SIGN) {
                //quick fix for bug where water placed on a sign flows
                SafeBuckets.setSafe(relativeBlock, true);
            } else if (relativeBlock.getType() == Material.AIR) {
                SafeBuckets.setSafe(relativeBlock, true);
            }
            return;
        }

        // staff trying to flow
        // maybe the clicked block is waterlogged and needs to be flowed?
        if (clickedBlock.getBlockData() instanceof Waterlogged && mainHand.getType() == Material.WATER_BUCKET) {
            Waterlogged waterlogged = (Waterlogged) clickedBlock.getBlockData();
            if (waterlogged.isWaterlogged()) {
                // safe waterlogged block, flow
                SafeBuckets.setSafe(clickedBlock, false);
                Util.playFlowSound(player);
                // stop bucket from actually placing water
                Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
                    if (relativeBlock.getType() == Material.WATER) {
                        relativeBlock.setType(Material.AIR);
                    }
                }, 1);
                return;
            }

            // the clicked block is waterloggable, but it's either not waterlogged
            // or is already unsafe, so turn attention to the relative block instead
        }

        // relative block is water or lava
        if (relativeBlock.getType() == Material.WATER || relativeBlock.getType() == Material.LAVA) {
            if (SafeBuckets.isSafe(relativeBlock)) {
                // if it's safe: flow, replenish, exit
                SafeBuckets.setSafe(relativeBlock, false);
                Util.playFlowSound(player);
                Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> player.getEquipment().setItemInMainHand(mainHand), 1);
            }

            // if it's unsafe then this might be a misclick, so exit
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a liquid is picked up via a bucket, remove that block's safe
     * designation.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Block blockClicked = event.getBlockClicked();
        Block relativeBlock = blockClicked.getRelative(event.getBlockFace());
        if (blockClicked instanceof Waterlogged) {
            if (((Waterlogged) blockClicked.getBlockData()).isWaterlogged()) {
                SafeBuckets.removeSafe(blockClicked);
                return;
            }
        }
        if (relativeBlock.getType() == Material.WATER || relativeBlock.getType() == Material.LAVA) {
            SafeBuckets.removeSafe(relativeBlock);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle players querying the safety of blocks and players trying to self-flow.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.hasItem() && event.getItem().getType() == Configuration.TOOL_ITEM) {
                if (player.hasPermission("safebuckets.tools.item")) {
                    useTool(event, event.getClickedBlock());
                }
            }
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                if (player.hasPermission("safebuckets.playerflow") && PlayerFlowCache.isCached(player)) {
                    handlePlayerFlow(event);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handles tool-based actions: safety status querying, and safety status
     * toggling.
     */
    private void useTool(PlayerInteractEvent event, Block block) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        boolean isSafe = SafeBuckets.isSafe(block);
        boolean isToggle = event.getAction() == Action.LEFT_CLICK_BLOCK;
        if (isToggle) {
            isSafe = !isSafe;
            SafeBuckets.setSafe(block, isSafe);
            player.sendMessage(ChatColor.DARK_AQUA + "SafeBuckets toggle: " + Util.formatCoords(block.getLocation()) + " is now " + ChatColor.YELLOW + (isSafe ? "safe" : "unsafe"));
        } else {
            player.sendMessage(ChatColor.DARK_AQUA + "SafeBuckets query: " + Util.formatCoords(block.getLocation()) + " is " + ChatColor.YELLOW + (isSafe ? "safe" : "unsafe"));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handles players attempting to flow their own liquid.
     */
    private void handlePlayerFlow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        Block relativeBlock = clickedBlock.getRelative(event.getBlockFace());

        // allow players to eat while in flow mode
        if (event.hasItem() && event.getItem().getType().isEdible()) {
            return;
        }

        // allow players to place blocks while in flow mode
        if (event.hasBlock() && !player.getEquipment().getItemInMainHand().getType().equals(Material.AIR)
                && !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        if (clickedBlock instanceof Waterlogged) {
            if (((Waterlogged) clickedBlock.getBlockData()).isWaterlogged()) {
                if (SafeBuckets.isSafe(clickedBlock)) {
                    event.setCancelled(true);
                    if (SafeBuckets.isPlayerFlowPermitted(player, clickedBlock)) {
                        SafeBuckets.setSafe(clickedBlock, false);
                        player.sendMessage(ChatColor.DARK_AQUA + "Flowed " + clickedBlock.getType().toString() + " at " + Util.formatCoords(clickedBlock.getLocation()));
                        Util.playFlowSound(player);
                        return;
                    } else {
                        player.sendMessage(ChatColor.RED + "You can only flow liquids in regions you " + (Configuration.PLAYER_SELF_FLOW_MODE == PlayerFlowMode.OWNER ? "own" : "are a member of") + "!");
                        return;
                    }
                }
            }
        }

        if (relativeBlock.getType() == Material.WATER || relativeBlock.getType() == Material.LAVA) {
            if (SafeBuckets.isSafe(relativeBlock)) {
                event.setCancelled(true);
                if (SafeBuckets.isPlayerFlowPermitted(player, relativeBlock)) {
                    SafeBuckets.setSafe(relativeBlock, false);
                    player.sendMessage(ChatColor.DARK_AQUA + "Flowed " + relativeBlock.getType().toString() + " at " + Util.formatCoords(relativeBlock.getLocation()));
                    Util.playFlowSound(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You can only flow liquids in regions you " + (Configuration.PLAYER_SELF_FLOW_MODE == PlayerFlowMode.OWNER ? "own" : "are a member of") + "!");
                }
            }
        }

    }

} // SafeBucketsListener