SafeBuckets
===========
SafeBuckets prevents flowing and physics events from affecting bucket-placed
liquids, so that player-placed water and lava never flow. SafeBuckets doesn't
interfere with blocks placed with actual liquid blocks, so people with access
to those them can still add normal flowing liquids.

In this documentation, non-flowing liquids are referred to as "safe", whereas
flowing liquids are "unsafe".


Inspector Item and Block
------------------------
SafeBuckets supports the use of both a configurable item and a configurable
block to query or toggle liquid flow.  By default, these inspector tools are
a blaze rod and a lapis ore block, respectively.  In order to use the inspector
tool item or block, you must have the "safebuckets.tools.item" or
"safebuckets.tools.block" permissions, respectively.

Left-clicking with a tool will toggle flow on or off at a location.  Right-
clicking will merely query whether the location has liquid flow enabled.

The location selected by the inspector tool block is different from that
selected by the tool item.  Whereas the inspector tool item queries or toggles
the block you click on, the inspector tool block queries the block *adjacent*
to the particular side of the block you click on.  When left-clicking on a
block with the inspector tool block, bear in mind that the block toggled is
not the one you are *punching*, but the *hole* that would be filled if you
were placing a block by right-clicking.


Dispenser Handling
------------------
If the plugin is configured for safe dispensers (the default) then they will
dispense non-flowing liquids, when first placed.  Dispensers can be toggled
between dispensing safe and unsafe liquids by left-clicking on them with the
inspector tool item.  It is *not* necessary to enable flow of the dispensed
liquid block; SafeBuckets will do that itself if the dispenser is set to unsafe.

