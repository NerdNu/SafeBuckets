package nu.nerd.SafeBuckets;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockFromToEvent;

public class SafeBucketsBlockListener extends BlockListener
{
    private final SafeBuckets plugin;

    SafeBucketsBlockListener(SafeBuckets instance)
    {
        plugin = instance;
    }

    @Override
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
    
    @Override
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

    @Override
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
}
