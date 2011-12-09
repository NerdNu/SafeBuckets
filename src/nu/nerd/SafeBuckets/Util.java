package nu.nerd.SafeBuckets;

public class Util {
    private static long FixXZ(int n)
    {
        return (long)(n + 32000000) & 0x0fffffff;
    }

    public static long GetHashCode(int x, int y, int z)
    {
        return (FixXZ(x) << 28) | (((long)y & 0xff) << 56) | FixXZ(z);
    }

    /* not used right now but keeping around for future reference
    public static boolean IntersectsNonBucketLava(TreeSet<Long> bucketBlocks, Entity entity)
    {
        //we only care about players
        if (!(entity instanceof CraftPlayer))
            return false;

        EntityPlayer player = ((CraftPlayer)entity).getHandle();
        AxisAlignedBB bb = player.boundingBox;
        int i = (int)Math.floor(bb.a);
        int j = (int)Math.floor(bb.d + 1.0D);
        int k = (int)Math.floor(bb.b);
        int l = (int)Math.floor(bb.e + 1.0D);
        int i1 = (int)Math.floor(bb.c);
        int j1 = (int)Math.floor(bb.f + 1.0D);

        for (int x = i; x < j; ++x) {
            for (int y = k; y < l; ++y) {
                for (int z = i1; z < j1; ++z) {
                    Material mat = entity.getWorld().getBlockAt(x, y, z).getType();
                    if (mat == Material.STATIONARY_LAVA) {
                        long hash = Util.GetHashCode(x, y, z);

                        if (!bucketBlocks.contains(hash))
                            return true;
                    }

                    if (mat == Material.LAVA || mat == Material.FIRE)
                        return true;
                }
            }
        }

        return false;
    }*/
}
