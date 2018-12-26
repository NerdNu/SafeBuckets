package nu.nerd.SafeBuckets;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Predicate;

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

    // ------------------------------------------------------------------------
    /**
     * Returns true if the given block is waterlogged.
     *
     * @param block the block.
     * @return true if the given block is waterlogged.
     */
    static boolean isWaterlogged(Block block) {
        return block.getBlockData() instanceof Waterlogged && ((Waterlogged) block.getBlockData()).isWaterlogged();
    }

    // ------------------------------------------------------------------------
    /**
     * Plays a sound at the player's location. Used as an auditory confirmation
     * of a successful flow.
     *
     * @param player the player.
     */
    static void playFlowSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------------------
    /**
     * Displays colored particles around the given block to visually indicate the
     * new safety state.
     *
     * @param block the block.
     * @param state the new state.
     */
    static void showParticles(Block block, boolean state) {
        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
        boolean isLiquid = block.getType() == Material.WATER; // lava is liquid but not transparent
        Particle.DustOptions color = (state) ? new Particle.DustOptions(Color.LIME, 1)
                                         : new Particle.DustOptions(Color.RED, 1);
        if (isLiquid) {
            // center only
            block.getWorld().spawnParticle(Particle.REDSTONE, blockCenter, 10, color);
        } else {
            // edges too
            for (Vector offset : CORNER_VECTORS) {
                block.getWorld().spawnParticle(Particle.REDSTONE, block.getLocation().add(offset), 5, color);
            }
        }
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
        Block adjacentBlock = ADJACENT_BLOCK_FACES.stream()
                                                  .map(block::getRelative)
                                                  .filter(IS_AIR)
                                                  .findAny()
                                                  .orElse(block.getRelative(BlockFace.NORTH));
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

    // ------------------------------------------------------------------------
    /**
     * A predicate to test if a block is an air block. As of this writing, the
     * Material.VOID_AIR material is only used for air blocks in the interval
     *
     *                         { y : y < 0 || y > 255 }.
     */
    private static final Predicate<Block> IS_AIR = (block) -> block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR;

    // ------------------------------------------------------------------------
    /**
     * A set of relative vectors corresponding to the eight corners of a block.
     */
    private static final HashSet<Vector> CORNER_VECTORS = new HashSet<>(Arrays.asList(
        new Vector(1,0,0),
        new Vector(0,0,1),
        new Vector(1,0,1),
        new Vector(0,0,0),

        new Vector(1,1,0),
        new Vector(0,1,1),
        new Vector(1,1,1),
        new Vector(0,1,0)
    ));

}
