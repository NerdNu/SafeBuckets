package nu.nerd.SafeBuckets;

import me.sothatsit.usefulsnippets.EnchantGlow;
import org.bukkit.Bukkit;
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

        if (Liquid.isSupportedType(material)) { // filled buckets
            if (CONFIG.DISPENSERS_ENABLED) {
                if (CONFIG.DISPENSERS_SAFE && SafeBuckets.isSafe(event.getBlock())) {
                    SafeBuckets.setSafe(dispensed, true);
                }
            } else {
                event.setCancelled(true);
            }
        } else if (material == Material.BUCKET) { // empty buckets
            if (Liquid.isSupportedType(dispensed.getType())) {
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

    /*
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldEdit(EditSessionEvent event) {
        System.out.println("edit session");
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
                System.out.println("caught");
                Material newMaterial = BukkitAdapter.adapt(block.getBlockType());
                if (Liquid.isSupportedType(newMaterial) && event.getWorld() != null) {
                    World world = BukkitAdapter.adapt(event.getWorld());
                    Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
                        Block newBlock = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                        SafeBuckets.setSafe(newBlock, true);
                        System.out.println("Set safe after WE: " + newBlock);
                    }, 1);
                }
                return super.setBlock(location, block);
            }
        });
    }*/

    // ------------------------------------------------------------------------------------------------------
    /**
     * Prevents ice from melting.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (CONFIG.PREVENT_ICE_MELT && event.getBlock().getType() == Material.ICE) {
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
        Block block = event.getBlock();
        FrozenLiquid frozenLiquid = FrozenLiquid.getType(block);
        if (frozenLiquid != null && frozenLiquid.meltsWhenBroken()) {
            if (!event.getPlayer().getEquipment().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
                // make the ice block safe to prevent flowing immediately upon breaking
                SafeBuckets.setSafe(block, true);

                // wait one tick and then make the new water block safe for persistence, otherwise
                // BlockStore will "forget" the ice (since it stops existing) and the water will
                // flow if it recieves a block update after the next restart
                Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
                    Block meltedBlock = block.getWorld().getBlockAt(block.getLocation());
                    SafeBuckets.setSafe(meltedBlock, true);
                }, 1);
            }
        } else if (block.getType() == Material.DISPENSER) {
            SafeBuckets.removeSafe(block);
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
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        if (CONFIG.BUCKETS_ENABLED) {
            if (CONFIG.BUCKETS_SAFE) {
                Liquid liquid = Liquid.getType(new ItemStack(event.getBucket()));
                ItemStack itemInHand = player.getEquipment().getItemInMainHand().clone();
                if (liquid != null && EnchantGlow.hasGlow(itemInHand)) { // staff flow
                    // flow
                    SafeBuckets.setSafe(block, false);

                    // replenish
                    final ItemStack bucket = liquid.getBucket(false);
                    Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN,
                                                       () -> player.getEquipment().setItemInMainHand(bucket),
                                                       1);
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
        Material material = event.getItemStack().getType();
        if (Liquid.isSupportedType(material)) {
            Block block = event.getBlockClicked().getRelative(event.getBlockFace());
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

        boolean isSafe = SafeBuckets.isSafe(block);
        boolean isToggle = event.getAction() == Action.LEFT_CLICK_BLOCK;
        if (isToggle) {
            isSafe = !isSafe;
            SafeBuckets.setSafe(block, isSafe);
        }

        String msg = new StringBuilder().append(ChatColor.DARK_AQUA)
                                        .append("SafeBuckets ")
                                        .append(isToggle ? "toggle" : "query")
                                        .append(": ")
                                        .append(Util.formatCoords(block.getLocation()))
                                        .append(" is ")
                                        .append(isToggle ? "now " : " ")
                                        .append(ChatColor.YELLOW)
                                        .append(isSafe ? "safe" : "unsafe")
                                        .append(ChatColor.DARK_AQUA)
                                        .append(".")
                                        .toString();

        event.getPlayer().sendMessage(msg);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Handles players attempting to flow their own liquid.
     */
    private void handlePlayerFlow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock().getRelative(event.getBlockFace());
        Liquid liquid = Liquid.getType(block);
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
        if (liquid != null && isSafe) {
            event.setCancelled(true);
            if (SafeBuckets.isPlayerFlowPermitted(player, block)) {
                SafeBuckets.setSafe(block, false);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(new StringBuilder().append(ChatColor.DARK_AQUA)
                                                      .append("Flowed ")
                                                      .append(block.getType().toString().toLowerCase())
                                                      .append(" at ")
                                                      .append(Util.formatCoords(block.getLocation()))
                                                      .toString());
            } else {
                player.sendMessage(new StringBuilder().append(ChatColor.RED)
                                                      .append("You can only flow liquids in regions you ")
                                                      .append(CONFIG.PLAYER_SELF_FLOW_MODE == PlayerFlowMode.OWNER ? "own" : "are a member of")
                                                      .append("!")
                                                      .toString());
            }
        }
    } // handlePlayerFlow

} // SafeBucketsListener