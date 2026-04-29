package com.megalithx.taxpro.manager;

import com.megalithx.taxpro.TaxPro;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {

    private final TaxPro plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(TaxPro plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
        }
    }
    
    public int getStrikes(UUID playerUUID) {
        return dataConfig.getInt("strikes." + playerUUID.toString(), 0);
    }

    public void addStrike(UUID playerUUID) {
        int strikes = getStrikes(playerUUID);
        dataConfig.set("strikes." + playerUUID.toString(), strikes + 1);
        saveData();
    }

    public void resetStrikes(UUID playerUUID) {
        dataConfig.set("strikes." + playerUUID.toString(), null);
        saveData();
    }

    public long getLastWealthTaxRun() {
        return dataConfig.getLong("schedule.wealth_tax_last_run", 0L);
    }

    public void setLastWealthTaxRun(long timestamp) {
        dataConfig.set("schedule.wealth_tax_last_run", timestamp);
        saveData();
    }

    public long getLastLandTaxRun() {
        return dataConfig.getLong("schedule.land_tax_last_run", 0L);
    }

    public void setLastLandTaxRun(long timestamp) {
        dataConfig.set("schedule.land_tax_last_run", timestamp);
        saveData();
    }
}
