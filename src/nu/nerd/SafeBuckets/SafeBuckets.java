package nu.nerd.SafeBuckets;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import me.sothatsit.usefulsnippets.EnchantGlow;
import net.sothatsit.blockstore.BlockStoreApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;

// ------------------------------------------------------------------------
/**
 * The main plugin and command-handling class.
 */
public class SafeBuckets extends JavaPlugin {

    // ------------------------------------------------------------------------
    /**
     * This plugin.
     */
    static SafeBuckets PLUGIN;

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable().
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        new Configuration();

        new PlayerFlowCache();
        new SafeBucketsListener();

        // create glow enchantment for compatibility with ModMode item serialization
        EnchantGlow.getGlow();
    }

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onDisable().
     */
    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    // ------------------------------------------------------------------------
    /**
     * If true, the given block is safe. Consults cache before calling the BlockStore API.
     *
     * @param block the block.
     * @return true if the given block is safe.
     */
    static boolean isSafe(Block block) {
        if (CACHE.contains(block.getLocation())) {
            return true;
        } else {
            Object o = BlockStoreApi.getBlockMeta(block, SafeBuckets.PLUGIN, METADATA_KEY);
            if (o == null || !((boolean) o)) {
                return false;
            } else {
                CACHE.add(block.getLocation());
                return true;
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Sets the safety status of a given block and updates cache & BlockStore.
     *
     * @param block the block.
     * @param state the safety status (true = safe).
     */
    static void setSafe(Block block, boolean state) {
        if (state) {
            CACHE.add(block.getLocation());
        } else {
            CACHE.remove(block.getLocation());
            Util.forceBlockUpdate(block);
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, () -> showParticles(block, state), 1);
        BlockStoreApi.setBlockMeta(block, PLUGIN, METADATA_KEY, state);
    }

    // ------------------------------------------------------------------------
    /**
     * Displays colored particles around the given block to visually indicate the
     * new safety state.
     *
     * @param block the block.
     * @param state the new state.
     */
    private static void showParticles(Block block, boolean state) {
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
     * Removes the safety status of the given block from both cache and BlockStore. Removal is not synonymous
     * with setSafe(*, false).
     *
     * @param block the block.
     */
    static void removeSafe(Block block) {
        CACHE.remove(block.getLocation());
        BlockStoreApi.removeBlockMeta(block, PLUGIN, METADATA_KEY);
        showParticles(block, false);
    }

    // ------------------------------------------------------------------------
    /**
     * Determines if a block is eligible to be flowed, e.g. a waterlogged block or a liquid block.
     *
     * @param block the block.
     * @return true if the block is eligible to be flowed.
     */
    static boolean canBeFlowed(Block block) {
        return Configuration.LIQUID_BLOCKS.contains(block.getType()) || block.getBlockData() instanceof Waterlogged;
    }

    // ------------------------------------------------------------------------
    /**
     * Determines if a given ItemStack matches an unsafe bucket.
     *
     * @param item the ItemStack.
     * @return true if the given ItemStack matches an unsafe bucket.
     */
    private boolean isUnsafeBucket(ItemStack item) {
        return Configuration.BUCKETS.contains(item.getType()) && EnchantGlow.hasGlow(item);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(CommandSender, Command, String, String[]).
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("sb")) {
            return sbCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("flow")) {
            return playerFlowCommand(sender);
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Handles the /sb command.
     *
     * @param sender the CommandSender.
     * @param args the command args.
     * @return true if processing is successful.
     */
    private boolean sbCommand(CommandSender sender, String[] args) {

        // /sb reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("safebuckets.reload")) {
                Configuration.reload();
                sender.sendMessage(ChatColor.DARK_AQUA + "Configuration reloaded.");
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
            }
            return true;
        }

        // /sb flowsel
        if (args.length == 1 && args[0].equalsIgnoreCase("flowsel")) {

            if (!sender.hasPermission("safebuckets.flowsel")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                return true;
            }

            if (!hasWorldEdit() || !Configuration.WORLDEDIT_FLOWSEL_ENABLED) {
                sender.sendMessage(ChatColor.RED + "That feature is not enabled.");
                return true;
            }

            if (sender instanceof Player) {
                flowLiquidsInSelection((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "You must be in-game to flow liquids!");
            }
            return true;
        }

        // /sb
        if (args.length <= 2) {

            if (!sender.hasPermission("safebuckets.tools.unsafe")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must be in-game to flow liquids!");
                return true;
            }

            Player player = (Player) sender;
            ItemStack itemInHand = player.getEquipment().getItemInMainHand();

            boolean safe = false;
            Material liquidContainer = Material.WATER_BUCKET;

            if (itemInHand != null &&
                itemInHand.getType() != Material.AIR &&
                itemInHand.getType() != Material.BUCKET &&
                itemInHand.getType() != Material.WATER_BUCKET &&
                itemInHand.getType() != Material.LAVA_BUCKET) {
                sender.sendMessage(ChatColor.RED +
                                   "[SafeBuckets] Your main hand must be empty, or holding an empty bucket, water bucket or lava bucket.");
                return true;
            }

            if (itemInHand.getType() == Material.WATER_BUCKET || itemInHand.getType() == Material.LAVA_BUCKET) {
                safe = isUnsafeBucket(itemInHand);
                liquidContainer = itemInHand.getType();
            }

            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("safe")) {
                    safe = true;
                } else if (args[0].equalsIgnoreCase("unsafe")) {
                    safe = false;
                } else {
                    sender.sendMessage(ChatColor.RED + "[SafeBuckets] Valid conditions are safe and unsafe");
                    return true;
                }

                if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("water"))
                        liquidContainer = Material.WATER_BUCKET;
                    else if (args[1].equalsIgnoreCase("lava"))
                        liquidContainer = Material.LAVA_BUCKET;
                    else {
                        sender.sendMessage(ChatColor.RED + "[SafeBuckets] Valid liquids are water and lava");
                        return true;
                    }
                }
            }

            ItemStack newItem = safe ? getSafeBucket(liquidContainer) : getUnSafeBucket(liquidContainer);
            player.getInventory().setItemInMainHand(newItem);
        }

        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns an ItemStack corresponding to a safe version of the given bucket.
     *
     * @param liquidContainer the bucket.
     * @return a safe bucket as an ItemStack.
     */
    private ItemStack getSafeBucket(Material liquidContainer) {
        return new ItemStack(liquidContainer);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns an ItemStack corresponding to an unsafe version of the given bucket.
     *
     * @param liquidContainer the bucket.
     * @return an unsafe bucket as an ItemStack.
     */
    private static ItemStack getUnSafeBucket(Material liquidContainer) {
        ItemStack unsafeBucket = new ItemStack(liquidContainer);
        String liquidName = "Water";
        if (liquidContainer.equals(Material.LAVA_BUCKET)) {
            liquidName = "Lava";
        }

        ItemMeta meta = unsafeBucket.getItemMeta();
        meta.setDisplayName("Unsafe " + liquidName + " Bucket");
        unsafeBucket.setItemMeta(meta);
        EnchantGlow.addGlow(unsafeBucket);

        return unsafeBucket;
    }

    // ------------------------------------------------------------------------
    /**
     * Faciliates execution of the /flow command.
     *
     * @param sender the CommandSender.
     * @return true if the execution is successful.
     */
    private boolean playerFlowCommand(CommandSender sender) {

        if (!sender.hasPermission("safebuckets.playerflow")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
            return true;
        }

        if (!hasWorldGuard() || !Configuration.PLAYER_SELF_FLOW) {
            sender.sendMessage(ChatColor.RED + "That feature is not enabled.");
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!PlayerFlowCache.isCached(player)) {
                PlayerFlowCache.cache(player);
            } else {
                PlayerFlowCache.forceExpire(player);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You must be in-game to flow liquids!");
        }

        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if WorldEdit is present.
     *
     * @return true if WorldEdit is present.
     */
    private boolean hasWorldEdit() {
        return getWorldEdit() != null;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the WorldEdit plugin if present, null otherwise.
     *
     * @return the WorldEdit plugin if present, null otherwise.
     */
    private WorldEditPlugin getWorldEdit() {
        if (!Configuration.WORLDEDIT_HOOK) {
            return null;
        }
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        return plugin instanceof WorldEditPlugin ? (WorldEditPlugin) plugin : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if WorldGuard is present.
     *
     * @return true if WorldGuard is present.
     */
    private boolean hasWorldGuard() {
        return getWorldGuard() != null;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the WorldGuard plugin if present, null otherwise.
     *
     * @return the WorldGuard plugin if present, null otherwise.
     */
    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        return plugin instanceof WorldGuardPlugin ? (WorldGuardPlugin) plugin : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Determines if a player can flow a given block.
     *
     * @param player the player.
     * @param block the block.
     * @return true if the player can flow the given block.
     */
    static boolean isPlayerFlowPermitted(Player player, Block block) {
        if (!PLUGIN.hasWorldGuard()) {
            return false;
        }

        com.sk89q.worldedit.world.World wrappedWorld = BukkitAdapter.adapt(block.getWorld());
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wrappedWorld);
        LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (regions != null) {
            Location loc = block.getLocation();
            BlockVector3 wrappedVector = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            ApplicableRegionSet applicable = regions.getApplicableRegions(wrappedVector);

            switch (Configuration.PLAYER_SELF_FLOW_MODE) {
                case OWNER:
                    return applicable.isOwnerOfAll(wgPlayer) && applicable.size() > 0;

                case MEMBER:
                    return applicable.isMemberOfAll(wgPlayer) && applicable.size() > 0;
            }

        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Flows applicable liquids within a WorldEdit selection.
     *
     * @param player the player performing the action.
     */
    private void flowLiquidsInSelection(Player player) {
        int blocksAffected = 0;

        if (getWorldEdit() == null) {
            player.sendMessage(ChatColor.RED + "WorldEdit must be installed to do that!");
            return;
        }

        LocalSession localSession = getWorldEdit().getSession(player);
        com.sk89q.worldedit.world.World wrappedWorld = BukkitAdapter.adapt(player.getWorld());
        Region region;
        try {
            region = localSession.getSelection(wrappedWorld);
            if (region == null) {
                throw new IncompleteRegionException();
            }
        } catch (IncompleteRegionException e) {
            player.sendMessage(ChatColor.RED + "You must select a region first!");
            return;
        }

        int regionArea = region.getArea();
        if (Configuration.WORLDEDIT_FLOWSEL_MAX_BLOCKS != 0 && regionArea > Configuration.WORLDEDIT_FLOWSEL_MAX_BLOCKS) {
            player.sendMessage(ChatColor.RED + "Your selection must be under " + Configuration.WORLDEDIT_FLOWSEL_MAX_BLOCKS + " blocks!");
            return;
        }

        World world = player.getWorld();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (canBeFlowed(block)) {
                        setSafe(block, false);
                        blocksAffected++;
                    }
                }
            }
        }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Flowed " + blocksAffected + " blocks around " + Util.formatCoords(player.getLocation()) + ".");
    }

    // ------------------------------------------------------------------------
    /**
     * A logging method used instead of {@link java.util.logging.Logger} to faciliate prefix coloring.
     *
     * @param msg the message to log.
     */
    static void log(String msg) {
        System.out.println(PREFIX + msg);
    }

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

    // ------------------------------------------------------------------------
    /**
     * This plugin's prefix as a string; for logging.
     */
    private static final String PREFIX = ChatColor.WHITE + "[" + ChatColor.AQUA + "SafeBuckets" + ChatColor.WHITE + "] ";

    // ------------------------------------------------------------------------
    /**
     * The BlockStore metadata key as a static final String for persistence.
     */
    private static final String METADATA_KEY = "safebuckets";

    // ------------------------------------------------------------------------
    /**
     * The block safety cache.
     */
    private static final HashSet<Location> CACHE = new HashSet<>();

}