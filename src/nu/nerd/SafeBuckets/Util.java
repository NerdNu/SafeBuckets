package nu.nerd.SafeBuckets;

public class Util {
    private static final long end = (long)Math.pow(2, 28) - 1;

    private static long FixXZ(int n)
    {
        return (long)(n + 32000000) & 0x0fffffff;
    }

    public static long GetHashCode(int x, int y, int z)
    {
        return (FixXZ(x) << 28) | (((long)y & 0xff) << 56) | FixXZ(z);
    }
}
