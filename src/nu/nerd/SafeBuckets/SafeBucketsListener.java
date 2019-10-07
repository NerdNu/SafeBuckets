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
import org.bukkit.Tag;
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
        Block adjacentBlock = event.getBlock().getRelative(dispenser.getFacing());

        if (SafeBuckets.CONFIG.BUCKETS.contains(material)) { // filled bucket being dumped
            if (SafeBuckets.CONFIG.DISPENSERS_ENABLED) {
                if (SafeBuckets.CONFIG.DISPENSERS_SAFE && SafeBuckets.isSafe(event.getBlock())) {
                    SafeBuckets.setSafe(adjacentBlock, true);
                }
            } else {
                event.setCancelled(true);
            }
        } else if (material == Material.BUCKET) { // empty bucket picking up liquid block or a waterloggable
            if (Util.isWaterlogged(adjacentBlock) || SafeBuckets.CONFIG.LIQUID_BLOCKS.contains(adjacentBlock.getType())) {
                if (SafeBuckets.CONFIG.DISPENSERS_ENABLED) {
                    SafeBuckets.removeSafe(adjacentBlock);
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
                if (SafeBuckets.CONFIG.LIQUID_BLOCKS.contains(newMaterial) && event.getWorld() != null) {
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
     * Prevents ice (except frosted ice) from melting.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getNewState().getType() == Material.WATER) {
            if (SafeBuckets.CONFIG.PREVENT_ICE_MELT && event.getBlock().getType() != Material.FROSTED_ICE) {
                event.setCancelled(true);
            } else {
                SafeBuckets.setSafe(event.getBlock(), true);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handles a few things. If the broken block is...
     *
     *  (i) a dispenser, the block's safety information is removed from BlockStore;
     *  (ii) kelp, the block's material will change from kelp to water and thus
     *       needs to be made safe again;
     *  (iii) generally waterloggable, it will be made safe since the water block
     *        will usually persist (e.g. broken fence);
     *  (iv) meltable ice, the resulting water block will be made safe, unless
     *       the player is using a silk touch tool or the block does not actually
     *       turn into water.
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

        if (blockType == Material.KELP || blockType == Material.KELP_PLANT) {
            Location location = block.getLocation();
            Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
                SafeBuckets.setSafe(block.getWorld().getBlockAt(location), true);
            }, 1);
        }

        // handle waterloggables
        if (Util.isWaterlogged(block)) {
            SafeBuckets.setSafe(block, true);
            return;
        }

        // meltable ice broken
        if (SafeBuckets.CONFIG.MELTABLE_ICE.contains(blockType)) {
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
     * Handles a few things. If the placed block is...
     *
     *  (i) kelp, the height limit will be checked, and the resulting block will
     *      be made safe if it is not kelp-limited;
     *  (ii) seagrass, the block(s) will be made safe;
     *  (iii) a dispenser, the block will be made safe;
     *  (iv) water or lava, the block will not change safety state since the game
     *       will not stop the block from flowing;
     *  (v) generally waterloggable, it will be made safe.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        Material placedType = event.getBlockPlaced().getType();
        Material replacedType = event.getBlockReplacedState().getType();

        switch (placedType) {
            case KELP_PLANT:
            case KELP:
                if (Util.limitKelpHeight(placedBlock)) {
                    event.setCancelled(true);
                } else {
                    SafeBuckets.setSafe(placedBlock, true);
                }
                break;

            case SEAGRASS:
                SafeBuckets.setSafe(placedBlock, true);
                break;

            case TALL_SEAGRASS:
                SafeBuckets.setSafe(placedBlock, true);
                SafeBuckets.setSafe(placedBlock.getRelative(BlockFace.UP), true);
                break;

            case DISPENSER:
                if (!SafeBuckets.CONFIG.DISPENSERS_ENABLED) {
                    event.setCancelled(true);
                } else if (SafeBuckets.CONFIG.DISPENSERS_SAFE) {
                    SafeBuckets.setSafe(event.getBlockPlaced(), true);
                }
                break;

            default:
                if (replacedType == Material.WATER || replacedType == Material.LAVA) {
                    if (!Util.isWaterlogged(placedBlock) && placedType != Material.WATER
                                                         && placedType != Material.LAVA) {
                        SafeBuckets.removeSafe(placedBlock);
                        return;
                    }
                }
                Block block = event.getBlock();
                if (Util.isWaterlogged(block)) {
                    SafeBuckets.setSafe(block, true);
                }
                break;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handles liquids being placed by bucket. Includes logic for staff flowing.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!SafeBuckets.CONFIG.BUCKETS_ENABLED) {
            event.setCancelled(true);
            return;
        } else if (!SafeBuckets.CONFIG.BUCKETS_SAFE) {
            return;
        }

        Player player = event.getPlayer();
        final ItemStack mainHand = player.getEquipment().getItemInMainHand().clone();

        Block clickedBlock = event.getBlockClicked();
        Block relativeBlock = clickedBlock.getRelative(event.getBlockFace());

        // not a staff member trying to flow, and buckets are set to be safe
        if (!EnchantGlow.hasGlow(mainHand)) {
            if (Util.isWaterloggable(clickedBlock) && Util.isWaterBucket(mainHand)) {
                if (!Util.isWaterlogged(clickedBlock)) {
                    SafeBuckets.setSafe(clickedBlock, true);
                } else {
                    SafeBuckets.setSafe(relativeBlock, true);
                }
            } else if (Tag.SIGNS.isTagged(relativeBlock.getType())) {
                //quick fix for bug where water placed on a sign flows
                SafeBuckets.setSafe(relativeBlock, true);
            } else if (Util.AIR_BLOCKS.contains(relativeBlock.getType())) {
                SafeBuckets.setSafe(relativeBlock, true);
            } else if (relativeBlock.getType() == Material.WATER && !SafeBuckets.isSafe(relativeBlock)) {
                // emptying a bucket on an unsafe source will not stop the flow,
                // so let's not make the block safe again (which will just then
                // make the stored state different from the actual state
                return;
            } else {
                // it is safebuckets, after all
                SafeBuckets.setSafe(relativeBlock, true);
                SafeBuckets.setSafe(clickedBlock, true);
            }
            return;
        }

        // staff trying to flow
        // maybe the clicked block is waterlogged and needs to be flowed?
        if (Util.isWaterlogged(clickedBlock) && mainHand.getType() == Material.WATER_BUCKET) {
            // safe waterlogged block, flow
            SafeBuckets.setSafe(clickedBlock, false);
            Util.playFlowSound(player);
            // stop bucket from actually placing water, replenish unsafe bucket
            Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
                player.getEquipment().setItemInMainHand(mainHand);
                if (relativeBlock.getType() == Material.WATER) {
                    relativeBlock.setType(Material.AIR);
                }
            }, 1);
            return;

            // the clicked block is waterloggable, but it's either not waterlogged
            // or is already unsafe, so turn attention to the relative block instead
        }

        // relative block is water or lava
        if (relativeBlock.getType() == Material.WATER ||
            relativeBlock.getType() == Material.LAVA ||
            relativeBlock.getType() == Material.BUBBLE_COLUMN) {
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
            if (event.hasItem()) {
                if (event.getItem().getType() == SafeBuckets.CONFIG.TOOL_ITEM && player.hasPermission("safebuckets.tools.item")) {
                    useTool(event, event.getClickedBlock());
                } else if (event.getItem().getType() == SafeBuckets.CONFIG.INSPECTION_BLOCK && player.hasPermission("safebuckets.tools.block")) {
                    useBlock(event, event.getClickedBlock().getRelative(event.getBlockFace()));
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
     * Handles safety status querying with the inspector block.
     */
    private void useBlock(PlayerInteractEvent event, Block block) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        boolean isSafe = SafeBuckets.isSafe(block);
        player.sendMessage(ChatColor.DARK_AQUA + "SafeBuckets query: " + Util.formatCoords(block.getLocation()) + " is " + ChatColor.YELLOW + (isSafe ? "safe" : "unsafe"));
    }

    // ------------------------------------------------------------------------
    /**
     * Handles safety status querying and safety status toggling with the
     * inspector item.
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

        if (Util.isWaterlogged(clickedBlock) && SafeBuckets.isSafe(clickedBlock)) {
            if (SafeBuckets.isPlayerFlowPermitted(player, clickedBlock)) {
                SafeBuckets.setSafe(clickedBlock, false);
                SafeBuckets.messageAndLog(player, ChatColor.DARK_AQUA + "Flowed " + clickedBlock.getType().toString() + " at " + Util.formatCoords(clickedBlock.getLocation()));
                Util.playFlowSound(player);
            } else {
                player.sendMessage(ChatColor.RED + "You can only flow liquids in regions you " + (SafeBuckets.CONFIG.PLAYER_SELF_FLOW_MODE == PlayerFlowMode.OWNER ? "own" : "are a member of") + "!");
            }
            event.setCancelled(true);
            return;
        }

        if (relativeBlock.getType() == Material.WATER ||
            relativeBlock.getType() == Material.LAVA ||
            relativeBlock.getType() == Material.BUBBLE_COLUMN) {
            if (SafeBuckets.isSafe(relativeBlock)) {
                event.setCancelled(true);
                if (SafeBuckets.isPlayerFlowPermitted(player, relativeBlock)) {
                    SafeBuckets.setSafe(relativeBlock, false);
                    SafeBuckets.messageAndLog(player, ChatColor.DARK_AQUA + "Flowed " + relativeBlock.getType().toString() + " at " + Util.formatCoords(relativeBlock.getLocation()));
                    Util.playFlowSound(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You can only flow liquids in regions you " + (SafeBuckets.CONFIG.PLAYER_SELF_FLOW_MODE == PlayerFlowMode.OWNER ? "own" : "are a member of") + "!");
                }
            }
        }

    }

} // SafeBucketsListener