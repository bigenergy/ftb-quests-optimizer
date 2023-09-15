package com.bigenergy.ftbquestsoptimizer.forge;

import com.bigenergy.ftbquestsoptimizer.FTBQuestsOptimizer;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FTBQuestsOptimizer.MODID)
public class FTBQuestsOptimizerForge {
    public FTBQuestsOptimizerForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(FTBQuestsOptimizer.MODID, FMLJavaModLoadingContext.get().getModEventBus());
        IEventBus modEventBus = EventBuses.getModEventBus(FTBQuestsOptimizer.MODID).orElseThrow();

        modEventBus.addListener(this::onInitialize);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onInitialize(final FMLCommonSetupEvent event) {
        FTBQuestsOptimizer.init();
    }
}
