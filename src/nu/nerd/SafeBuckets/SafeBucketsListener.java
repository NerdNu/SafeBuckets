package nu.nerd.SafeBuckets;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import me.sothatsit.usefulsnippets.EnchantGlow;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dispenser;
import org.bukkit.plugin.java.JavaPlugin;

import static nu.nerd.SafeBuckets.SafeBuckets.CONFIG;

// ----------------------------------------------------------------------------------------------------------
/**
 * The event-handling class. See {@link Listener}.
 */
public class SafeBucketsListener implements Listener {

    // ------------------------------------------------------------------------------------------------------
    /**
     * Constructor. Called once during {@link JavaPlugin#onEnable()}.
     */
    SafeBucketsListener() {
        Bukkit.getPluginManager().registerEvents(this, SafeBuckets.PLUGIN);
        WorldEdit.getInstance().getEventBus().register(this);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Handles dispensers.
     *
     * When a dispenser fires there exist two cases (excluding no action) which are handled: when the item is
     *      (i) a filled bucket -- the resulting liquid block will be made safe; or
     *      (ii) an empty bucket -- the bucket will take the liquid block (if present) and make that location
     *                              unsafe once again.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        Material material = event.getItem().getType();
        Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();
        Block dispensed = event.getBlock().getRelative(dispenser.getFacing());

        if (Configuration.BUCKETS.contains(material)) { // filled bucket being dumped
            if (CONFIG.DISPENSERS_ENABLED) {
                if (CONFIG.DISPENSERS_SAFE && SafeBuckets.isSafe(event.getBlock())) {
                    SafeBuckets.setSafe(dispensed, true);
                }
            } else {
                event.setCancelled(true);
            }
        } else if (material == Material.BUCKET) { // empty bucket picking up liquid block
            if (Configuration.LIQUID_BLOCKS.contains(dispensed.getType())) {
                if (CONFIG.DISPENSERS_ENABLED) {
                    SafeBuckets.removeSafe(dispensed);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Prevent liquids from flowing.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (SafeBuckets.isSafe(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Prevents WorldEdited liquids from flowing.
     */
    @Subscribe
    public void onWorldEdit(EditSessionEvent event) {
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public boolean setBlock(Vector loc, BlockStateHolder block) throws WorldEditException {
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

    // ------------------------------------------------------------------------------------------------------
    /**
     * Prevents ice from melting.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (CONFIG.PREVENT_ICE_MELT && event.getNewState().getType() == Material.WATER) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * When a block of ice is broken the water that replaces it will be made safe, unless the player
     * is holding a tool enchanted with Silk Touch (in which case no water appears). When a dispenser is
     * broken, remove its safety status.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != null) {
            ItemStack mainHand = event.getPlayer().getEquipment().getItemInMainHand();
            if (mainHand.containsEnchantment(Enchantment.SILK_TOUCH)) {
                return;
            }
        }

        Block block = event.getBlock();
        Material material = block.getType();
        if (material == Material.DISPENSER) {
            SafeBuckets.removeSafe(block);
        } else if (Configuration.MELTABLE_ICE.contains(material)) {
            SafeBuckets.setSafe(block, true);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Automatically makes placed dispensers safe.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (CONFIG.DISPENSERS_ENABLED) {
            if (CONFIG.DISPENSERS_SAFE) {
                Block block = event.getBlockPlaced();
                if (block.getType() == Material.DISPENSER) {
                    SafeBuckets.setSafe(block, true);
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Handles liquids being placed by bucket. Includes logic for staff flowing.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        final ItemStack mainHand = player.getEquipment().getItemInMainHand().clone();

        Block block = Util.getRelevantBlock(event);

        if (CONFIG.BUCKETS_ENABLED) {
            if (CONFIG.BUCKETS_SAFE) {
                if (EnchantGlow.hasGlow(mainHand)) { // staff: flow & replenish bucket
                    SafeBuckets.setSafe(block, false);
                    Util.forceBlockUpdate(block);
                    Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> player.getEquipment().setItemInMainHand(mainHand), 1);
                } else {
                    SafeBuckets.setSafe(block, true);
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * When a liquid is picked up via a bucket, remove that block's safe designation.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Block block = Util.getRelevantBlock(event);
        if (SafeBuckets.canBeFlowed(block)) {
            SafeBuckets.removeSafe(block);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Handle players querying the safety of blocks and players trying to self-flow.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.hasItem() && event.getItem().getType() == CONFIG.TOOL_ITEM) {
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

    // ------------------------------------------------------------------------------------------------------
    /**
     * Handles tool-based actions: safety status querying, and safety status toggling.
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

    // ------------------------------------------------------------------------------------------------------
    /**
     * Handles players attempting to flow their own liquid.
     */
    private void handlePlayerFlow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = Util.getRelevantBlock(event);
        boolean isSafe = SafeBuckets.isSafe(block);

        // allow players to eat while in flow mode
        if (event.hasItem() && event.getItem().getType().isEdible()) {
            return;
        }

        // allow players to place blocks while in flow mode
        if (event.hasBlock() && !player.getEquipment().getItemInMainHand().getType().equals(Material.AIR)
                             && !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        // if the liquid is a supported type and is safe, it is eligible to be flowed
        if (SafeBuckets.canBeFlowed(block) && isSafe) {
            event.setCancelled(true);
            if (SafeBuckets.isPlayerFlowPermitted(player, block)) {
                SafeBuckets.setSafe(block, false);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(ChatColor.DARK_AQUA + "Flowed " + block.getType().toString() + " at " + Util.formatCoords(block.getLocation()));
            } else {
                player.sendMessage(ChatColor.RED + "You can only flow liquids in regions you " + (CONFIG.PLAYER_SELF_FLOW_MODE == PlayerFlowMode.OWNER ? "own" : "are a member of") + "!");
            }
        }

    } // handlePlayerFlow

} // SafeBucketsListener