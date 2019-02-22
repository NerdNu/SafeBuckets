package nu.nerd.SafeBuckets;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import me.sothatsit.usefulsnippets.EnchantGlow;
import net.sothatsit.blockstore.BlockStoreApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

// ------------------------------------------------------------------------
/**
 * The main plugin class.
 */
public class SafeBuckets extends JavaPlugin {

    // ------------------------------------------------------------------------
    /**
     * This plugin.
     */
    static SafeBuckets PLUGIN;

    // ------------------------------------------------------------------------
    /**
     * This plugin's configuration.
     */
    static Configuration CONFIG;

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable().
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        CONFIG = new Configuration();

        new Commands();
        new PlayerFlowCache();
        new SafeBucketsListener();

        // if WorldEdit is enabled in config, try to find the plugin
        if (CONFIG.WORLDEDIT_HOOK) {
            Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
            _worldEditEnabled = plugin instanceof WorldEditPlugin;
            if (!_worldEditEnabled) {
                log("WorldEdit compatibility was enabled in config.yml but the WorldEdit plugin could not be found.");
            }
        }

        // try to find WorldGuard
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        _worldGuardEnabled = plugin instanceof WorldGuardPlugin;
        if (!_worldGuardEnabled) {
            log("The WorldGuard plugin could not be found. Player flow will be disabled.");
        }

        // create glow enchantment for compatibility with ModMode item serialization
        EnchantGlow.getGlow();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the current version of SafeBuckets.
     *
     * @return the current version of SafeBuckets.
     */
    public static String getVersion() {
        return PLUGIN.getClass().getPackage().getImplementationVersion();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the given block is safe. This method consults an internal
     * cache for a result before calling the BlockStoreAPI if none is found.
     *
     * @param block the block.
     * @return true if the given block is safe.
     */
    static boolean isSafe(Block block) {
        if (CACHE.contains(block.getLocation())) {
            sendDebugMessage("Safety query: " + Util.formatCoords(block.getLocation()) + " is §aSAFE");
            return true;
        } else {
            Object o = BlockStoreApi.getBlockMeta(block, SafeBuckets.PLUGIN, METADATA_KEY);
            if (o == null || !((boolean) o)) {
                sendDebugMessage("Safety query: " + Util.formatCoords(block.getLocation()) + " is §cUNSAFE");
                return false;
            } else {
                CACHE.add(block.getLocation());
                sendDebugMessage("Safety query: " + Util.formatCoords(block.getLocation()) + " is §aSAFE");
                return true;
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Sets the safety status of a given block, updating the block's status both
     * in the internal cache and BlockStore.
     *
     * @param block the block.
     * @param state the safety status (true = safe).
     */
    static void setSafe(Block block, boolean state) {
        Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
            if (state) {
                sendDebugMessage("Safety change: " + Util.formatCoords(block.getLocation()) + " has been made §aSAFE");
                CACHE.add(block.getLocation());
            } else {
                sendDebugMessage("Safety change: " + Util.formatCoords(block.getLocation()) + " has been made §cUNSAFE");
                CACHE.remove(block.getLocation());
            }

            // check if there's an entry before spawning particles
            if (CONFIG.SHOW_PARTICLES && BlockStoreApi.getBlockMeta(block, PLUGIN, METADATA_KEY) != null) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, () -> Util.showParticles(block, state), 1);
            }

            BlockStoreApi.setBlockMeta(block, PLUGIN, METADATA_KEY, state);

            if (!state) {
                Util.forceBlockUpdate(block);
            }
        }, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Removes the safety status of the given block from both cache and BlockStore.
     * Removal is not necessarily synonymous with setSafe(*, false): the effects
     * are the same but the false safety status implies intention while no status
     * implies mundaneness.
     *
     * @param block the block.
     */
    static void removeSafe(Block block) {
        CACHE.remove(block.getLocation());
        BlockStoreApi.removeBlockMeta(block, PLUGIN, METADATA_KEY);
        // check if there's an entry before spawning particles
        if (BlockStoreApi.getBlockMeta(block, PLUGIN, METADATA_KEY) != null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, () -> Util.showParticles(block, false), 1);
        }
        sendDebugMessage("Safety change: " + Util.formatCoords(block.getLocation()) + " has been made §aSAFE");
    }

    // ------------------------------------------------------------------------
    /**
     * Determines if a given ItemStack matches an unsafe bucket.
     *
     * @param item the ItemStack.
     * @return true if the given ItemStack matches an unsafe bucket.
     */
    static boolean isUnsafeBucket(ItemStack item) {
        return CONFIG.BUCKETS.contains(item.getType()) && EnchantGlow.hasGlow(item);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns an ItemStack corresponding to the safe or unsafe version of the
     * given material.
     *
     * @param liquidContainer the bucket.
     * @param safe whether or not the bucket should be safe.
     * @return an unsafe bucket as an ItemStack.
     */
    static ItemStack getBucket(Material liquidContainer, boolean safe) {
        ItemStack bucket = new ItemStack(liquidContainer);
        if (safe) {
            return bucket;
        }
        String liquidName = "Water";
        if (liquidContainer.equals(Material.LAVA_BUCKET)) {
            liquidName = "Lava";
        }

        ItemMeta meta = bucket.getItemMeta();
        meta.setDisplayName("Unsafe " + liquidName + " Bucket");
        bucket.setItemMeta(meta);
        EnchantGlow.addGlow(bucket);

        return bucket;
    }

    // ------------------------------------------------------------------------
    /**
     * Determines if a player can flow a given block.
     *
     * @apiNote Requires WorldGuard.
     *
     * @param player the player.
     * @param block the block.
     * @return true if the player can flow the given block.
     */
    static boolean isPlayerFlowPermitted(Player player, Block block) {
        if (!_worldGuardEnabled) {
            return false;
        }

        com.sk89q.worldedit.world.World wrappedWorld = BukkitAdapter.adapt(block.getWorld());
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wrappedWorld);
        LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (regions != null) {
            Location loc = block.getLocation();
            BlockVector3 wrappedVector = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            ApplicableRegionSet applicable = regions.getApplicableRegions(wrappedVector);

            switch (CONFIG.PLAYER_SELF_FLOW_MODE) {
                case OWNER:
                    return applicable.isOwnerOfAll(wgPlayer) && applicable.size() > 0;

                case MEMBER:
                    return applicable.isMemberOfAll(wgPlayer) && applicable.size() > 0;
            }

        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * A logging method used instead of {@link java.util.logging.Logger} to
     * faciliate prefix coloring.
     *
     * @param msg the message to log.
     */
    static void log(String msg) {
        System.out.println(PREFIX + msg);
    }

    // ------------------------------------------------------------------------
    /**
     * Sends the message to the given player and logs it (and the player's name)
     * to console.
     *
     * @param player the player.
     * @param msg the message to log.
     */
    static void messageAndLog(Player player, String msg) {
        System.out.println(PREFIX + msg);
        player.sendMessage(PREFIX + "[sent to: " + player.getName() + "] " +  msg);
    }

    // ------------------------------------------------------------------------
    /**
     * Sends a debug message to players with the safebuckets.debug permission.
     *
     * @param msg the debug message to be sent.
     */
    static void sendDebugMessage(String msg) {
        if (!CONFIG.DEBUG) {
            return;
        }
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("safebuckets.debug")) {
                player.sendMessage(PREFIX + msg);
            }
        });
    }

    // ------------------------------------------------------------------------
    /**
     * This plugin's prefix as a string; for logging.
     */
    private static final String PREFIX = ChatColor.WHITE + "[" + ChatColor.AQUA + "SafeBuckets" + ChatColor.WHITE + "] ";

    // ------------------------------------------------------------------------
    /**
     * The BlockStore metadata key as a static final String for persistence.
     */
    private static final String METADATA_KEY = "safebuckets";

    // ------------------------------------------------------------------------
    /**
     * The block safety cache.
     */
    private static final HashSet<Location> CACHE = new HashSet<>();

    // ------------------------------------------------------------------------
    /**
     * Whether or not WorldEdit is enabled.
     */
    static boolean _worldEditEnabled;

    // ------------------------------------------------------------------------
    /**
     * Whether or not WorldGuard is enabled.
     */
    static boolean _worldGuardEnabled;

}