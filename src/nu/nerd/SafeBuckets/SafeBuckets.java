package nu.nerd.SafeBuckets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import me.sothatsit.usefulsnippets.EnchantGlow;

import nu.nerd.SafeBuckets.database.SafeLiquid;
import nu.nerd.SafeBuckets.database.SafeLiquidTable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

public class SafeBuckets extends JavaPlugin {

    private final SafeBucketsListener l = new SafeBucketsListener(this);
    public SafeLiquidTable table;
    public static final Logger log = Logger.getLogger("Minecraft");
    public HashMap<String, HashSet<Long>> cachedSafeBlocks = new HashMap<String, HashSet<Long>>();
    //public HashSet<Long> cachedSafeBlocks = new HashSet<Long>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
		if (!command.getName().equalsIgnoreCase("sb")) {
			return false;
		}

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("safebuckets.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            reloadConfig();
            sender.sendMessage("SafeBuckets: reloaded config");
            log.info("SafeBuckets: reloaded config");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("flowsel")) {
            if (!sender.hasPermission("safebuckets.flowsel")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("[SafeBuckets] Console flow water!");
                return true;
            }
            if (getWE() == null) {
                sender.sendMessage(ChatColor.RED + "This feature requires WorldEdit to be installed.");
                return true;
            }
            if (!this.getConfig().getBoolean("flowsel.enabled")) {
                sender.sendMessage(ChatColor.RED + "That feature is not enabled in the SafeBuckets config.");
                return true;
            }
            Player player = (Player)sender;
            int blocks = this.flowLiquidsInSelection(player);
            if (blocks > -1) {
                String msg = String.format("Flowed %d liquid blocks", blocks);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + msg);
            }
            return true;
        }

		if (0 <= args.length && args.length <= 2) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("[SafeBuckets] Console can't hold a water bucket!");
				return true;
			}
			if (!sender.hasPermission("safebuckets.tools.unsafe")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
				return true;
			}

			Player player = (Player)sender;
			ItemStack itemInHand = player.getItemInHand();

			Boolean safe = true;
			Material liquidContainer = Material.WATER_BUCKET;

			if (itemInHand.getType().equals(Material.WATER_BUCKET) || itemInHand.getType().equals(Material.LAVA_BUCKET)) {
				safe = isUnsafeBucket(itemInHand);
				liquidContainer = itemInHand.getType();
			}

			if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("safe"))
					safe = true;
				else if (args[0].equalsIgnoreCase("unsafe"))
					safe = false;
				else {
					sender.sendMessage("[SafeBuckets] Valid conditions are safe and unsafe");
					return true;
				}

				if (args.length == 2) {
					if (args[1].equalsIgnoreCase("water"))
						liquidContainer = Material.WATER_BUCKET;
					else if (args[1].equalsIgnoreCase("lava"))
						liquidContainer = Material.LAVA_BUCKET;
					else {
						sender.sendMessage("[SafeBuckets] Valid liquids are water and lava");
						return true;
					}
				}
			}

			ItemStack newItem;
			if (safe)
				newItem = getSafeBucket(liquidContainer);
			else
				newItem = getUnSafeBucket(liquidContainer);

			if (itemInHand.getType().equals(liquidContainer) || itemInHand.getType().equals(Material.AIR))
				player.getInventory().setItemInHand(newItem);
			else
				player.getInventory().addItem(newItem);

		}

        return true;
    }

	public ItemStack getSafeBucket(Material liquidContainer) {
		return new ItemStack(liquidContainer);
	}

	public ItemStack getUnSafeBucket(Material liquidContainer) {
		ItemStack unsafeBucket = new ItemStack(liquidContainer);
		String liquidName = "Water";
		if (liquidContainer.equals(Material.WATER_BUCKET)) {
			liquidName = "Water";
		} else if (liquidContainer.equals(Material.LAVA_BUCKET)) {
			liquidName = "lava";
		}

		ItemMeta meta = unsafeBucket.getItemMeta();
		meta.setDisplayName("Unsafe " + liquidName + " Bucket");
		unsafeBucket.setItemMeta(meta);
		EnchantGlow.addGlow(unsafeBucket);

		return unsafeBucket;
	}

	public boolean isUnsafeBucket(ItemStack item) {
		if (item.getType().equals(Material.WATER_BUCKET) || item.getType().equals(Material.LAVA_BUCKET))
			return EnchantGlow.hasGlow(item);
		
		return false;
	}

    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this); //ensure database is unlocked
        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " disabled.");
    }

    @Override
    public void onEnable() {
    	saveDefaultConfig();

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(l, this);

        setupDatabase();
        table = new SafeLiquidTable(this);

        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");
    }

    public boolean setupDatabase() {
        try {
            getDatabase().find(SafeLiquid.class).findRowCount();
            List<SafeLiquid> liquids = getDatabase().find(SafeLiquid.class).findList();
            for (SafeLiquid l : liquids) {
                addSafeLiquidToCache(l);
            }
        } catch (PersistenceException ex) {
            getLogger().log(Level.INFO, "First run, initializing database.");
            installDDL();
            return true;
        }

        return false;
    }

    public void addBlockToCacheAndDB(Block block) {
        SafeLiquid stat = new SafeLiquid();
        stat.setWorld(block.getWorld().getName());
        stat.setX(block.getX());
        stat.setY(block.getY());
        stat.setZ(block.getZ());

        table.queueAdd.add(stat);
        addSafeLiquidToCache(stat);
    }
    
    public void removeSafeLiquidFromCacheAndDB(Block block) {
        String world = block.getWorld().getName();
        Long l = Util.GetHashCode(block.getX(), block.getY(), block.getZ());
        if (cachedSafeBlocks.containsKey(world)) {
            cachedSafeBlocks.get(world).remove(l);
        }
        
        table.removeSafeLiquid(block);
    }
    
    public boolean isSafeLiquid(Block block) {
        String world = block.getWorld().getName();
        Long l = Util.GetHashCode(block.getX(), block.getY(), block.getZ());
        
        if (cachedSafeBlocks.containsKey(world)) {
            return cachedSafeBlocks.get(world).contains(l);
        }
        return false;
    }

    public void addSafeLiquidToCache(SafeLiquid liquid) {
        String world = liquid.getWorld();
        if (!cachedSafeBlocks.containsKey(world)) {
            cachedSafeBlocks.put(world, new HashSet<Long>());
        }
        cachedSafeBlocks.get(world).add(Util.GetHashCode(liquid.getX(), liquid.getY(), liquid.getZ()));
    }

    @Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(SafeLiquid.class);
        return list;
    }

    public int flowLiquidsInSelection(Player player) {
        int blocksAffected = 0;
        int maxSelSize = this.getConfig().getInt("flowsel.maxsize");
        Selection sel = getWE().getSelection(player);
        if (sel != null) {
            if (!(sel instanceof CuboidSelection)) {
                player.sendMessage(ChatColor.RED + "You must use a cuboid selection!");
                return -1;
            }
            if (sel.getArea() > maxSelSize && maxSelSize != 0) {
                player.sendMessage(ChatColor.RED + String.format("Your selection must be under %d blocks.", maxSelSize));
                return -1;
            }
            World world = sel.getWorld();
            Location min = sel.getMinimumPoint();
            Location max = sel.getMaximumPoint();
            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA) {
                            if (block.getData() != 0x0) continue; //ensure source block
                            Long l = Util.GetHashCode(block.getX(), block.getY(), block.getZ());
                            if (cachedSafeBlocks.containsKey(world.getName())) {
                                cachedSafeBlocks.get(world.getName()).remove(l);
                            }
                            table.queueRemove.add(block);
                            blocksAffected++;
                            if (block.getType() == Material.STATIONARY_WATER) {
                                block.setType(Material.WATER);
                            } else {
                                block.setType(Material.LAVA);
                            }
                        }
                    }
                }
            }
            int px = player.getLocation().getBlockX();
            int py = player.getLocation().getBlockY();
            int pz = player.getLocation().getBlockZ();
            String msg = String.format("%s flowed %d blocks near x:%d, y:%d, z:%d", player.getName(), blocksAffected, px, py, pz);
            getLogger().info(msg);
        } else {
            player.sendMessage(ChatColor.RED + "You must make a selection first!");
            return -1;
        }
        return blocksAffected;
    }

    private WorldEditPlugin getWE() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            return null;
        }
        return (WorldEditPlugin) plugin;
    }

}
