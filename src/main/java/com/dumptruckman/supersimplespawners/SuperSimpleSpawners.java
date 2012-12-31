package com.dumptruckman.supersimplespawners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * EGG PLACE SPAWNER.  SPAWNER DROP EGG.
 */
public class SuperSimpleSpawners extends JavaPlugin implements Listener {

    /**
     * Spawn Egg item id.
     */
    private static final int SPAWN_EGG = 383;

    /**
     * Contains a set of non-solid blocks, which you cannot
     * place blocks on, but you can place blocks in.
     */
    private static final Set<Material> NON_SOLID_BLOCKS = EnumSet.noneOf(Material.class);

    /**
     * Contains a set of blocks that are valid to place on
     * and are REPLACED instead of placed on.
     */
    private static final Set<Material> REPLACEABLE_BLOCKS = EnumSet.noneOf(Material.class);

    /**
     * Contains a set of MaterialData classes that indicate
     * block types that are interactive and cannot be placed on.
     */
    private static final Set<Material> INTERACTIVE_MATERIALS = EnumSet.noneOf(Material.class);

    static {
        NON_SOLID_BLOCKS.add(Material.AIR);
        NON_SOLID_BLOCKS.add(Material.WATER);
        NON_SOLID_BLOCKS.add(Material.STATIONARY_WATER);
        NON_SOLID_BLOCKS.add(Material.LAVA);
        NON_SOLID_BLOCKS.add(Material.STATIONARY_LAVA);

        REPLACEABLE_BLOCKS.add(Material.SNOW);
        REPLACEABLE_BLOCKS.add(Material.LONG_GRASS);
        REPLACEABLE_BLOCKS.add(Material.FIRE);
        REPLACEABLE_BLOCKS.add(Material.VINE);

        INTERACTIVE_MATERIALS.add(Material.ANVIL);
        INTERACTIVE_MATERIALS.add(Material.BEACON);
        INTERACTIVE_MATERIALS.add(Material.BED_BLOCK);
        INTERACTIVE_MATERIALS.add(Material.BREWING_STAND);
        INTERACTIVE_MATERIALS.add(Material.BURNING_FURNACE);
        INTERACTIVE_MATERIALS.add(Material.CAKE_BLOCK);
        INTERACTIVE_MATERIALS.add(Material.COMMAND);
        INTERACTIVE_MATERIALS.add(Material.CHEST);
        INTERACTIVE_MATERIALS.add(Material.DIODE_BLOCK_OFF);
        INTERACTIVE_MATERIALS.add(Material.DIODE_BLOCK_ON);
        INTERACTIVE_MATERIALS.add(Material.DISPENSER);
        INTERACTIVE_MATERIALS.add(Material.ENCHANTMENT_TABLE);
        INTERACTIVE_MATERIALS.add(Material.ENDER_CHEST);
        INTERACTIVE_MATERIALS.add(Material.FENCE_GATE);
        INTERACTIVE_MATERIALS.add(Material.FURNACE);
        INTERACTIVE_MATERIALS.add(Material.IRON_DOOR_BLOCK);
        INTERACTIVE_MATERIALS.add(Material.JUKEBOX);
        INTERACTIVE_MATERIALS.add(Material.LEVER);
        INTERACTIVE_MATERIALS.add(Material.NOTE_BLOCK);
        INTERACTIVE_MATERIALS.add(Material.STONE_BUTTON);
        INTERACTIVE_MATERIALS.add(Material.TRAP_DOOR);
        INTERACTIVE_MATERIALS.add(Material.WOOD_BUTTON);
        INTERACTIVE_MATERIALS.add(Material.WOODEN_DOOR);
        INTERACTIVE_MATERIALS.add(Material.WORKBENCH);
    }

    /**
     * Permission to be able to place spawn eggs as spawners.
     */
    private static final Permission ALL_PERMS = new Permission(
            "sss.*",
            "Gives all permissions for this plugin.",
            PermissionDefault.FALSE);

    /**
     * Permission to be able to place spawn eggs as spawners.
     */
    private static final Permission CAN_PLACE = new Permission(
            "sss.place.*",
            "Spawn eggs place spawners.",
            PermissionDefault.FALSE);
    /**
     * Permission to have spawners drop as spawn eggs.
     */
    private static final Permission CAN_DROP = new Permission(
            "sss.drop.*",
            "Spawners drop spawn eggs",
            PermissionDefault.FALSE);

    /**
     * Permission to have spawners drop as spawn eggs.
     */
    private static final Permission SILK_TOUCH = new Permission(
            "sss.silk_touch",
            "Forces the player to have silk touch enchant to get drops from spawners",
            PermissionDefault.FALSE);

    /**
     * Permission to use the reload command.
     */
    private static final Permission RELOAD_COMMAND = new Permission(
            "sss.reload",
            "Allows the use of the supersimplespawnersreload command.",
            PermissionDefault.OP);

    /**
     * Permission map for placing specific spawners.
     */
    private static final Map<EntityType, Permission> PLACE_SPECIFIC = new HashMap<EntityType, Permission>();

    /**
     * Permission map for dropping specific spawn eggs.
     */
    private static final Map<EntityType, Permission> DROP_SPECIFIC = new HashMap<EntityType, Permission>();

    /**
     * Config key for explosion drops.
     */
    private static final String EXPLOSION_DROP_KEY = "drop_egg_on_spawner_explosion";

    @Override
    public final void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        ensureConfigPrepared();
        registerPermissions();
        // No log message is needed here, Bukkit already tells the server that it is enabling.
    }

    private void ensureConfigPrepared() {
        // Check if a config file exists, if not, copy the default from the jar.
        // This will not overwrite it if it already exists.
        saveDefaultConfig();
        // Lets set defaults for all the config values in case the config file exists but the user erased the contents.
        getConfig().addDefaults(YamlConfiguration.loadConfiguration(getResource("config.yml")));
        // And then copy the key-value pairs that may not exist.
        getConfig().options().copyDefaults(true);
        getConfig().options().header("The " + EXPLOSION_DROP_KEY + " setting tells the plugin whether or not to drop spawn eggs when a spawner is blown up by an explosion.");
        // We need to save the config file in order to apply the previous to changes.
        saveConfig();
    }

    private void registerPermissions() {
        final PluginManager pm = this.getServer().getPluginManager();

        // Let's add child permissions to our drop/place permissions for each entity type.
        for (EntityType entityType : EntityType.values()) {
            if (entityType.isAlive() && entityType.isSpawnable()) {
                String name = entityType.getName().toLowerCase();

                Permission drop = new Permission("sss.drop." + name,
                        "Spawners drop spawn eggs for " + name, PermissionDefault.FALSE);
                drop.addParent(CAN_DROP, true);
                pm.addPermission(drop);
                DROP_SPECIFIC.put(entityType, drop);

                Permission place = new Permission("sss.place." + name,
                        "Spawn eggs place spawners for " + name, PermissionDefault.FALSE);
                place.addParent(CAN_PLACE, true);
                pm.addPermission(place);
                PLACE_SPECIFIC.put(entityType, place);
            }
        }
        // Add the drop/place permission to the global parent permission. (sss.*)
        CAN_DROP.addParent(ALL_PERMS, true);
        CAN_PLACE.addParent(ALL_PERMS, true);
        RELOAD_COMMAND.addParent(ALL_PERMS, true);
        // Register all of our permissions.
        pm.addPermission(CAN_PLACE);
        pm.addPermission(CAN_DROP);
        pm.addPermission(SILK_TOUCH);
        pm.addPermission(RELOAD_COMMAND);
        pm.addPermission(ALL_PERMS);
    }

    /**
     * Retrieves a new {@link #SPAWN_EGG} type ItemStack based on the given entity type.
     *
     * @param entityType The entity type a spawn egg is needed for.
     * @return A new spawn egg item.
     */
    private static ItemStack getSpawnEgg(final EntityType entityType) {
        return new MaterialData(SPAWN_EGG, (byte)entityType.getTypeId()).toItemStack(1);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        // We only have one command and it does one simple thing.
        // As such we don't need to do any argument checking or anything else.
        // This method is only going to be called for our single command and that's it!
        reloadConfig();
        sender.sendMessage(ChatColor.AQUA + "=== SuperSimpleSpawners config reloaded ===");
        return true;
    }

    /**
     * Called when a player left or right clicks.
     *
     * @param event The event for said left or right clicks.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public final void playerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        // Check if this is an event the plugin should be interested in, a right click with a
        // spawn egg, if it isn't stop here.
        if (!event.hasItem()
                || event.getItem().getTypeId() != SPAWN_EGG
                || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || !event.hasBlock()) {
            return;
        }
        final ItemStack itemInHand = player.getItemInHand();
        EntityType entityType = EntityType.fromId(itemInHand.getDurability());

        // Check if the Metadata on the egg being placed is valid for a spawner, if it
        // isn't stop here.
        if (entityType == null
                || !entityType.isAlive()
                || !entityType.isSpawnable()) {
            return;
        }
        // Check if the user has the permission to place the egg they're attempting to
        // place based on sss.place.entity_type, if not stop here.
        if (!player.hasPermission(PLACE_SPECIFIC.get(entityType))) {
            return;
        }

        // Get the block that the player is attempting to place on and ensure
        // that the block is valid
        final Block targetBlock = event.getClickedBlock();
        if (NON_SOLID_BLOCKS.contains(targetBlock.getType())
                || targetBlock.getState() instanceof InventoryHolder) {
            return;
        }
        
        // Get the block that the player is attempting to place on and ensure
        // that the block isn't interactive, if it is then use the block
        // example: workbench
        // if the block is interactive and the player is sneaking, place like normal
        if (INTERACTIVE_MATERIALS.contains(targetBlock.getType()) && !player.isSneaking()) {
            return;
        }

        // Prevent the normal spawn egg behaviours
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);

        // Figure out which side of the block was clicked on and use that to determine
        // where to place the spawner.  However, if the block is a type that normally gets
        // replaced when you place on it, we'll use that position.
        final Block placedBlock;
        if (REPLACEABLE_BLOCKS.contains(targetBlock.getType())) {
            placedBlock = targetBlock;
        } else {
            placedBlock = targetBlock.getRelative(event.getBlockFace());
        }

        // Ensure that the player is not placing the block in themselves.
        final Block playerLocation = player.getLocation().getBlock();
        if (playerLocation.getRelative(0, -1, 0).equals(placedBlock)
                || playerLocation.equals(placedBlock)) {
            return;
        }

        // Select the block we should be changing into a spawner and ensure it's air,
        // water, or lava which are blocks you can normally put items in.
        if (!NON_SOLID_BLOCKS.contains(placedBlock.getType())
                && !REPLACEABLE_BLOCKS.contains(placedBlock.getType())) {
            return;
        }
        // Save the previous state of the block being manipulated, in case the fake block place
        // event we throw is canceled.
        final BlockState previousState = placedBlock.getState();
        // Replace the placed block with a spawner cage.
        placedBlock.setType(Material.MOB_SPAWNER);
        final CreatureSpawner spawner = (CreatureSpawner) placedBlock.getState();
        // We're going to change the item type to a monster spawner so it looks like that's what the
        // player is placing for the fake block place event.
        itemInHand.setType(Material.MOB_SPAWNER);
        // Create a block place event for compatibility, then call it.
        final BlockPlaceEvent bpEvent = new BlockPlaceEvent(placedBlock, previousState, targetBlock,
                itemInHand, player, canBuild(player, placedBlock.getX(), placedBlock.getZ()));
        Bukkit.getPluginManager().callEvent(bpEvent);
        // Now we'll switch that monster spawner back to spawn eggs so the player sees no change
        if (itemInHand.getType() == Material.MOB_SPAWNER) {
            itemInHand.setType(Material.MONSTER_EGG);
        }
        // If the block place event was cancelled, the item was changed to something other than
        // a spawner, or the player is trying to place in spawn protection we need to stop here.
        if (bpEvent.isCancelled()
                || itemInHand.getType() != Material.MONSTER_EGG
                || !bpEvent.canBuild()) {
            // We'll revert the block back to what it was by updating the previous state.
            previousState.update(true);
            return;
        }
        entityType = EntityType.fromId(itemInHand.getDurability());
        // We need to ensure that the item in their hand is still a valid monster spawner egg.
        if (entityType == null
                || !entityType.isAlive()
                || !entityType.isSpawnable()) {
            // We'll revert the block back to what it was by updating the previous state.
            previousState.update(true);
            return;
        }
        // Update the spawner to spawn the entity type of the egg held in hand.
        spawner.setSpawnedType(entityType);
        spawner.update(true);
        // Remove one spawn egg from the players hand if they're not in creative mode.
        if (placedBlock.getState() instanceof CreatureSpawner
                && player.getGameMode().equals(GameMode.SURVIVAL)) {
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInHand(null);
            }
            player.updateInventory();
        }
    }

    /**
     * Called when a player breaks a block, successful or not.
     *
     * @param event The event for said breakage.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void blockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (event.isCancelled()
                || !block.getType().equals(Material.MOB_SPAWNER)) {
            return;
        }
        final Player player = event.getPlayer();
        if (player.hasPermission(SILK_TOUCH)) {
            final ItemStack itemHeld = player.getItemInHand();
            if (itemHeld == null || !itemHeld.containsEnchantment(Enchantment.SILK_TOUCH)) {
                return;
            }
        }
        final CreatureSpawner spawner = (CreatureSpawner) block.getState();
        final EntityType entityType = spawner.getSpawnedType();
        if (!player.hasPermission(DROP_SPECIFIC.get(entityType))) {
            return;
        }
        if (player.getGameMode() == GameMode.SURVIVAL) {
            final ItemStack spawnEgg = getSpawnEgg(entityType);
            final Location blockLocation = block.getLocation();
            blockLocation.getWorld().dropItemNaturally(blockLocation, spawnEgg);
        }
        block.setTypeId(0, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public final void onEntityExplode(final EntityExplodeEvent event) {
        // If explosion drops is enabled in the config when spawners are destroyed by explosions
        // the correct egg for that spawner will drop.
        if (getConfig().getBoolean(EXPLOSION_DROP_KEY)) {
            for (final Block block : event.blockList()) {
                if (block.getState() instanceof CreatureSpawner) {
                    final CreatureSpawner spawner = (CreatureSpawner) block.getState();
                    ItemStack spawnEgg = getSpawnEgg(spawner.getSpawnedType());

                    final Location blockLocation = block.getLocation();
                    blockLocation.getWorld().dropItemNaturally(blockLocation, spawnEgg);
                }
            }
        }
    }

    /**
     * Modified canBuild from CraftBukkit.  Basically checks to make sure they're not within spawn protection.
     *
     * @param player The player to check for build access
     * @param x The x coord of the placed block
     * @param z The y coord of the placed block
     * @return true if player is not trying to build in spawn radius or is op.
     */
    private static boolean canBuild(Player player, int x, int z) {
        int spawnSize = Bukkit.getServer().getSpawnRadius();
        final World world = player.getWorld();

        if (!world.equals(Bukkit.getWorlds().get(0))) {
            return true;
        }
        if (spawnSize <= 0) {
            return true;
        }
        if (Bukkit.getServer().getOperators().isEmpty()) {
            return true;
        }
        if (player.isOp()) {
            return true;
        }

        final Chunk chunkCoordinates = player.getLocation().getChunk();

        final int distanceFromSpawn = (int) Math.max(Math.abs(x - chunkCoordinates.getX()),
                Math.abs(z - chunkCoordinates.getZ()));
        return distanceFromSpawn > spawnSize;
    }
}
