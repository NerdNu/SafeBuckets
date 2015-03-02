package nu.nerd.SafeBuckets.database;

import org.bukkit.block.Block;

import com.avaje.ebean.Query;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import nu.nerd.SafeBuckets.SafeBuckets;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class SafeLiquidTable {

	SafeBuckets plugin;
	public ConcurrentLinkedQueue<Block> queueRemove;
	public ConcurrentLinkedQueue<SafeLiquid> queueAdd;

	public SafeLiquidTable(SafeBuckets plugin) {
		this.plugin = plugin;
		this.queueRemove = new ConcurrentLinkedQueue<Block>();
		this.queueAdd = new ConcurrentLinkedQueue<SafeLiquid>();

		startSaveThread();
	}

	public SafeLiquid getID(int id) {
		SafeLiquid retVal = null;

		Query<SafeLiquid> query = plugin.getDatabase().find(SafeLiquid.class).where().eq("id", id).query();

		if (query != null) {
			retVal = query.findUnique();
		}

		return retVal;
	}

	public void startSaveThread() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
				save();
            }
        });
	}

	public void save() {
		while (!Thread.currentThread().isInterrupted()) {
			try
			{
				Thread.sleep(100);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			Iterator i = this.queueAdd.iterator();
			while (i.hasNext()) {
				try {
					SafeLiquid stat = (SafeLiquid) i.next();
					if (plugin.getConfig().getBoolean("debug.console"))
						plugin.getLogger().info("Adding   : " + stat.getWorld() + "," + stat.getX() + "," + stat.getY() + "," + stat.getZ());
					save(stat);
				} catch(Exception e) {
					e.printStackTrace();
				}
				i.remove();
			}

			i = this.queueRemove.iterator();
			while (i.hasNext()) {
				Block block = (Block) i.next();

				try {
					if (plugin.getConfig().getBoolean("debug.console"))
						plugin.getLogger().info("Removing : " + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
					removeSafeLiquidFromDB(block);
				} catch(Exception e) {
					e.printStackTrace();
				}

				i.remove();
			}
		}
	}

	public void removeSafeLiquid(final Block block){
		if (!isSafeLiquid(block)){
			return;
		}

		this.queueRemove.add(block);
	}

	public void removeSafeLiquidFromDB(final Block block){
		List<SafeLiquid> dbbl = null;
		Query<SafeLiquid> query = plugin.getDatabase().find(SafeLiquid.class).where()
		.eq("world", block.getWorld().getName())
		.eq("x",block.getX())
		.eq("y",block.getY())
		.eq("z",block.getZ())
		.query();

		if (query != null) {
			dbbl = query.findList();
			for (SafeLiquid s : dbbl){
				plugin.getDatabase().delete(s);
			}
		}
	}

	public boolean isSafeLiquid(Block block){
		try {
			int retVal = 0;
			Query<SafeLiquid> query = plugin.getDatabase().find(SafeLiquid.class).where()
			.eq("world", block.getWorld().getName())
			.eq("x",block.getX())
			.eq("y",block.getY())
			.eq("z",block.getZ())
			.query();
			if (query != null) {
				retVal = query.findRowCount();
			}

			return retVal > 0;
		} catch(Exception e) {
			e.printStackTrace();
			return true; // Just in case we fuck something up
		}
	}

	public void save(SafeLiquid deathstat) {
		plugin.getDatabase().save(deathstat);
	}

}
