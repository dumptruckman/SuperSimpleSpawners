package com.dumptruckman.supersimplespawners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
     * Player's reach in blocks.
     */
    private static final int PLAYER_REACH = 5;
    /**
     * Permission to be able to place spawn eggs as spawners.
     */
    private static final Permission CAN_PLACE = new Permission(
            "supersimplespawners.place",
            "Spawn eggs place spawners.",
            PermissionDefault.FALSE);
    /**
     * Permission to have spawners drop as spawn eggs.
     */
    private static final Permission CAN_DROP = new Permission(
            "supersimplespawners.drop",
            "Spawners drop spawn eggs",
            PermissionDefault.FALSE);

    @Override
    public final void onDisable() {
        // Display disable message/version info
        this.getLogger().info("disabled.");
    }

    @Override
    public final void onEnable() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.addPermission(CAN_PLACE);
        pm.addPermission(CAN_DROP);
        this.getLogger().info("enabled.");
    }

    /**
     * Called when a player left or right clicks.
     *
     * @param event The event for said left or right clicks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public final void playerInteract(final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.isCancelled()
                || !event.hasItem()
                || event.getItem().getTypeId() != SPAWN_EGG
                || !player.hasPermission(CAN_PLACE)
                || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        ItemStack itemInHand = player.getItemInHand();
        if (EntityType.valueOf(itemInHand.getDurability()) == null) {
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
        placedBlock.setType(Material.MOB_SPAWNER);
        CreatureSpawner spawner = (CreatureSpawner) placedBlock.getState();
        spawner.setCreatureType(EntityType.valueOf(
                itemInHand.getDurability()).getType());
        itemInHand.setTypeId(0);
    }

    /**
     * Called when a player breaks a block, successful or not.
     *
     * @param event The event for said breakage.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public final void blockBreak(final BlockBreakEvent event) {
        Block block = event.getBlock();
        if (event.isCancelled()
                || !block.getType().equals(Material.MOB_SPAWNER)
                || !event.getPlayer().hasPermission(CAN_DROP)) {
            return;
        }
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        Short entityId = EntityType.valueOf(spawner.getCreatureType()).getId();
        if (entityId == null) {
            this.getLogger().warning("Unsupported spawner type, " +
                    "nag dumptruckman to update this!");
            return;
        }
        ItemStack spawnEgg = new ItemStack(SPAWN_EGG, 1, entityId);
        block.getWorld().dropItemNaturally(block.getLocation(),
                spawnEgg);
        block.setTypeId(0, true);
    }
}
