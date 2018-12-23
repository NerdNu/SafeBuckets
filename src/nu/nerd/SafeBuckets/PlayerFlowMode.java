package nu.nerd.SafeBuckets;

// ------------------------------------------------------------------------
/**
 * The player self-flow mode possibilities.
 */
enum PlayerFlowMode {

    // ------------------------------------------------------------------------
    /**
     * Players will only be able to flow liquids in WorldGuard regions they own.
     */
    OWNER,

    // ------------------------------------------------------------------------
    /**
     * Players will only be able to flow liquids in WorldGuard regions of which they are a member.
     */
    MEMBER

}
