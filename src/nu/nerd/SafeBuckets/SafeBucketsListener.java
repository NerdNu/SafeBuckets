package nu.nerd.SafeBuckets;

import java.util.TreeSet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class SafeBucketsListener implements Listener {
    
    private final SafeBuckets plugin;
    
    SafeBucketsListener(SafeBuckets instance) {
        plugin = instance;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent event)
    {
        if (event.isCancelled())
            return;

        Material mat = event.getBlock().getType();
        if (mat == Material.STATIONARY_LAVA || mat == Material.STATIONARY_WATER) {
            Long hash = Util.GetHashCode(event.getBlock().getX(),
                                         event.getBlock().getY(),
                                         event.getBlock().getZ());
            String name = event.getBlock().getWorld().getName();
            if (plugin.bucketBlocks.get(name).contains(hash)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event)
    {
        if (event.isCancelled())
            return;

        Block block = event.getBlock();
        Long hash = Util.GetHashCode(block.getX(), block.getY(), block.getZ());
        String name = block.getWorld().getName();

        if (plugin.bucketBlocks.get(name).contains(hash)) {
            //somehow our block got changed to flowing, change it back
            if (block.getType() == Material.WATER)
                block.setTypeId(9, false);
            if (block.getType() == Material.LAVA)
                block.setTypeId(11, false);

            event.setCancelled(true);
            return;
        }

        hash = Util.GetHashCode(event.getToBlock().getX(),
                                event.getToBlock().getY(),
                                event.getToBlock().getZ());

        if (plugin.bucketBlocks.get(name).contains(hash)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (event.isCancelled())
            return;

        //only stop tracking if source blocks were placed - this makes rollbacks work
        if (!event.getBlockPlaced().isLiquid())
            return;

        Long hash = Util.GetHashCode(event.getBlockPlaced().getX(),
                                     event.getBlockPlaced().getY(),
                                     event.getBlockPlaced().getZ());
        String name = event.getBlockPlaced().getWorld().getName();
 
        plugin.bucketBlocks.get(name).remove(hash);
        plugin.saveSet();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
    {
        if (event.isCancelled())
            return;

        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        long hash = Util.GetHashCode(block.getX(), block.getY(), block.getZ());

        plugin.bucketBlocks.get(block.getWorld().getName()).add(hash);
        plugin.saveSet();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBucketFill(PlayerBucketFillEvent event)
    {
        if (event.isCancelled())
            return;

        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        long hash = Util.GetHashCode(block.getX(), block.getY(), block.getZ());

        plugin.bucketBlocks.get(block.getWorld().getName()).remove(hash);
        plugin.saveSet();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.log.info(event.getWorld().getName() + " loaded");
        String name = event.getWorld().getName();
        if (!plugin.bucketBlocks.containsKey(name)) {
            plugin.bucketBlocks.put(name, new TreeSet<Long>());
            plugin.saveSet();
        }
    }
}
