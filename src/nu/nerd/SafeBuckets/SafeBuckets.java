package nu.nerd.SafeBuckets;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.server.Block;
import net.minecraft.server.Material;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class SafeBuckets extends JavaPlugin
{
    private final SafeBucketsPlayerListener pl = new SafeBucketsPlayerListener(this);
    public static final Logger log = Logger.getLogger("Minecraft");

    @Override
    public void onDisable()
    {
        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " disabled.");
    }

    @Override
    public void onEnable()
    {
        try {
            Block.byId[9] = null;
            SafeStationary water = new SafeStationary(9, Material.WATER);
            water.a("water");
//            Method c = water.getClass().getMethod("c", types)

            Block.byId[11] = null;
            SafeStationary lava = new SafeStationary(11, Material.LAVA);
            lava.a("lava");
        } catch (Exception err) {
            err.printStackTrace();
        }

        PluginManager pm = this.getServer().getPluginManager();
        //abusing monitor mode but we want to make sure we're after everything else
        pm.registerEvent(Type.PLAYER_BUCKET_EMPTY, pl, Priority.Monitor, this);

        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");
    }
}
