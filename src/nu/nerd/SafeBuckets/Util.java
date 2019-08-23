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
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
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
     * A set of the different types of air blocks.
     */
    static final HashSet<Material> AIR_BLOCKS = new HashSet<>(Arrays.asList(
        Material.AIR, Material.CAVE_AIR, Material.VOID_AIR
    ));

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
     * Return true if the given block is able to be waterlogged. Some blocks,
     * such as a double slab, are not actually able to be waterlogged (i.e.
     * clicking with a filled bucket places the water against a double-slab)
     * but still implement Waterlogged.
     *
     * @param block the block.
     * @return true if the block is able to be waterlogged.
     */
    static boolean isWaterloggable(Block block) {
        if (block.getBlockData() instanceof Slab) {
            if (((Slab) block.getBlockData()).getType() == Slab.Type.DOUBLE) {
                return false;
            }
        }
        return block.getBlockData() instanceof Waterlogged;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the given ItemStack is capable of placing water in the
     * world.
     *
     * @param itemStack the item.
     * @return true if the item is capable of placing water in the world.
     */
    static boolean isWaterBucket(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        Material bucketType = itemStack.getType();
        if (bucketType == Material.LAVA_BUCKET || bucketType == Material.MILK_BUCKET) {
            return false;
        }
        return SafeBuckets.CONFIG.BUCKETS.contains(bucketType) || bucketType.toString().contains("_BUCKET");
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
        Particle.DustOptions color = new Particle.DustOptions(state ? Color.LIME : Color.RED, 1);
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
        return String.format("(x:%d, y:%d, z:%d)",
            location.getBlockX(),
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
        Block updateBlock = null;
        for (BlockFace blockFace : ADJACENT_BLOCK_FACES) {
            Block adjBlock = block.getRelative(blockFace);
            Material adjType = adjBlock.getType();
            if (adjBlock.getState() instanceof InventoryHolder) {
                continue;
            }
            if (IS_AIR.test(adjBlock) || adjType.isOccluding() || adjBlock.isLiquid()) {
                updateBlock = adjBlock;
                break;
            }
        }
        if (updateBlock == null) {
            // fail gracefully
            SafeBuckets.log("Failed to force a block update at " + Util.formatCoords(block.getLocation())+ ": no suitable adjacent blocks.");
            return;
        }
        BlockState currentState = updateBlock.getState();
        updateBlock.setType(Material.VOID_AIR);
        final Block updatedBlock = updateBlock;
        Bukkit.getScheduler().runTaskLater(SafeBuckets.PLUGIN, () -> {
            updatedBlock.setBlockData(currentState.getBlockData());
            currentState.update(true, true);
        }, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Checks if a kelp plant has reached its maximum height. This method patches
     * what is essentially a vanilla exploit that allows players to make kelp
     * towers taller than the intended maximum of 26.
     *
     * @param kelp the newly-placed kelp.
     * @return true if the kelp tower should be limited; false if not.
     */
    static boolean limitKelpHeight(Block kelp) {
        Block nextBlock = kelp;
        Material nextBlockType = kelp.getRelative(BlockFace.DOWN).getType();
        int n = 0; // the placed block is not included in the count
        while (nextBlockType == Material.KELP || nextBlockType == Material.KELP_PLANT) {
            nextBlock = nextBlock.getRelative(BlockFace.DOWN);
            nextBlockType = nextBlock.getType();
            n++;
        }
        return n >= 26;
    }

    /**
     * A set of BlockFaces directly adjacent to an abstract block.
     */
    public static final HashSet<BlockFace> ADJACENT_BLOCK_FACES = new HashSet<>(Arrays.asList(
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    ));

    /**
     * A predicate to test if a block is an air block. As of this writing, the
     * Material.VOID_AIR material is only used for air blocks in the interval
     *
     *                         { y : y < 0 || y > 255 }.
     */
    private static final Predicate<Block> IS_AIR = (block) -> block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR;

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
