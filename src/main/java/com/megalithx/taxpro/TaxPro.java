package com.megalithx.taxpro;

import com.megalithx.taxpro.manager.ConfigManager;
import com.megalithx.taxpro.manager.DataManager;
import com.megalithx.taxpro.utils.LoggerSystem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class TaxPro extends JavaPlugin {
    private static TaxPro instance;
    private Economy econ = null;

    private ConfigManager configManager;
    private DataManager dataManager;
    private LoggerSystem loggerSystem;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.dataManager = new DataManager(this);
        this.loggerSystem = new LoggerSystem(this);

        if (configManager.getConfig().getBoolean("Integrations.Vault-Enabled", true)) {
            if (!setupEconomy() ) {
                getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency/Economy Provider found!", getDescription().getName()));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        if (configManager.getConfig().getBoolean("Integrations.GriefPrevention-Enabled", true)) {
            if (getServer().getPluginManager().getPlugin("GriefPrevention") == null) {
                getLogger().warning("GriefPrevention not found! Land tax will not work as expected.");
            }
        }


        getServer().getPluginManager().registerEvents(new com.megalithx.taxpro.listeners.PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new com.megalithx.taxpro.listeners.CommandPreprocessListener(this), this);


        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.megalithx.taxpro.hooks.TaxProExpansion(this).register();
            getLogger().info("PlaceholderAPI integration successfully enabled.");
        }


        new com.megalithx.taxpro.scheduler.TaxScheduler(this);


        if (getCommand("tax") != null) {
            com.megalithx.taxpro.commands.TaxCommand taxCommand = new com.megalithx.taxpro.commands.TaxCommand(this);
            getCommand("tax").setExecutor(taxCommand);
            getCommand("tax").setTabCompleter(taxCommand);
        }

        getLogger().info("TaxPro enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }
        getLogger().info("TaxPro disabled.");
    }

    public static TaxPro getInstance() {
        return instance;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("General.Debug-Mode", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public LoggerSystem getLoggerSystem() {
        return loggerSystem;
    }
}
