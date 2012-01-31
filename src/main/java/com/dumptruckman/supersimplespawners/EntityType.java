package com.dumptruckman.supersimplespawners;

import org.bukkit.entity.CreatureType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * THIS SHOULD NOT BE NECESSARY. :(
 */
public enum EntityType {
    CREEPER((short) 50, CreatureType.CREEPER),
    SKELETON((short) 51, CreatureType.SKELETON),
    SPIDER((short) 52, CreatureType.SPIDER),
    GIANT((short) 53, CreatureType.GIANT),
    ZOMBIE((short) 54, CreatureType.ZOMBIE),
    SLIME((short) 55, CreatureType.SLIME),
    GHAST((short) 56, CreatureType.GHAST),
    PIG_ZOMBIE((short) 57, CreatureType.PIG_ZOMBIE),
    ENDERMAN((short) 58, CreatureType.ENDERMAN),
    CAVE_SPIDER((short) 59, CreatureType.CAVE_SPIDER),
    SILVERFISH((short) 60, CreatureType.SILVERFISH),
    BLAZE((short) 61, CreatureType.BLAZE),
    MAGMA_CUBE((short) 62, CreatureType.MAGMA_CUBE),
    ENDER_DRAGON((short) 63, CreatureType.ENDER_DRAGON),

    PIG((short) 90, CreatureType.PIG),
    SHEEP((short) 91, CreatureType.SHEEP),
    COW((short) 92, CreatureType.COW),
    CHICKEN((short) 93, CreatureType.CHICKEN),
    SQUID((short) 94, CreatureType.SQUID),
    WOLF((short) 95, CreatureType.WOLF),
    MUSHROOM_COW((short) 96, CreatureType.MUSHROOM_COW),
    SNOWMAN((short) 97, CreatureType.SNOWMAN),
    //OCELOT(98, CreatureType.OCELOT),
    VILLAGER((short) 120, CreatureType.VILLAGER);

    private final short id;
    private final CreatureType type;

    private static final Map<Short, EntityType> ID_MAP =
            new HashMap<Short, EntityType>();
    private static final Map<CreatureType, EntityType> TYPE_MAP =
            new HashMap<CreatureType, EntityType>();

    EntityType(final short id, final CreatureType type) {
        this.id = id;
        this.type = type;
    }

    static {
        for (EntityType e : EnumSet.allOf(EntityType.class)) {
            ID_MAP.put(e.getId(), e);
            TYPE_MAP.put(e.getType(), e);
        }
    }

    public final short getId() {
        return id;
    }

    public final CreatureType getType() {
        return type;
    }

    public static EntityType valueOf(final short id) {
        return ID_MAP.get(id);
    }

    public static EntityType valueOf(final CreatureType type) {
        return TYPE_MAP.get(type);
    }
}
