package nu.nerd.SafeBuckets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import me.sothatsit.usefulsnippets.EnchantGlow;

import nu.nerd.SafeBuckets.database.SafeLiquid;
import nu.nerd.SafeBuckets.database.SafeLiquidTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

		} else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("safebuckets.reload")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
				return true;
			}

        	reloadConfig();
        	sender.sendMessage("SafeBuckets: reloaded config");
        	log.info("SafeBuckets: reloaded config");
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
}
