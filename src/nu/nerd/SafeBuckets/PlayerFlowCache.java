package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

// ----------------------------------------------------------------------------------------------------------
/**
 * A singleton cache of players currently in flow mode.
 */
class PlayerFlowCache implements Listener {

    // ------------------------------------------------------------------------------------------------------
    /**
     * The cache of players currently in flow mode.
     */
    private static final HashMap<UUID, Long> _activePlayers = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------
    /**
     * Constructor, called once during {@link JavaPlugin#onEnable()}.
     */
    PlayerFlowCache() {
        Bukkit.getPluginManager().registerEvents(this, SafeBuckets.PLUGIN);
        Bukkit.getScheduler().runTaskTimer(SafeBuckets.PLUGIN,
                                           PlayerFlowCache::reviewCache,
                                           1, // delay
                                           SafeBuckets.CONFIG.PLAYER_SELF_FLOW_CACHE_REVIEW_PERIOD);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Adds a player to the cache.
     *
     * @param player the player.
     */
    static void cache(Player player) {
        SafeBuckets.log("Added to cache: " + player.getName());
        _activePlayers.put(player.getUniqueId(), System.currentTimeMillis());
        Util.Message.PLAYERFLOW_ON.send(player);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns true if the given player is cached (is in flow mode).
     *
     * @param player the player.
     * @return true if the given player is cached (is in flow mode).
     */
    static boolean isCached(Player player) {
        return _activePlayers.containsKey(player.getUniqueId());
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Forces a player's flow session to expire, removing them from the cache and flow mode.
     *
     * @param player the player.
     */
    static void forceExpire(Player player) {
        SafeBuckets.log("Forcing cache to expire for: " + player.getName());
        _activePlayers.remove(player.getUniqueId());
        Util.Message.PLAYERFLOW_OFF.send(player);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Reviews the cache to determine if any players' flow sessions have expired.
     */
    private static void reviewCache() {
        SafeBuckets.log("Reviewing cache: " + _activePlayers.keySet().toString());
        _activePlayers.keySet().removeIf(PlayerFlowCache::willExpire);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns true if the given player's flow session should expire.
     *
     * @param uuid the player's UUID.
     * @return true if the given player's flow session should expire.
     */
    private static boolean willExpire(UUID uuid) {
        long expirationTimestamp = _activePlayers.get(uuid) + SafeBuckets.CONFIG.PLAYER_SELF_FLOW_DURATION;
        if (expirationTimestamp - System.currentTimeMillis() < 0) {
            Player player = Bukkit.getServer().getPlayer(uuid);
            Util.Message.PLAYERFLOW_OFF.send(player);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Forces a player's flow session to expire upon logout.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        forceExpire(event.getPlayer());
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Forces a player's flow session to expire upon being kicked.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        forceExpire(event.getPlayer());
    }

}