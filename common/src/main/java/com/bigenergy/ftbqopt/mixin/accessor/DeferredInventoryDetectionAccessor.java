package com.bigenergy.ftbqopt.mixin.accessor;

import dev.ftb.mods.ftbquests.util.DeferredInventoryDetection;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Access to package-private method via Invoker.
 */
@Mixin(DeferredInventoryDetection.class)
public interface DeferredInventoryDetectionAccessor {
    @Invoker("scheduleInventoryCheck")
    static void ftbq$invokeScheduleInventoryCheck(ServerPlayer player, int delayTicks) {
        throw new AssertionError("Mixin did not apply");
    }
}
