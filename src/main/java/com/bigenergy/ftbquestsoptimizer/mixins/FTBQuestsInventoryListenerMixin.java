package com.bigenergy.ftbquestsoptimizer.mixins;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(value = FTBQuestsInventoryListener.class, remap = false)
public class FTBQuestsInventoryListenerMixin {

    @Shadow
    public final ServerPlayer player;

   // private int ticksSkipped;

    public FTBQuestsInventoryListenerMixin(ServerPlayer player) {
        this.player = player;
    }

//    private boolean tryTick()
//    {
//        int skipTicksAmount = 5;
//        if (skipTicksAmount <= 0)
//            return true;
//
//        this.ticksSkipped++;
//        if (this.ticksSkipped > skipTicksAmount)
//        {
//            this.ticksSkipped = 0;
//            return true;
//        }
//
//        return false;
//    }

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

        Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player);

        if (team.isEmpty()) {
            return;
        }

        TeamData data = file.getNullableTeamData(team.get().getTeamId());

        file.withPlayerContext(player, () -> file.getSubmitTasks().stream() // WARN: NOT USE ASYNC! -> SERVER CRASH EVERY 5 MIN
                .filter(task -> task.id != sourceTask && data.canStartTasks(task.getQuest()))
                .forEach(task -> task.submitTask(data, player, craftedItem)));
    }


//    public void slotChanged(AbstractContainerMenu container, int index, ItemStack stack) {
//        if (!this.tryTick()) {
//            return;
//        }
//        if (!stack.isEmpty() && container.getSlot(index).container == player.getInventory()) {
//            int slotNum = container.getSlot(index).getContainerSlot();
//            if (slotNum >= 0 && slotNum < player.getInventory().items.size()) {
//                // Only checking for items in the main inventory & hotbar
//                // Armor slots can contain items with rapidly changing NBT (especially powered modded armor)
//                //  which can trigger a lot of unnecessary inventory scans
//                int delay = Mth.clamp(ServerQuestFile.INSTANCE.getDetectionDelay(), 0, 200);
//                if (delay == 0) {
//                    FTBQuestsInventoryListener.detect(player, ItemStack.EMPTY, 0);
//                } else {
//                    DeferredInventoryDetection.scheduleInventoryCheck(player, delay);
//                }
//            }
//        }
//    }

}
