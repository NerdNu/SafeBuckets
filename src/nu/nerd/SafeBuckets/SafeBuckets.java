package nu.nerd.SafeBuckets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class SafeBuckets extends JavaPlugin
{
    private final SafeBucketsPlayerListener pl = new SafeBucketsPlayerListener(this);
    private final SafeBucketsBlockListener bl = new SafeBucketsBlockListener(this);
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
        }
        catch (Exception e) {
            e.printStackTrace();
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
            }
            catch (Exception e) {
                e.printStackTrace();
                bucketBlocks = new HashMap<String, TreeSet<Long>>();
                for (World world : Bukkit.getWorlds())
                    bucketBlocks.put(world.getName(), new TreeSet<Long>());
            }
        }
        else {
            bucketBlocks = new HashMap<String, TreeSet<Long>>();
            for (World world : Bukkit.getWorlds())
                bucketBlocks.put(world.getName(), new TreeSet<Long>());
        }
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
        pm.registerEvent(Type.PLAYER_BUCKET_EMPTY, pl, Priority.Monitor, this);

        pm.registerEvent(Type.BLOCK_PLACE,   bl, Priority.Monitor, this);
        pm.registerEvent(Type.BLOCK_PHYSICS, bl, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_FROMTO,  bl, Priority.Highest, this);

        loadSet();

        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");
    }
}
