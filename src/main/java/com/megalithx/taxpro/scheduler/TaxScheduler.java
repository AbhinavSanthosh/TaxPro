package com.megalithx.taxpro.scheduler;

import com.megalithx.taxpro.TaxPro;
import com.megalithx.taxpro.tasks.LandTaxTask;
import com.megalithx.taxpro.tasks.WealthTaxTask;
import org.bukkit.Bukkit;

public class TaxScheduler {
    private final TaxPro plugin;

    public TaxScheduler(TaxPro plugin) {
        this.plugin = plugin;
        

        long now = System.currentTimeMillis();
        if (plugin.getDataManager().getLastWealthTaxRun() == 0) {
            plugin.getDataManager().setLastWealthTaxRun(now);
        }
        if (plugin.getDataManager().getLastLandTaxRun() == 0) {
            plugin.getDataManager().setLastLandTaxRun(now);
        }
        
        startScheduler();
    }

    private void startScheduler() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();


            if (plugin.getConfig().getBoolean("Wealth-Tax.Enabled", true) && plugin.getConfig().getBoolean("Wealth-Tax.Auto-Run", true)) {
                int freqHours = plugin.getConfig().getInt("Wealth-Tax.Frequency-Hours", 168);
                long freqMs = freqHours * 60L * 60L * 1000L;
                long lastRun = plugin.getDataManager().getLastWealthTaxRun();

                if (now - lastRun >= freqMs) {
                    plugin.getDataManager().setLastWealthTaxRun(now);
                    new WealthTaxTask(plugin).startTaxing();
                }
            }


            if (plugin.getConfig().getBoolean("Land-Tax.Enabled", true) && plugin.getConfig().getBoolean("Land-Tax.Auto-Run", true)) {
                int freqHours = plugin.getConfig().getInt("Land-Tax.Frequency-Hours", 24);
                long freqMs = freqHours * 60L * 60L * 1000L;
                long lastRun = plugin.getDataManager().getLastLandTaxRun();

                if (now - lastRun >= freqMs) {
                    plugin.getDataManager().setLastLandTaxRun(now);
                    new LandTaxTask(plugin).startTaxing();
                }
            }

        }, 20L, 1200L);
    }
}
