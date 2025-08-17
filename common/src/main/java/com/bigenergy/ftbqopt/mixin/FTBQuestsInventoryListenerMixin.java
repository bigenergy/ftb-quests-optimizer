package com.bigenergy.ftbqopt.mixin;

import com.bigenergy.ftbqopt.util.DetectionDebouncer;
import com.bigenergy.ftbqopt.util.LastSeenSlotCache;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import dev.ftb.mods.ftbquests.util.PlayerInventorySummary;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimization:
 * 1) Rewrite static detection() method: build PlayerInventorySummary only for the submit path and only if there are candidates.
 * 2) slotChanged(): early exits, "same item+count" filter (ignoring NBT noise), scheduling debounce.
 */
@Mixin(FTBQuestsInventoryListener.class)
public abstract class FTBQuestsInventoryListenerMixin {

    @Final
    @Shadow public ServerPlayer player;

    @Inject(
            method = "detect(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;J)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private static void ftbqopt$optimizedDetect(ServerPlayer player, ItemStack craftedItem, long sourceTask, CallbackInfo ci) {
        DetectionDebouncer.markCompleted(player); // remove "in queue" if it is a deferred call

        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || PlayerHooks.isFake(player)) { ci.cancel(); return; }

        final boolean craftingPath = !craftedItem.isEmpty();
        List<Task> tasksToCheck = craftingPath ? file.getCraftingTasks() : file.getSubmitTasks();
        if (tasksToCheck.isEmpty()) { ci.cancel(); return; }

        FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
            TeamData data = file.getNullableTeamData(team.getId());
            if (data == null || data.isLocked()) return;

            // Prefilter: immediately cut off tasks that definitely won't start
            ArrayList<Task> candidates = new ArrayList<>(tasksToCheck.size());
            for (Task t : tasksToCheck) {
                if (t.id != sourceTask && data.canStartTasks(t.getQuest())) {
                    candidates.add(t);
                }
            }
            if (candidates.isEmpty()) return;

            file.withPlayerContext(player, () -> {
                // Inventory scan (expensive) is only needed for the submit path
                if (!craftingPath) {
                    PlayerInventorySummary.build(player);
                }
                for (Task task : candidates) {
                    task.submitTask(data, player, craftedItem);
                }
            });
        });

        ci.cancel();
    }

    @Inject(
            method = "slotChanged(Lnet/minecraft/world/inventory/AbstractContainerMenu;ILnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void ftbqopt$slotChanged(AbstractContainerMenu menu, int index, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty()) { ci.cancel(); return; }

        var slot = menu.getSlot(index);
        if (slot.container != this.player.getInventory()) { ci.cancel(); return; }

        int slotNum = slot.getContainerSlot();
        var inv = this.player.getInventory();
        int mainSize = inv.items.size(); // main+hotbar
        if (slotNum < 0 || slotNum >= mainSize) { ci.cancel(); return; }

        // Ignore NBT noise: item and quantity unchanged
        if (LastSeenSlotCache.isSameItemCount(this.player, slotNum, stack)) {
            ci.cancel();
            return;
        }
        LastSeenSlotCache.update(this.player, slotNum, stack);

        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) { ci.cancel(); return; }

        int delay = Mth.clamp(file.getDetectionDelay(), 0, 200);
        if (delay == 0) {
            FTBQuestsInventoryListener.detect(this.player, ItemStack.EMPTY, 0L);
        } else {
            // debounce: set the task only if it does not exist yet
            DetectionDebouncer.scheduleIfNotQueued(this.player, delay);
        }
        ci.cancel();
    }
}
