package com.bigenergy.ftbquestsoptimizer;

import com.bigenergy.ftbquestsoptimizer.config.FTBQuestsOptimizerConfig;
import dev.architectury.platform.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class FTBQuestsOptimizer {

    public static final String MODID = "ftbqoptimizer";
    public static final Logger LOGGER = LogManager.getLogger("FTB Quests Optimizer");

    public static final String CONFIG_FILE = MODID + ".snbt";

    private static final String[] DEFAULT_CONFIG = {
            "Default config file that will be copied to instance's config/ftbqoptimizer.snbt location",
            "Copy values you wish to override in here",
            "Example:",
            "",
            "{",
            "	misc: {",
            "		enderchest: {",
            "			enabled: false",
            "		}",
            "	}",
            "}",
    };

    public static void init() {
        LOGGER.info("Enabling FTB Quests Optimizer");

        Path configFilePath = Platform.getConfigFolder().resolve(CONFIG_FILE);
        Path defaultConfigFilePath = Platform.getConfigFolder().resolve("../defaultconfigs/ftbessentials-server.snbt");

        FTBQuestsOptimizerConfig.CONFIG.load(configFilePath, defaultConfigFilePath, () -> DEFAULT_CONFIG);
    }

}
