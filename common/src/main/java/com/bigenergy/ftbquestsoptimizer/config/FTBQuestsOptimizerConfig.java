package com.bigenergy.ftbquestsoptimizer.config;

import com.bigenergy.ftbquestsoptimizer.FTBQuestsOptimizer;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;

public interface FTBQuestsOptimizerConfig {
    SNBTConfig CONFIG = SNBTConfig.create(FTBQuestsOptimizer.MODID)
            .comment("FTBQuestOptimizer config file", "If you're a modpack maker, edit defaultconfigs/ftbqoptimizer-server.snbt instead");

    BooleanValue DETECT_OPTIMIZATION = CONFIG.addBoolean("detect_optimization", true)
            .comment("Enable optimization of quest completion detection");

    IntValue SKIP_TICKS_AMOUNT = CONFIG.addInt("skip_ticks_amount", 5).range(0, 100)
            .comment("Number of tick skips for checking player inventories, 0 to disable tick skipping. Experiment and find the best value for you!");



}
