package nu.nerd.SafeBuckets;

public class Util {
    private static final long end = (long)Math.pow(2, 28) - 1;

    private static long FixXZ(int n)
    {
        return (long)(n + 32000000) & 0x0fffffff;
    }

    public static int[] ConvertOldToNew(long hash)
    {
        int[] coords = new int[3];

        coords[0] = (int)((hash >> 28) & 0x0fffffff);
        coords[1] = (int)((hash >> 56) & 0xff);
        coords[2] = (int)(hash & 0x0fffffff);

        return coords;
    }

    public static long GetHashCode(int x, int y, int z)
    {
        return (FixXZ(x) << 28) | (((long)y & 0xff) << 56) | FixXZ(z);
    }
}
