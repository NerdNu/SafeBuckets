package nu.nerd.SafeBuckets;

import org.bukkit.Material;
import org.bukkit.block.Block;

// ----------------------------------------------------------------------------------------------------------
/**
 * Represents a static collection of Frozen Liquids. Accessible information includes the frozen liquid's
 * simple name (a human-readable String), block representation (a Material), and whether or not the frozen
 * liquid "melts" when broken (i.e. replaces itself with a liquid block).
 */
public enum FrozenLiquid {

    BLUE_ICE("Blue Ice", Material.BLUE_ICE, false),
    PACKED_ICE("Packed Ice", Material.PACKED_ICE, false),
    ICE("Ice", Material.ICE, true),
    FROSTED_ICE("Frosted Ice", Material.FROSTED_ICE, true);

    // ------------------------------------------------------------------------------------------------------
    /**
     * The selected Frozen Liquid's simple name.
     */
    private String _simpleName;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The selected Frozen Liquid's block representation (as a Material).
     */
    private Material _blockMaterial;

    // ------------------------------------------------------------------------------------------------------
    /**
     * Whether or not the selected Frozen Liquid will "melt" when broken (i.e. replace itself with liquid).
     */
    private boolean _meltsWhenBroken;

    // ------------------------------------------------------------------------------------------------------
    /**
     * Constructor.
     */
    FrozenLiquid(String simpleName, Material material, boolean melts) {
        _simpleName = simpleName;
        _blockMaterial = material;
        _meltsWhenBroken = melts;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the simple name of the selected Frozen Liquid.
     *
     * @return the simple name of the selected Frozen Liquid.
     */
    public String getSimpleName() {
        return _simpleName;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the block (as a Material) of the selected Frozen Liquid.
     *
     * @return the block (as a Material) of the selected Frozen Liquid.
     */
    public Material getBlockMaterial() {
        return _blockMaterial;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns true if the Frozen Liquid will "melt" when broken (i.e. replace itself with liquid).
     *
     * @return true if the Frozen Liquid will "melt" when broken (i.e. replace itself with liquid).
     */
    public boolean meltsWhenBroken() {
        return _meltsWhenBroken;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Attempts to find a Frozen Liquid whose material matches the input block's material.
     *
     * @param block the block.
     * @return a Frozen Liquid if found; null otherwise.
     */
    public static FrozenLiquid getType(Block block) {
        Material material = block.getType();
        for (FrozenLiquid frozenLiquid : values()) {
            if (frozenLiquid.getBlockMaterial() == material) {
                return frozenLiquid;
            }
        }
        return null;
    }

}