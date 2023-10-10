package com.bigenergy.ftbquestsoptimizer.mixin;

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

}
