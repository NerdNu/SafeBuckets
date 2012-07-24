package nu.nerd.SafeBuckets;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import nu.nerd.SafeBuckets.database.SafeLiquid;
import nu.nerd.SafeBuckets.database.SafeLiquidTable;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class SafeBuckets extends JavaPlugin
{
    private final SafeBucketsListener l = new SafeBucketsListener(this);
    public SafeLiquidTable table;
    public HashMap<String, TreeSet<Long>> bucketBlocks;
    public static final Logger log = Logger.getLogger("Minecraft");

    public void saveSet()
    {
        File saveFile = new File(this.getDataFolder() + File.separator + "bucketBlocks.dat");
        saveFile.getParentFile().mkdirs();

        try {
            FileOutputStream fos = new FileOutputStream(saveFile);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(bucketBlocks);
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public void loadSet()
    {
        File saveFile = new File(this.getDataFolder() + File.separator + "bucketBlocks.dat");
        if (saveFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(saveFile);
                ObjectInputStream in = new ObjectInputStream(fis);
                bucketBlocks = (HashMap<String, TreeSet<Long>>)in.readObject();
                // storage for new worlds
                for (World world : Bukkit.getWorlds())
                    if (!bucketBlocks.containsKey(world.getName()))
                        bucketBlocks.put(world.getName(), new TreeSet<Long>());
                return;
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }

        //if file doesn't exist or can't be loaded make new storage
        bucketBlocks = new HashMap<String, TreeSet<Long>>();
        for (World world : Bukkit.getWorlds())
            bucketBlocks.put(world.getName(), new TreeSet<Long>());
    }

    @Override
    public void onDisable()
    {
        saveSet();

        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " disabled.");
    }

    @Override
    public void onEnable()
    {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(l, this);

        loadSet();
        
        setupDatabase();
		table = new SafeLiquidTable(this);
        
        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");
    }
    
	public boolean setupDatabase() {
        try {
            getDatabase().find(SafeLiquid.class).findRowCount();
        } catch (PersistenceException ex) {
            getLogger().log(Level.INFO, "First run, initializing database.");
            installDDL();
            return true;
        }
        
        return false;
    }
	
	@Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(SafeLiquid.class);
        return list;
    }
}
