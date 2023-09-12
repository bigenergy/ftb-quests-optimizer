package com.bigenergy.ftbquestsoptimizer.mixins;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FTBQuestsInventoryListener.class, remap = false)
public class FTBQuestsInventoryListenerMixin {

    @Shadow
    public final ServerPlayer player;

    private int ticksSkipped;

    public FTBQuestsInventoryListenerMixin(ServerPlayer player) {
        this.player = player;
    }

    private boolean tryTick()
    {
        int skipTicksAmount = 5;
        if (skipTicksAmount <= 0)
            return true;

        this.ticksSkipped++;
        if (this.ticksSkipped > skipTicksAmount)
        {
            this.ticksSkipped = 0;
            return true;
        }

        return false;
    }

//    @Inject(
//            method = "detect",
//            at={@At(value="HEAD")}
//    )
//    private static void gpu$fixFTBQLags(ServerPlayer player, ItemStack craftedItem, long sourceTask, CallbackInfo ci) {
//        if (!tryTick()) {
//            return;
//        }
//    }

    /**
     * @author Big_Energy
     * @reason performance fix
     */
    @Overwrite
    public static void detect(ServerPlayer player, ItemStack craftedItem, long sourceTask) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;

        if (file == null || PlayerHooks.isFake(player)) {
            return;
        }

        TeamData data = file.getNullableTeamData(FTBTeamsAPI.getPlayerTeamID(player.getUUID()));

        if (data == null || data.isLocked()) {
            return;
        }

        file.withPlayerContext(player, () -> file.getSubmitTasks().stream() // WARN: NOT USE ASYNC! -> SERVER CRASH EVERY 5 MIN
                .filter(task -> task.id != sourceTask && data.canStartTasks(task.quest))
                .forEach(task -> task.submitTask(data, player, craftedItem)));
    }


    public void slotChanged(AbstractContainerMenu container, int index, ItemStack stack) {
        if (!this.tryTick()) {
            return;
        }
        if (!stack.isEmpty() && container.getSlot(index).container == player.getInventory()) {
            detect(player, ItemStack.EMPTY, 0);
        }
    }

}
