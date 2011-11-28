package nu.nerd.SafeBuckets;

import java.util.ArrayList;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerListener;


public class SafeBucketsPlayerListener extends PlayerListener
{
    private final SafeBuckets plugin;
    private ArrayList<String> firstLogin = new ArrayList<String> ();

    public SafeBucketsPlayerListener(SafeBuckets instance)
    {
        plugin = instance;
    }

    @Override
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled())
            return;

        Block b = event.getBlockClicked().getRelative(event.getBlockFace());

        //make the block stationary
        if (event.getBucket() == Material.WATER_BUCKET) {
            b.setTypeIdAndData(9, (byte)0xFF, false);
        }
        if (event.getBucket() == Material.LAVA_BUCKET) {
            b.setTypeIdAndData(11, (byte)0xFF, false);
        }

        //empty the bucket
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE)
            event.getItemStack().setType(Material.BUCKET);

        //cancel the event so the normal liquid doesn't get placed
        event.setCancelled(true);
    }
}
