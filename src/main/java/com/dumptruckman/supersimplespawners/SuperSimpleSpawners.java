package com.dumptruckman.supersimplespawners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * EGG PLACE SPAWNER.  SPAWNER DROP EGG.
 */
public class SuperSimpleSpawners extends JavaPlugin implements Listener {

    /**
     * Spawn Egg item id.
     */
    private static final int SPAWN_EGG = 383;
    /**
     * Player's reach in blocks.
     */
    private static final int PLAYER_REACH = 5;

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
     * Permission map for placing specific spawners.
     */
    private static final Map<EntityType, Permission> PLACE_SPECIFIC = new HashMap<EntityType, Permission>();

    /**
     * Permission map for dropping specific spawn eggs.
     */
    private static final Map<EntityType, Permission> DROP_SPECIFIC = new HashMap<EntityType, Permission>();

    @Override
    public final void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        registerPermissions();
        this.getLogger().info("enabled.");
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
    }

    /**
     * Called when a player left or right clicks.
     *
     * @param event The event for said left or right clicks.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void playerInteract(final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.isCancelled()
                || !event.hasItem()
                || event.getItem().getTypeId() != SPAWN_EGG
                || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        ItemStack itemInHand = player.getItemInHand();
        EntityType entityType = EntityType.fromId(itemInHand.getDurability());
        if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
            return;
        }
        if (!player.hasPermission(PLACE_SPECIFIC.get(entityType))) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        Block targetBlock = player.getTargetBlock(null, PLAYER_REACH);
        if (targetBlock.getType().equals(Material.AIR)
                || !event.getBlockFace().equals(BlockFace.UP)) {
            return;
        }
        Location playerLocation = player.getLocation();
        if (playerLocation.getWorld().getBlockAt(
                playerLocation.getBlockX(),
                playerLocation.getBlockY() - 1,
                playerLocation.getBlockZ()).equals(targetBlock)) {
            return;
        }
        Block placedBlock = targetBlock.getWorld().getBlockAt(
                targetBlock.getX(),
                targetBlock.getY() + 1,
                targetBlock.getZ());
        BlockState previousState = placedBlock.getState();
        placedBlock.setType(Material.MOB_SPAWNER);
        CreatureSpawner spawner = (CreatureSpawner) placedBlock.getState();
        spawner.setSpawnedType(entityType);
        BlockPlaceEvent bpEvent = new BlockPlaceEvent(placedBlock, previousState, targetBlock,
                new ItemStack(Material.MOB_SPAWNER, 1, entityType.getTypeId()),
                player, canBuild(player, placedBlock.getX(), placedBlock.getZ()));
        Bukkit.getPluginManager().callEvent(bpEvent);
        if (bpEvent.isCancelled()) {
            previousState.update(true);
            return;
        }
        spawner.update();
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
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        EntityType entityType = spawner.getSpawnedType();
        if (!event.getPlayer().hasPermission(DROP_SPECIFIC.get(entityType))) {
            return;
        }
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
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
