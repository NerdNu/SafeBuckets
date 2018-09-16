package nu.nerd.SafeBuckets;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

// ----------------------------------------------------------------------------------------------------------
/**
 * The configuration class.
 */
class Configuration {

    // ------------------------------------------------------------------------------------------------------
    /**
     * The safe/unsafe inspection tool.
     */
    Material TOOL_ITEM;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If true, ice will not melt.
     */
    boolean PREVENT_ICE_MELT;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If false, buckets will not be usable.
     */
    boolean BUCKETS_ENABLED;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If true, liquids placed by buckets will be safe by default.
     */
    boolean BUCKETS_SAFE;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If false, dispeners will not be placeable.
     */
    boolean DISPENSERS_ENABLED;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If true, dispensers will be safe by default.
     */
    boolean DISPENSERS_SAFE;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If true, WorldEdit compatibility will be checked. If false, it will be ignored.
     */
    boolean WORLDEDIT_HOOK;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If true, those with appropriate permission will be able to flow liquid in a WorldEdit selection.
     */
    boolean WORLDEDIT_FLOWSEL_ENABLED;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The maximum size (in sq. blocks) imposed on WorldEdit liquid flow selections.
     */
    int WORLDEDIT_FLOWSEL_MAX_BLOCKS;

    // ------------------------------------------------------------------------------------------------------
    /**
     * If true, players can flow water inside their own WorldGuard regions.
     */
    boolean PLAYER_SELF_FLOW;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The player self-flow mode state.
     */
    PlayerFlowMode PLAYER_SELF_FLOW_MODE;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The maximum time (in ms) for player self-flow mode to remain active.
     */
    int PLAYER_SELF_FLOW_DURATION;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The period (in ticks) between each player self-flow mode cache review.
     */
    int PLAYER_SELF_FLOW_CACHE_REVIEW_PERIOD;

    // ------------------------------------------------------------------------------------------------------
    /**
     * The currently-loaded FileConfiguration instance.
     */
    private FileConfiguration _config;

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

        TOOL_ITEM = getMaterial(Keys.TOOL_ITEM.getKey(), Material.BLAZE_ROD);

        PREVENT_ICE_MELT = _config.getBoolean(Keys.PREVENT_ICE_MELT.getKey(), true);

        PLAYER_SELF_FLOW = _config.getBoolean(Keys.PLAYER_SELF_FLOW.getKey(), true);
        try {
            String flowMode = _config.getString(Keys.PLAYER_SELF_FLOW_MODE.getKey(), "OWNER");
            PLAYER_SELF_FLOW_MODE = PlayerFlowMode.valueOf(flowMode);
        } catch (Exception e) {
            PLAYER_SELF_FLOW_MODE = PlayerFlowMode.OWNER;
        }
        PLAYER_SELF_FLOW_DURATION = _config.getInt(Keys.PLAYER_SELF_FLOW_DURATION.getKey(), 300) * 1000;
        PLAYER_SELF_FLOW_CACHE_REVIEW_PERIOD = _config.getInt(Keys.PLAYER_SELF_FLOW_CACHE_REVIEW_PERIOD.getKey(), 30) * Util.TPS;

        DISPENSERS_ENABLED = _config.getBoolean(Keys.DISPENSERS_ALLOWED.getKey(), true);
        DISPENSERS_SAFE = _config.getBoolean(Keys.DISPENSERS_SAFE.getKey(), true);

        BUCKETS_ENABLED = _config.getBoolean(Keys.BUCKETS_ALLOWED.getKey(), true);
        BUCKETS_SAFE = _config.getBoolean(Keys.BUCKETS_SAFE.getKey(), true);

        WORLDEDIT_HOOK = _config.getBoolean(Keys.WORLDEDIT_HOOK.getKey(), true);
        WORLDEDIT_FLOWSEL_ENABLED = _config.getBoolean(Keys.WORLDEDIT_FLOWSEL_ENABLED.getKey(), true);
        WORLDEDIT_FLOWSEL_MAX_BLOCKS = _config.getInt(Keys.WORLDEDIT_FLOWSEL_MAX_BLOCKS.getKey(), 100);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * A custom method for retriving Material objects from the configuration, a la
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
     * The YAML keys (as strings) for each configuration option.
     */
    private enum Keys {

        TOOL_ITEM("tools.inspection-tool"),

        PREVENT_ICE_MELT("physics.prevent-ice-melt"),

        BUCKETS_ALLOWED("buckets.enabled"),
        BUCKETS_SAFE("buckets.default-to-safe"),

        DISPENSERS_ALLOWED("dispensers.enabled"),
        DISPENSERS_SAFE("dispensers.default-to-safe"),

        WORLDEDIT_HOOK("worldedit.hook"),
        WORLDEDIT_FLOWSEL_ENABLED("worldedit.flow-selection.enabled"),
        WORLDEDIT_FLOWSEL_MAX_BLOCKS("worldedit.flow-selection.maxblocks"),

        PLAYER_SELF_FLOW("player-self-flow.enabled"),
        PLAYER_SELF_FLOW_MODE("player-self-flow.mode"),
        PLAYER_SELF_FLOW_DURATION("player-self-flow.duration-in-seconds"),
        PLAYER_SELF_FLOW_CACHE_REVIEW_PERIOD("player-self-flow.cache-review-period-in-seconds");

        private String _key;

        Keys(String key) {
            _key = key;
        }

        public String getKey() {
            return _key;
        }

    } // Keys

} // Configuration