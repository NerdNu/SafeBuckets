package nu.nerd.SafeBuckets;

import org.bukkit.craftbukkit.entity.CraftEntity;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;

public class SafeBucketsEntityListener extends EntityListener {
    private final SafeBuckets plugin;

    SafeBucketsEntityListener(SafeBuckets instance)
    {
        plugin = instance;
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event)
    {
        if (event.isCancelled())
            return;

        if (!plugin.getConfig().getBoolean("block-lava-damage"))
            return;

        //block damage caused by being in player-placed lava
        if (event.getCause() == DamageCause.LAVA || event.getCause() == DamageCause.FIRE) {
            String name = event.getEntity().getWorld().getName();
            Entity entity = event.getEntity();
            if (!Util.IntersectsNonBucketLava(plugin.bucketBlocks.get(name), entity))
                event.setCancelled(true);
        }
    }

    @Override
    public void onEntityCombust(EntityCombustEvent event) {
        if (event.isCancelled())
            return;

        if (!plugin.getConfig().getBoolean("block-lava-damage"))
            return;

        String name = event.getEntity().getWorld().getName();
        Entity entity = event.getEntity();
        if (!Util.IntersectsNonBucketLava(plugin.bucketBlocks.get(name), entity)) {
            net.minecraft.server.Entity mcEntity = ((CraftEntity)entity).getHandle();
            //set fireticks so we keep getting combust events
            mcEntity.fireTicks = -20;
            event.setCancelled(true);
        }
    }
}
