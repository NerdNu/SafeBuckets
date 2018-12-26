package nu.nerd.SafeBuckets;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

// ------------------------------------------------------------------------
/**
 * The command-handling class.
 */
public class Commands implements CommandExecutor {

    // ------------------------------------------------------------------------
    /**
     * Constructor, called once during {@link JavaPlugin#onEnable()}.
     */
    Commands() {
        SafeBuckets.PLUGIN.getCommand("sb").setExecutor(this);
        SafeBuckets.PLUGIN.getCommand("flow").setExecutor(this);
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

            if (!SafeBuckets._worldEditEnabled || !Configuration.WORLDEDIT_FLOWSEL_ENABLED) {
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

            if (itemInHand == null) {
                sender.sendMessage(ChatColor.RED + "[SafeBuckets] Your main hand was null, but that shouldn't happen.");
                return true;
            }

            if (itemInHand.getType() == Material.WATER_BUCKET || itemInHand.getType() == Material.LAVA_BUCKET) {
                safe = SafeBuckets.isUnsafeBucket(itemInHand);
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

            ItemStack newItem = SafeBuckets.getBucket(liquidContainer, safe);
            player.getInventory().setItemInMainHand(newItem);
        }

        return true;
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

        if (!SafeBuckets._worldGuardEnabled || !Configuration.PLAYER_SELF_FLOW) {
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
     * Flows applicable liquids within a WorldEdit selection.
     *
     * @param player the player performing the action.
     */
    private void flowLiquidsInSelection(Player player) {
        int blocksAffected = 0;

        if (!SafeBuckets._worldEditEnabled) {
            player.sendMessage(ChatColor.RED + "WorldEdit must be installed to do that!");
            return;
        }

        WorldEditPlugin worldEdit = (WorldEditPlugin) SafeBuckets.PLUGIN.getServer().getPluginManager().getPlugin("WorldEdit");
        LocalSession localSession = worldEdit.getSession(player);
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
                    if (block.getType() == Material.WATER || block.getType() == Material.LAVA || Util.isWaterlogged(block)) {
                        SafeBuckets.setSafe(block, false);
                        blocksAffected++;
                    }
                }
            }
        }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Flowed " + blocksAffected + " blocks around " + Util.formatCoords(player.getLocation()) + ".");
    }

}
