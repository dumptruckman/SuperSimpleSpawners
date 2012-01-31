package com.dumptruckman.supersimplespawners;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A necessary helper class until bukkit has a way get the held
 * item slot (int).
 */
public class InventoryTools {

    private InventoryTools() { }

    /**
     * This method is necessary to remove objects more gracefully from a player's
     * inventory than the standard bukkit .remove() methods.  This will only remove
     * items if there are amount or greater available.
     *
     * @param inventory Inventory to remove from.
     * @param type Item type to remove.
     * @param durability Item data to remove.
     * @param amount Item amount to remove.
     * @return True if the item(s) were removed.
     */
    public static boolean remove(Inventory inventory, Material type, short durability, int amount) {
        HashMap<Integer, ? extends ItemStack> allItems = inventory.all(type);
        HashMap<Integer, Integer> removeFrom = new HashMap<Integer, Integer>();
        int foundAmount = 0;
        for (Map.Entry<Integer, ? extends ItemStack> item : allItems.entrySet()) {
            if (item.getValue().getDurability() == durability) {
                if (item.getValue().getAmount() >= amount - foundAmount) {
                    removeFrom.put(item.getKey(), amount - foundAmount);
                    foundAmount = amount;
                } else {
                    foundAmount += item.getValue().getAmount();
                    removeFrom.put(item.getKey(), item.getValue().getAmount());
                }
                if (foundAmount >= amount) {
                    break;
                }
            }
        }
        if (foundAmount == amount) {
            for (Map.Entry<Integer, Integer> toRemove : removeFrom.entrySet()) {
                ItemStack item = inventory.getItem(toRemove.getKey());
                if (item.getAmount() - toRemove.getValue() <= 0) {
                    inventory.clear(toRemove.getKey());
                } else {
                    item.setAmount(item.getAmount() - toRemove.getValue());
                    inventory.setItem(toRemove.getKey(), item);
                }
            }
            return true;
        }
        return false;
    }
}
