package com.bigenergy.ftbqopt.util;

import com.bigenergy.ftbqopt.mixin.accessor.DeferredInventoryDetectionAccessor;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple debouncer: does not set repeated checks while one is already pending.
 * Removing the flag happens in our detect(...) mixin via markCompleted(player).
 */
public final class DetectionDebouncer {
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private DetectionDebouncer() {}

    public static void scheduleIfNotQueued(ServerPlayer player, int delayTicks) {
        if (PENDING.add(player.getUUID())) {
            try {
                DeferredInventoryDetectionAccessor.ftbq$invokeScheduleInventoryCheck(player, delayTicks);
            } catch (Throwable t) {
                // Fallback: if suddenly Invoker is not mixed, we will remove the flag and not spam the log.
                PENDING.remove(player.getUUID());
                throw t;
            }
        }
    }

    public static void markCompleted(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }
}
