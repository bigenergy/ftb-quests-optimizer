package com.bigenergy.ftbqopt.mixin;

import com.bigenergy.ftbqopt.config.FTBQuestsOptimizerConfig;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Optimization:
 * - Cache getValidDisplayItems() by signature (item + components + matchComponents).
 * - submitTask(): aggregated consumption (config-controlled), single addProgress(), early exit.
 */
@Mixin(value = ItemTask.class, remap = false)
public abstract class ItemTaskMixin extends Task {

    @Shadow private ItemStack itemStack;
    @Shadow private long count;
    @Shadow private ItemMatchingSystem.ComponentMatchType matchComponents;

    @Unique private List<ItemStack> ftbqopt$validCache;
    @Unique private int ftbqopt$validSig;

    public ItemTaskMixin(long id, Quest quest) {
        super(id, quest);
    }

    @Unique
    private int ftbqopt$signature() {
        int h = System.identityHashCode(itemStack.getItem());
        h = 31 * h + itemStack.getComponents().hashCode(); // 1.21+: data components вместо NBT
        h = 31 * h + matchComponents.ordinal();
        return h;
    }

    // ---- кэш валидных стэков ----
    @Inject(
            method = "readData(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("TAIL")
    )
    private void ftbqopt$invalidateOnRead(CallbackInfo ci) {
        ftbqopt$validCache = null;
    }

    @Inject(
            method = "setStackAndCount(Lnet/minecraft/world/item/ItemStack;I)Ldev/ftb/mods/ftbquests/quest/task/ItemTask;",
            at = @At("TAIL")
    )
    private void ftbqopt$invalidateOnSet(CallbackInfoReturnable<ItemTask> cir) {
        ftbqopt$validCache = null;
    }

    @Inject(method = "getValidDisplayItems", at = @At("HEAD"), cancellable = true)
    private void ftbqopt$getValidDisplayItemsCached(CallbackInfoReturnable<List<ItemStack>> cir) {
        int sig = ftbqopt$signature();
        if (ftbqopt$validCache != null && sig == ftbqopt$validSig) {
            cir.setReturnValue(ftbqopt$validCache);
            return;
        }
        List<ItemStack> res = ItemMatchingSystem.INSTANCE.getAllMatchingStacks(itemStack, getQuestFile().holderLookup());
        ftbqopt$validCache = List.copyOf(res); // иммутабельная копия
        ftbqopt$validSig = sig;
        cir.setReturnValue(ftbqopt$validCache);
    }

    // ---- агрегированное потребление под контролем конфига ----
    @Inject(
            method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ftbqopt$aggregateSubmit(TeamData teamData, ServerPlayer player, ItemStack craftedItem, CallbackInfo ci) {
        // если фича выключена — даём оригиналу работать
        if (!FTBQuestsOptimizerConfig.AGGREGATE_CONSUME.get()) return;

        ItemTask self = (ItemTask)(Object)this;

        // быстрые выходы — как в оригинале
        if (self.isTaskScreenOnly()
                || teamData.isCompleted(self)
                || (itemStack.getItem() instanceof dev.ftb.mods.ftbquests.item.MissingItem)
                || (craftedItem.getItem() instanceof dev.ftb.mods.ftbquests.item.MissingItem)
                || !teamData.canStartTasks(self.getQuest())) {
            return; // не отменяем — пусть оригинал решит
        }

        // оптимизируем только consumesResources && не из крафта
        if (!self.consumesResources() || !craftedItem.isEmpty()) return;

        long progress = teamData.getProgress(self);
        long remaining = count - progress;
        if (remaining <= 0L) { ci.cancel(); return; }

        var inv = player.getInventory().items;
        boolean changed = false;
        long taken = 0L;

        final int limit = Math.max(1, FTBQuestsOptimizerConfig.MAX_SLOTS_PER_PASS.get());
        final int maxSlots = Math.min(inv.size(), limit);

        for (int i = 0; i < maxSlots && remaining > 0; i++) {
            ItemStack s = inv.get(i);
            if (s.isEmpty() || !self.test(s)) continue;

            int canTake = (int) Math.min(s.getCount(), remaining);
            if (canTake <= 0) continue;

            s.shrink(canTake);
            if (s.isEmpty()) inv.set(i, ItemStack.EMPTY);

            changed = true;
            taken += canTake;
            remaining -= canTake;
        }

        if (taken > 0 && teamData.getFile().isServerSide()) {
            teamData.addProgress(self, taken); // ровно один вызов
            if (changed) {
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
            }
        }

        // мы целиком обработали свою ветку — оригинал не нужен
        ci.cancel();
    }
}
