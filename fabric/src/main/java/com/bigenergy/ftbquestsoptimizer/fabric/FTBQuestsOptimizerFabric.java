package com.bigenergy.ftbquestsoptimizer.fabric;

import com.bigenergy.ftbquestsoptimizer.FTBQuestsOptimizer;
import net.fabricmc.api.ModInitializer;

public class FTBQuestsOptimizerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FTBQuestsOptimizer.init();
    }
}
