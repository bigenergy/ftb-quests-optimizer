package com.bigenergy.ftbquestsoptimizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FTBQuestsOptimizer {
    public static final String MODID = "ftbqoptimizer";
    public static final Logger LOGGER = LogManager.getLogger("FTB Quests Optimizer");

    public static void init() {
        LOGGER.info("Enabling FTB Quests Optimizer");
    }
}
