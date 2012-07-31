package nu.nerd.SafeBuckets;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import nu.nerd.SafeBuckets.database.SafeLiquid;
import nu.nerd.SafeBuckets.database.SafeLiquidTable;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class SafeBuckets extends JavaPlugin
{
    private final SafeBucketsListener l = new SafeBucketsListener(this);
    public SafeLiquidTable table;
    public static final Logger log = Logger.getLogger("Minecraft");

    @Override
    public void onDisable()
    {
        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " disabled.");
    }

    @Override
    public void onEnable()
    {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(l, this);
        
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
