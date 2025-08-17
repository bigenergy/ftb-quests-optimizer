package com.bigenergy.ftbqopt.mixin;

import com.bigenergy.ftbqopt.config.FTBQuestsOptimizerConfig;
import com.bigenergy.ftbqopt.mixin.accessor.DeferredInventoryDetectionAccessor;
import com.bigenergy.ftbqopt.util.DetectionDebouncer;
import com.bigenergy.ftbqopt.util.LastSeenSlotCache;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
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
        // Снимаем "в очереди" только если дебаунс включён
        if (FTBQuestsOptimizerConfig.DEBOUNCE.get()) {
            DetectionDebouncer.markCompleted(player);
        }

        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || PlayerHooks.isFake(player)) { ci.cancel(); return; }

        final boolean craftingPath = !craftedItem.isEmpty();
        List<Task> tasksToCheck = craftingPath ? file.getCraftingTasks() : file.getSubmitTasks();
        if (tasksToCheck.isEmpty()) { ci.cancel(); return; }

        FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
            TeamData data = file.getNullableTeamData(team.getId());
            if (data == null || data.isLocked()) return;

            // Предфильтр задач
            ArrayList<Task> candidates = new ArrayList<>(tasksToCheck.size());
            for (Task t : tasksToCheck) {
                if (t.id != sourceTask && data.canStartTasks(t.getQuest())) {
                    candidates.add(t);
                }
            }
            if (candidates.isEmpty()) return;

            file.withPlayerContext(player, () -> {
                // дорогой скан инвентаря нужен только для submit-пути
                if (!craftingPath) {
                    dev.ftb.mods.ftbquests.util.PlayerInventorySummary.build(player);
                }
                for (Task task : candidates) {
                    task.submitTask(data, player, craftedItem);
                }
            });
        });

        ci.cancel(); // полностью заменили оригинал
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
        int mainSize = this.player.getInventory().items.size(); // main+hotbar
        if (slotNum < 0 || slotNum >= mainSize) { ci.cancel(); return; }

        // 1) Игнорируем NBT-only «шум» (тот же item + count)
        if (FTBQuestsOptimizerConfig.IGNORE_NBT_ONLY.get()
                && LastSeenSlotCache.isSameItemCount(this.player, slotNum, stack)) {
            ci.cancel();
            return;
        }
        LastSeenSlotCache.update(this.player, slotNum, stack);

        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) { ci.cancel(); return; }

        // 2) Задержка: override из конфига или FTBQ
        int ftbqDelay = Mth.clamp(file.getDetectionDelay(), 0, 200);
        int cfg = FTBQuestsOptimizerConfig.DELAY_OVERRIDE.get();
        int delay = cfg >= 0 ? cfg : ftbqDelay;

        // Немедленный путь FTBQ при delay == 0
        if (delay == 0) {
            FTBQuestsInventoryListener.detect(this.player, ItemStack.EMPTY, 0L);
            ci.cancel();
            return;
        }

        if (FTBQuestsOptimizerConfig.DEBOUNCE.get()) {
            DetectionDebouncer.scheduleIfNotQueued(this.player, delay); // использует Invoker внутри
        } else {
            DeferredInventoryDetectionAccessor.ftbq$invokeScheduleInventoryCheck(this.player, delay);
        }

        ci.cancel();
    }
}
