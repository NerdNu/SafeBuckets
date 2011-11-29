package nu.nerd.SafeBuckets;

public class Util {
    private static final long end = (long)Math.pow(2, 28) - 1;

    public static long GetHashCode(int x, int y, int z)
    {
        return ((long)x << 28) | (((long)y & 0xff) << 56) | ((long)z & end);
    }
}
