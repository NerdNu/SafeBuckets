package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// ------------------------------------------------------------------------
/**
 * Utilities class.
 */
class Util {

    // ------------------------------------------------------------------------
    /**
     * The number of ticks per second on a Minecraft server.
     */
    static final int TPS = 20;

    static void playFlowSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------------------
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

    // ------------------------------------------------------------------------
    /**
     * Forces a block update on a given block. Necessary since, for some reason,
     * the BlockState#update method with applyPhysics=true does not allow
     * waterlogged blocks to flow the same way regular water/lava blocks do.
     *
     * @param block the block.
     */
    static void forceBlockUpdate(Block block) {
        Set<Block> adjacentAirBlocks = ADJACENT_BLOCK_FACES.stream()
                                                           .map(block::getRelative)
                                                           .filter(b -> b.getType() == Material.AIR)
                                                           .collect(Collectors.toSet());
        Block adjacentBlock;
        if (!adjacentAirBlocks.isEmpty()) {
            adjacentBlock = adjacentAirBlocks.stream().findAny().get();
        } else {
            adjacentBlock = block.getRelative(BlockFace.NORTH);
        }
        BlockState currentState = adjacentBlock.getState();
        adjacentBlock.setType(Material.VOID_AIR);
        Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> currentState.update(true, true), 1);
    }

    // ------------------------------------------------------------------------
    /**
     * A set of BlockFaces directly adjacent to an abstract block.
     */
    private static final HashSet<BlockFace> ADJACENT_BLOCK_FACES = new HashSet<>(Arrays.asList(
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    ));

}
