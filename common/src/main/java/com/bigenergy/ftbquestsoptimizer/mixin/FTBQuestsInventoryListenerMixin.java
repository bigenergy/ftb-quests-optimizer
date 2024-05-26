package com.bigenergy.ftbquestsoptimizer.mixin;

import com.bigenergy.ftbquestsoptimizer.config.FTBQuestsOptimizerConfig;
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
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = FTBQuestsInventoryListener.class, remap = false)
public class FTBQuestsInventoryListenerMixin {

    @Shadow
    public final ServerPlayer player;


    public FTBQuestsInventoryListenerMixin(ServerPlayer player) {
        this.player = player;
    }

    @Unique
    private static int fTBQuestsOptimizer$ticksSkipped = 0;
    @Unique
    private static final int fTBQuestsOptimizer$skipTicksAmount = FTBQuestsOptimizerConfig.SKIP_TICKS_AMOUNT.get();

    @Unique
    private static boolean fTBQuestsOptimizer$tryTick() {
        fTBQuestsOptimizer$ticksSkipped++;
        if (fTBQuestsOptimizer$ticksSkipped > fTBQuestsOptimizer$skipTicksAmount) {
            fTBQuestsOptimizer$ticksSkipped = 0;
            return true;
        }
        return false;
    }


    /**
     * @author Big_Energy
     * @reason performance fix by FTBQuestsOptimizer
     */
    @Overwrite
    public static void detect(ServerPlayer player, ItemStack craftedItem, long sourceTask) {
        if (!fTBQuestsOptimizer$tryTick()) {
            return;
        }

        ServerQuestFile file = ServerQuestFile.INSTANCE;

        if (file == null || PlayerHooks.isFake(player)) {
            return;
        }

        if (FTBQuestsOptimizerConfig.DETECT_OPTIMIZATION.get()) {
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
        } else {
            // default ftbq code
            List<Task> tasksToCheck = craftedItem.isEmpty() ? file.getSubmitTasks() : file.getCraftingTasks();

            if (!tasksToCheck.isEmpty()) {
                FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
                    TeamData data = file.getNullableTeamData(team.getId());
                    if (data != null && !data.isLocked()) {
                        file.withPlayerContext(player, () -> {
                            for (Task task : tasksToCheck) {
                                if (task.id != sourceTask && data.canStartTasks(task.getQuest())) {
                                    task.submitTask(data, player, craftedItem);
                                }
                            }
                        });
                    }
                });
            }
        }

    }

}
