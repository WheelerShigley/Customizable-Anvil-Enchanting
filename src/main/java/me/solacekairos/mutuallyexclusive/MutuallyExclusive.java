package me.solacekairos.mutuallyexclusive;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public final class MutuallyExclusive extends JavaPlugin {
    Logger ME_logger;
    public String plugin_name = "MutuallyExclusive";

    @Override
    public void onEnable() {
        ME_logger = getLogger();
        ME_logger.info("Enchantment Recipes Reworked!");
        // Create config files
        getConfig().options().copyDefaults(); saveDefaultConfig();
        //Substitute custom recipes
        Bukkit.getPluginManager().registerEvents(new Addition(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        ME_logger.info("Enchantment Recipes Reverted!");
    }
}
