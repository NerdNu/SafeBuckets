package nu.nerd.SafeBuckets;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// ----------------------------------------------------------------------------------------------------------
/**
 * The configuration class.
 */
class Configuration {

    /**
     * The currently-loaded FileConfiguration instance.
     */
    private FileConfiguration _config;

    /**
     * A set of all liquid blocks to be handled.
     */
    static HashSet<Material> LIQUID_BLOCKS = new HashSet<>();

    /**
     * A set of all buckets capable of placing liquid in the world.
     */
    static HashSet<Material> BUCKETS = new HashSet<>();

    /**
     * A set of all meltable ice blocks.
     */
    static HashSet<Material> MELTABLE_ICE = new HashSet<>();

    /**
     * The safe/unsafe inspection tool.
     */
    Material TOOL_ITEM;

    /**
     * If true, ice will not melt.
     */
    boolean PREVENT_ICE_MELT;

    /**
     * If false, buckets will not be usable.
     */
    boolean BUCKETS_ENABLED;

    /**
     * If true, liquids placed by buckets will be safe by default.
     */
    boolean BUCKETS_SAFE;

    /**
     * If false, dispeners will not be placeable.
     */
    boolean DISPENSERS_ENABLED;

    /**
     * If true, dispensers will be safe by default.
     */
    boolean DISPENSERS_SAFE;

    /**
     * If true, WorldEdit compatibility will be checked. If false, it will be ignored.
     */
    boolean WORLDEDIT_HOOK;

    /**
     * If true, those with appropriate permission will be able to flow liquid in a WorldEdit selection.
     */
    boolean WORLDEDIT_FLOWSEL_ENABLED;

    /**
     * The maximum size (in sq. blocks) imposed on WorldEdit liquid flow selections.
     */
    int WORLDEDIT_FLOWSEL_MAX_BLOCKS;

    /**
     * If true, players can flow water inside their own WorldGuard regions.
     */
    boolean PLAYER_SELF_FLOW;

    /**
     * The player self-flow mode state.
     */
    PlayerFlowMode PLAYER_SELF_FLOW_MODE;

    /**
     * The maximum time (in ms) for player self-flow mode to remain active.
     */
    int PLAYER_SELF_FLOW_DURATION;

    /**
     * The period (in ticks) between each player self-flow mode cache review.
     */
    int PLAYER_SELF_FLOW_CACHE_REVIEW_PERIOD;

    // ------------------------------------------------------------------------------------------------------
    /**
     * Constructor. Called once during {@link JavaPlugin#onEnable()}.
     */
    Configuration() {
        reload();
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Reloads the configuration.
     */
    void reload() {
        SafeBuckets.PLUGIN.saveDefaultConfig();
        _config = SafeBuckets.PLUGIN.getConfig();

        LIQUID_BLOCKS.clear();
        LIQUID_BLOCKS.addAll(getMaterialList("liquids"));

        MELTABLE_ICE.clear();
        MELTABLE_ICE.addAll(getMaterialList("meltable-ice"));

        BUCKETS.clear();
        BUCKETS.addAll(getMaterialList("buckets.list"));

        TOOL_ITEM = getMaterial("tools.inspection-tool", Material.BLAZE_ROD);

        PREVENT_ICE_MELT = _config.getBoolean("physics.prevent-ice-melt", true);

        PLAYER_SELF_FLOW = _config.getBoolean("player-self-flow.enabled", true);
        try {
            String flowMode = _config.getString("player-self-flow.mode");
            PLAYER_SELF_FLOW_MODE = PlayerFlowMode.valueOf(flowMode);
        } catch (Exception e) {
            PLAYER_SELF_FLOW_MODE = PlayerFlowMode.OWNER;
        }
        PLAYER_SELF_FLOW_DURATION = _config.getInt("player-self-flow.duration-in-seconds", 300) * 1000;
        PLAYER_SELF_FLOW_CACHE_REVIEW_PERIOD = _config.getInt("player-self-flow.cache-review-period-in-seconds", 30) * Util.TPS;

        DISPENSERS_ENABLED = _config.getBoolean("dispensers.enabled", true);
        DISPENSERS_SAFE = _config.getBoolean("dispensers.default-to-safe", true);

        BUCKETS_ENABLED = _config.getBoolean("tools.buckets-enabled", true);
        BUCKETS_SAFE = _config.getBoolean("buckets.default-to-safe", true);

        WORLDEDIT_HOOK = _config.getBoolean("worldedit.hook", true);
        WORLDEDIT_FLOWSEL_ENABLED = _config.getBoolean("worldedit.flow-selection.enabled", true);
        WORLDEDIT_FLOWSEL_MAX_BLOCKS = _config.getInt("worldedit.flow-selection.maxblocks", 100);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * A custom method for retrieving Material objects from the configuration, a la
     * {@link FileConfiguration#getString(String)}.
     *
     * @param key the YAML key.
     * @param def the default value.
     * @return the value.
     */
    private Material getMaterial(String key, Material def) {
        String materialName = _config.getString(key);
        Material material = Material.getMaterial(materialName);
        return material != null ? material : def;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * A custom method for retrieving a set of Material objects from the configuration, a la
     * {@link FileConfiguration#getStringList(String)}.
     *
     * @param key the YAML key.
     * @return a set of Materials.
     */
    private Set<Material> getMaterialList(String key) {
        return _config.getStringList(key).stream()
                                         .map(Material::getMaterial)
                                         .filter(Objects::nonNull)
                                         .collect(Collectors.toSet());
    }

} // Configuration