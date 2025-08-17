package com.bigenergy.ftbqopt.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Slot "last state" cache: compare only Item+count,
 * to ignore NBT oscillations of noisy items.
 */
public final class LastSeenSlotCache {
    private static final Map<UUID, Entry[]> CACHE = new WeakHashMap<>();

    private LastSeenSlotCache() {}

    public static boolean isSameItemCount(ServerPlayer player, int slot, ItemStack current) {
        Entry[] arr = CACHE.computeIfAbsent(player.getUUID(), id -> new Entry[64]); // запас слотов
        Entry e = arr[slot];
        Item item = current.getItem();
        int count = current.getCount();
        return e != null && e.item == item && e.count == count;
    }

    public static void update(ServerPlayer player, int slot, ItemStack current) {
        Entry[] arr = CACHE.computeIfAbsent(player.getUUID(), id -> new Entry[64]);
        Entry e = arr[slot];
        if (e == null) {
            e = new Entry();
            arr[slot] = e;
        }
        e.item = current.getItem();
        e.count = current.getCount();
    }

    private static final class Entry {
        Item item;
        int count;
    }
}
