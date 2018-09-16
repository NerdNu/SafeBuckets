package nu.nerd.SafeBuckets;

import me.sothatsit.usefulsnippets.EnchantGlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ----------------------------------------------------------------------------------------------------------
/**
 * Represents a static collection of Liquids. Accessible information includes the liquid's simple name (a
 * human-readable String), block representation (a Material), and bucket representation (a Material).
 */
public enum Liquid {

    WATER("Water", Material.WATER, Material.WATER_BUCKET),
    COD("Bucket of Cod", Material.WATER, Material.COD_BUCKET),
    PUFFERFISH("Bucket of Pufferfish", Material.WATER, Material.PUFFERFISH_BUCKET),
    SALMON("Bucket of Salmon", Material.WATER, Material.SALMON_BUCKET),
    TROPICAL_FISH("Bucket of Tropical Fish", Material.WATER, Material.TROPICAL_FISH_BUCKET),
    LAVA("Lava", Material.LAVA, Material.LAVA_BUCKET);

    // ------------------------------------------------------------------------------------------------------
    /**
     * The selected Liquid's simple name.
     */
    private String _simpleName;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The selected Liquid's block representation (as a Material).
     */
    private Material _blockMaterial;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The selected Liquid's bucket representation (as a Material).
     */
    private Material _bucketMaterial;

    // ------------------------------------------------------------------------------------------------------
    /**
     * Constructor.
     */
    Liquid(String simpleName, Material material, Material bucket) {
        _simpleName = simpleName;
        _blockMaterial = material;
        _bucketMaterial = bucket;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the simple name of the selected Liquid.
     *
     * @return the simple name of the selected Liquid.
     */
    public String getSimpleName() {
        return _simpleName;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the block (as a Material) of the selected Liquid.
     *
     * @return the block (as a Material) of the selected Liquid.
     */
    public Material getBlockMaterial() {
        return _blockMaterial;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns the bucket (as a Material) of the selected Liquid.
     *
     * @return the bucket (as a Material) of the selected Liquid.
     */
    public Material getBucketMaterial() {
        return _bucketMaterial;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns a bucket corresponding to the selected Liquid.
     *
     * @param safe if the bucket should be safe.
     * @return a bucket corresponding to the selected Liquid.
     */
    public ItemStack getBucket(boolean safe) {
        ItemStack itemStack = new ItemStack(_bucketMaterial);
        if (!safe) {
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName("Unsafe " + _simpleName + " Bucket");
            itemStack.setItemMeta(meta);
            EnchantGlow.addGlow(itemStack);
        }
        return itemStack;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Attempts to find a Liquid whose material matches the input material.
     *
     * @param material the material.
     * @return a Liquid if found; null otherwise.
     */
    public static Liquid getType(Material material) {
        for (Liquid liquid : values()) {
            if (liquid.getBlockMaterial() == material || liquid.getBucketMaterial() == material) {
                return liquid;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Attempts to find a Liquid whose material matches the input item's material.
     *
     * @param itemStack the item.
     * @return a Liquid if found; null otherwise.
     */
    public static Liquid getType(ItemStack itemStack) {
        return getType(itemStack.getType());
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Attempts to find a Liquid whose material matches the input block's material.
     *
     * @param block the block.
     * @return a Liquid if found; null otherwise.
     */
    public static Liquid getType(Block block) {
        return getType(block.getType());
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Attempts to find a Liquid whose simple name matches the input string.
     *
     * @param string the string.
     * @return a Liquid if found; null otherwise.
     */
    public static Liquid getType(String string) {
        for (Liquid liquid : values()) {
            if (liquid.getSimpleName().equalsIgnoreCase(string)) {
                return liquid;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Determines if the input material matches the material of any Liquid. Use this method in favor of
     * one of the getType methods if the exact Liquid is not needed.
     *
     * @param material the material.
     * @return true if the material matches the material of any Liquid.
     */
    public static boolean isSupportedType(Material material) {
        for (Liquid liquid : values()) {
            if (liquid.getBlockMaterial() == material || liquid.getBucketMaterial() == material) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Determines if the input block's material matches the material of any Liquid. Use this method in favor
     * of one of the getType methods if the exact Liquid is not needed.
     *
     * @param block the block.
     * @return true if the material matches the material of any Liquid.
     */
    public static boolean isSupportedType(Block block) {
        return isSupportedType(block.getType());
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns a string of all supported Liquids. Returns simple names for readability.
     *
     * @return a string of all supported Liquids.
     */
    public static String getSupportedTypes() {
        return stream().map(Liquid::getSimpleName)
                       .collect(Collectors.joining(", "));
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns a sequential stream with this enum's values as its source.
     *
     * @return a sequential stream with this enum's values as its source.
     */
    public static Stream<Liquid> stream() {
        return Arrays.stream(values());
    }

}
