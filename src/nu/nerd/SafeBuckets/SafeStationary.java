package nu.nerd.SafeBuckets;

import net.minecraft.server.BlockStationary;
import net.minecraft.server.Material;
import net.minecraft.server.World;

public class SafeStationary extends BlockStationary {

    SafeStationary(int id, Material mat) {
        super(id, mat);
    }

    @Override
    public void doPhysics(World world, int i, int j, int k, int l) {
        if (world.getData(i, j, k) == 0xFF)
            return;
        super.doPhysics(world, i, j, k, l);
    }
}
