package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

    // ------------------------------------------------------------------------------------------------------
    /**
     * A static library of common messages sent to players as they interact with the plugin.
     */
    enum Message {

        // general
        CONFIG_RELOADED(ChatColor.DARK_AQUA + "Configuration reloaded."),
        NO_PERMISSION(ChatColor.RED + "You don't have permission to do that!"),
        FEATURE_NOT_ENABLED(ChatColor.RED + "That feature is not enabled."),

        // Liquid
        SUPPORTED_LIQUIDS(ChatColor.RED + "Supported liquids: " + ChatColor.WHITE + Liquid.getSupportedTypes()),

        // player flow
        PLAYERFLOW_NOT_IN_GAME(ChatColor.RED + "You must be in-game to flow liquids!"),
        PLAYERFLOW_ON(ChatColor.DARK_AQUA + "Flow mode is " + ChatColor.YELLOW + "on"),
        PLAYERFLOW_OFF(ChatColor.DARK_AQUA + "Flow mode is " + ChatColor.YELLOW + "off"),

        // world edit + flowsel
        WORLDEDIT_MISSING(ChatColor.RED + "WorldEdit must be installed to do that!"),
        WORLDEDIT_SELECT_REGION(ChatColor.RED + "You must select a region first!"),
        WORLDEDIT_FLOWSEL_OVER_MAX(ChatColor.RED + "Your selection must be under " + SafeBuckets.CONFIG.WORLDEDIT_FLOWSEL_MAX_BLOCKS + " blocks!");

        private String _msg;

        Message(String msg) {
            _msg = msg;
        }

        void send(Player player) {
            player.sendMessage(_msg);
        }

        void send(CommandSender commandSender) {
            commandSender.sendMessage(_msg);
        }

    }

}
