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


Commands
--------
 * `/flow` - Toggle "flow mode" on or off. When on, a player can click a liquid block to make it flow in regions where they have build permission, or regions they own, depending on the configuration.
 * `/sb reload` - Reload the SafeBuckets configuration.
 * `/sb [safe|unsafe [water|lava]]`
   * Convert the player's empty hotbar slot or some kind of bucket (empty,
     water or lava) into a bucket with the specified safety and liquid type.
     If the arguments do not specify safety, turn an empty slot or empty bucket
     into an unsafe water bucket, or invert the safety of a bucket that contains
     water or lava. If the slot is not empty or a valid bucket, do not give
     the player any item. If the liquid is not specified, it defaults
     to water.
 * `/sb flowsel` - Flow all safe liquid blocks in the current WorldEdit selection.
 * `/sb reload` - Reload the SafeBuckets configuration.


Configuration
-------------
| Setting | Description |
| :--- | :--- |
| `debug.console` | If true, debug message messages are sent to the console. |
| `debug.players` | If true, debug message messages are sent to players with the `safebuckets.debug` permissions.|
| `tool.block` | The material type of the inspector block (defaults to LAPIS_ORE). |
| `tool.item` | The material type of the inspector item (defaults to BLAZE_ROD). |
| `bucket.enabled` | If true, players can empty buckets; otherwise, they cannot. |
| `bucket.safe` | If true, placed liquids do not flow unless placed from a special "unsafe" bucket. |
| `dispenser.enabled` | If true, dispensers can dispense liquids; otherwise, they cannot. |
| `dispenser.safe` | If true, liquids placed by dispensers do not flow unless the dispenser has been marked "unsafe". |
| `flowsel.enabled` | If true, the `/sb flowsel` sub-command can be used. |
| `flowsel.maxsize` | The maximum selection size allowed when using `/sb flowsel`. If 0 (the default), the allowed selection size is unlimited. |
| `playerflow.enabled` | If true, the `/flow` command can be used. |
| `playerflow.ownermode` | If true, players can only flow blocks in regions that they own. If false, they can also flow blocks in regions where they are a member. |


Permissions
-----------
 * `safebuckets.playerflow` - Permission to use the `/flow` command.
 * `safebuckets.tools.block` - Permission to use the SafeBuckets block inspector tool block.
 * `safebuckets.tools.item` - Permission to use the SafeBuckets inspector tool item.
 * `safebuckets.tools.unsafe` - Permission to summon an unsafe liquid bucket.
 * `safebuckets.tools.norefill` - Players with this permission do not need to refill the unsafe buckets they empty.
 * `safebuckets.flowsel` - Permission to use `/sb flowsel`.
 * `safebuckets.reload` - Permission to use `/sb reload`.
 * `safebuckets.debug` - Players with this permission receive debug messages.
