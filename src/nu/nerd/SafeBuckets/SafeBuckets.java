package nu.nerd.SafeBuckets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import nu.nerd.SafeBuckets.database.SafeLiquid;
import nu.nerd.SafeBuckets.database.SafeLiquidTable;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SafeBuckets extends JavaPlugin {

    private final SafeBucketsListener l = new SafeBucketsListener(this);
    public SafeLiquidTable table;
    public static final Logger log = Logger.getLogger("Minecraft");
    public HashMap<String, HashSet<Long>> cachedSafeBlocks = new HashMap<String, HashSet<Long>>();
    //public HashSet<Long> cachedSafeBlocks = new HashSet<Long>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("sbrl")) {
        	reloadConfig();
        	sender.sendMessage("SafeBuckets: reloaded config");
        	log.info("SafeBuckets: reloaded config");
        }

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
        table.save(stat);
        
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
