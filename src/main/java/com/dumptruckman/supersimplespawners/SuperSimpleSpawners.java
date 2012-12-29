package com.dumptruckman.supersimplespawners;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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
            PermissionDefault.OP);

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
     * Permission map for placing specific spawners.
     */
    private static final Map<EntityType, Permission> PLACE_SPECIFIC = new HashMap<EntityType, Permission>();

    /**
     * Permission map for dropping specific spawn eggs.
     */
    private static final Map<EntityType, Permission> DROP_SPECIFIC = new HashMap<EntityType, Permission>();

    @Override
    public final void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        registerPermissions();
        getLogger().info("enabled.");
    }

    private void registerPermissions() {
        PluginManager pm = this.getServer().getPluginManager();

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
        CAN_DROP.addParent(ALL_PERMS, true);
        CAN_PLACE.addParent(ALL_PERMS, true);
        pm.addPermission(CAN_PLACE);
        pm.addPermission(CAN_DROP);
        pm.addPermission(ALL_PERMS);
        pm.addPermission(SILK_TOUCH);
    }

    /**
     * Called when a player left or right clicks.
     *
     * @param event The event for said left or right clicks.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public final void playerInteract(final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // Check if this is an event the plugin should be interested in, a right click with a
        // spawn egg, if it isn't stop here.
        if (!event.hasItem()
                || event.getItem().getTypeId() != SPAWN_EGG
                || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || !event.hasBlock()) {
            return;
        }
        ItemStack itemInHand = player.getItemInHand();
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
        Block targetBlock = event.getClickedBlock();
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
        Block placedBlock;
        if (REPLACEABLE_BLOCKS.contains(targetBlock.getType())) {
            placedBlock = targetBlock;
        } else {
            placedBlock = targetBlock.getRelative(event.getBlockFace());
        }

        // Ensure that the player is not placing the block in themselves.
        Block playerLocation = player.getLocation().getBlock();
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
        BlockState previousState = placedBlock.getState();
        // Replace the placed block with a spawner cage.
        placedBlock.setType(Material.MOB_SPAWNER);
        CreatureSpawner spawner = (CreatureSpawner) placedBlock.getState();
        // We're going to change the item type to a monster spawner so it looks like that's what the
        // player is placing for the fake block place event.
        itemInHand.setType(Material.MOB_SPAWNER);
        // Create a block place event for compatibility, then call it.
        BlockPlaceEvent bpEvent = new BlockPlaceEvent(placedBlock, previousState, targetBlock,
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
        Block block = event.getBlock();
        if (event.isCancelled()
                || !block.getType().equals(Material.MOB_SPAWNER)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(SILK_TOUCH)) {
            ItemStack itemHeld = player.getItemInHand();
            if (itemHeld == null || !itemHeld.containsEnchantment(Enchantment.SILK_TOUCH)) {
                return;
            }
        }
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        EntityType entityType = spawner.getSpawnedType();
        if (!player.hasPermission(DROP_SPECIFIC.get(entityType))) {
            return;
        }
        if (player.getGameMode() == GameMode.SURVIVAL) {
            ItemStack spawnEgg = new ItemStack(SPAWN_EGG, 1, entityType.getTypeId());
            block.getWorld().dropItemNaturally(block.getLocation(),
                    spawnEgg);
        }
        block.setTypeId(0, true);
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
        World world = player.getWorld();

        if (!world.equals(Bukkit.getWorlds().get(0))) return true;
        if (spawnSize <= 0) return true;
        if (player.isOp()) return true;

        Chunk chunkcoordinates = player.getLocation().getChunk();

        int distanceFromSpawn = (int) Math.max(Math.abs(x - chunkcoordinates.getX()),
                Math.abs(z - chunkcoordinates.getZ()));
        return distanceFromSpawn > spawnSize;
    }
}
