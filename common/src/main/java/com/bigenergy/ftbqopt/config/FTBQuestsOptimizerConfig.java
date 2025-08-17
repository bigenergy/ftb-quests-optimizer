package com.bigenergy.ftbqopt.config;

import com.bigenergy.ftbqopt.FTBQuestsOptimizer;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;

public interface FTBQuestsOptimizerConfig {
    SNBTConfig CONFIG = SNBTConfig.create(FTBQuestsOptimizer.MODID)
            .comment("FTBQuestOptimizer config file", "If you're a modpack maker, edit defaultconfigs/ftbqoptimizer-server.snbt instead");

    BooleanValue DEBOUNCE = CONFIG.addBoolean("detect.debounce", true)
            .comment("Debounce detection so repeated slot changes schedule only one check.");

    IntValue DELAY_OVERRIDE = CONFIG.addInt("detect.delay_override", -1).range(-1, 200)
            .comment("Override FTBQ detection delay in ticks. -1 = use FTBQ's own value.");

    BooleanValue IGNORE_NBT_ONLY = CONFIG.addBoolean("detect.ignore_nbt_only_changes", true)
            .comment("Ignore slot changes where only NBT changed (same item & count).");

    BooleanValue AGGREGATE_CONSUME = CONFIG.addBoolean("consume.aggregate", true)
            .comment("Aggregate item removal and call addProgress() once.");

    IntValue MAX_SLOTS_PER_PASS = CONFIG.addInt("consume.max_slots_per_pass", 36).range(9, 200)
            .comment("Limit number of main+hotbar slots scanned per pass to avoid long stalls.");


}
