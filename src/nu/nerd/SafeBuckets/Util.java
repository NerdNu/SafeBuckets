package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<Block> adjacentAirBlocks = ADJACENT_BLOCK_FACES.stream()
                                                           .map(block::getRelative)
                                                           .filter(b -> b.getType() == Material.AIR)
                                                           .collect(Collectors.toSet());
        Block adjacentBlock;
        if (!adjacentAirBlocks.isEmpty()) {
            adjacentBlock = adjacentAirBlocks.stream().findAny().get();
            BlockData blockData = adjacentBlock.getBlockData();
            adjacentBlock.setType(Material.BARRIER);
            Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> adjacentBlock.setBlockData(blockData), 1);
        } else {
            // really should never happen since a block needs at least one adjacent air block in order
            // to be clicked
            adjacentBlock = block.getRelative(BlockFace.NORTH);
            BlockData blockData = adjacentBlock.getBlockData(); // save block information
            adjacentBlock.setType(Material.BARRIER);
            adjacentBlock.getState().update();
            adjacentBlock.setBlockData(blockData);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the more relevant block in a PlayerBucketEvent. A waterloggable block is always more relevant
     * than the relative block.
     *
     * @param event the PlayerBucketEvent.
     * @return the more relevant block.
     */
    static Block getRelevantBlock(PlayerBucketEvent event) {
        Block clickedBlock = event.getBlockClicked();
        if (clickedBlock.getBlockData() instanceof Waterlogged) {
            return clickedBlock;
        } else {
            return clickedBlock.getRelative(event.getBlockFace());
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the more relevant block in a PlayerInteractEvent. A waterloggable block is always more relevant
     * than the relative block.
     *
     * @param event the PlayerInteractEvent.
     * @return the more relevant block.
     */
    static Block getRelevantBlock(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getBlockData() instanceof Waterlogged) {
            return clickedBlock;
        } else {
            return clickedBlock.getRelative(event.getBlockFace());
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * A set of BlockFaces directly adjacent to an abstract block.
     */
    private static final HashSet<BlockFace> ADJACENT_BLOCK_FACES = new HashSet<>(Arrays.asList(
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    ));

}
