package com.bigenergy.ftbquestsoptimizer.mixin;

import dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(
        method = "slotChanged",
        at={@At(value="HEAD")}
    )
    public void slotChanged(AbstractContainerMenu menu, int index, ItemStack stack, CallbackInfo ci) {
        if (!this.tryTick()) {
            return;
        }
    }

}
