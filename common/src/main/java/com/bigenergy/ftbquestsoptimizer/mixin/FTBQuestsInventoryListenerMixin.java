package com.bigenergy.ftbquestsoptimizer.mixin;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = FTBQuestsInventoryListener.class, remap = false)
public class FTBQuestsInventoryListenerMixin {

    @Shadow
    public final ServerPlayer player;


    public FTBQuestsInventoryListenerMixin(ServerPlayer player) {
        this.player = player;
    }

    private static int ticksSkipped = 0;
    private static final int skipTicksAmount = 5;

    private static boolean tryTick() {
        ticksSkipped++;
        if (ticksSkipped > skipTicksAmount) {
            ticksSkipped = 0;
            return true;
        }
        return false;
    }


    /**
     * @author Big_Energy
     * @reason performance fix
     */
    @Overwrite
    public static void detect(ServerPlayer player, ItemStack craftedItem, long sourceTask) {
        if (!tryTick()) {
            return;
        }

        ServerQuestFile file = ServerQuestFile.INSTANCE;

        if (file == null || PlayerHooks.isFake(player)) {
            return;
        }

        List<Task> submitTasks = file.getSubmitTasks();
        List<Task> craftingTasks = file.getCraftingTasks();

        if (!submitTasks.isEmpty() || !craftingTasks.isEmpty()) {
            FTBTeamsAPI.API api = FTBTeamsAPI.api();
            Team team = api.getManager().getTeamForPlayer(player).orElse(null);

            if (team != null) {
                TeamData data = file.getNullableTeamData(team.getId());

                if (data != null && !data.isLocked()) {
                    file.withPlayerContext(player, () -> {
                        List<Task> tasksToCheck = craftedItem.isEmpty() ? submitTasks : craftingTasks;

                        for (Task task : tasksToCheck) {
                            if (task.id != sourceTask && data.canStartTasks(task.getQuest())) {
                                task.submitTask(data, player, craftedItem);
                            }
                        }
                    });
                }
            }
        }
    }

}
