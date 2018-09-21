package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

// ----------------------------------------------------------------------------------------------------------
/**
 * Utilities class.
 */
class Util {

    // ------------------------------------------------------------------------------------------------------
    /**
     * The number of ticks per second on a Minecraft server.
     */
    static final int TPS = 20;

    // ------------------------------------------------------------------------------------------------------
    /**
     * Formats a given location into a human-readable ordered triple.
     *
     * @param location the location.
     * @return a human-readable ordered triple as a string.
     */
    static String formatCoords(Location location) {
        return String.format("(x:%d, y:%d, z:%d)", location.getBlockX(),
                                                 location.getBlockY(),
                                                 location.getBlockZ());
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Forces a block update on a given block.
     *
     * @param block the block.
     */
    static void forceBlockUpdate(Block block) {
        Material currentType = block.getType();
        block.setType(Material.AIR);
        Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> block.setType(currentType, true), 1);
    }

}
