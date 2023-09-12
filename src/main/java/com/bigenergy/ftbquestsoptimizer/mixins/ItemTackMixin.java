package com.bigenergy.ftbquestsoptimizer.mixins;

import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftbquests.item.MissingItem;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ItemTask.class)
public class ItemTackMixin extends Task implements Predicate<ItemStack> {
    public ItemTackMixin(Quest q) {
        super(q);
    }

    @Shadow
    public TaskType getType() {
        return null;
    }

    @Shadow
    public boolean taskScreenOnly;
    @Shadow
    public Tristate onlyFromCrafting;
    @Shadow
    public long count;
    @Shadow
    public ItemStack item;

    @Shadow
    public ItemStack insert(TeamData teamData, ItemStack stack, boolean simulate) {
        if (!teamData.isCompleted(this) && consumesResources() && test(stack)) {
            long add = Math.min(stack.getCount(), count - teamData.getProgress(this));

            if (add > 0L) {
                if (!simulate && teamData.file.isServerSide()) {
                    teamData.addProgress(this, add);
                }

                ItemStack copy = stack.copy();
                copy.setCount((int) (stack.getCount() - add));
                return copy;
            }
        }

        return stack;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (taskScreenOnly || teamData.isCompleted(this) || item.getItem() instanceof MissingItem || craftedItem.getItem() instanceof MissingItem) {
            return;
        }

        if (!consumesResources()) {
            if (onlyFromCrafting.get(false)) {
                if (!craftedItem.isEmpty() && test(craftedItem)) {
                    teamData.addProgress(this, craftedItem.getCount());
                }
            } else {
                //long c = Math.min(count, player.getInventory().items.stream().filter(this).mapToLong(ItemStack::getCount).sum());

                long c = Math.min(count, 0L);
                for (ItemStack stack : player.getInventory().items) {
                    if (this.test(stack)) {
                        c += stack.getCount();
                    }
                }

                long progress = teamData.getProgress(this);
                if (c > progress) {
                    teamData.setProgress(this, c);
                }
            }
        } else if (craftedItem.isEmpty()) {
            boolean changed = false;
            List<ItemStack> inventory = player.getInventory().items;
            int inventorySize = inventory.size();
            for (int i = 0; i < inventorySize; i++) {
                ItemStack stack = inventory.get(i);
                ItemStack stack1 = insert(teamData, stack, false);

                if (stack != stack1) {
                    changed = true;
                    inventory.set(i, stack1.isEmpty() ? ItemStack.EMPTY : stack1);
                }
            }

            if (changed) {
                player.getInventory().setChanged();
                if (player.containerMenu != null) {
                    player.containerMenu.broadcastChanges();
                }
            }
        }
    }

    @Shadow
    public boolean test(ItemStack itemStack) {
        return false;
    }
}
