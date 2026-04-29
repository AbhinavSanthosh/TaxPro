package com.megalithx.taxpro.utils;

import com.megalithx.taxpro.TaxPro;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerSystem {

    private final TaxPro plugin;
    private File logFile;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LoggerSystem(TaxPro plugin) {
        this.plugin = plugin;
        setupLogger();
    }

    private void setupLogger() {
        logFile = new File(plugin.getDataFolder(), "taxes.log");
        if (!logFile.exists()) {
            logFile.getParentFile().mkdirs();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create taxes.log!");
            }
        }
    }


    public void logTransaction(String playerName, String taxType, double amount) {
        if (!plugin.getConfig().getBoolean("General.Log-Transactions", true)) return;
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileWriter fw = new FileWriter(logFile, true); PrintWriter out = new PrintWriter(fw)) {
                String timestamp = LocalDateTime.now().format(dtFormatter);
                out.println(String.format("[%s] %s | Player: %s | Amount: $%.2f", timestamp, taxType, playerName, amount));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write to taxes.log for player " + playerName);
            }
        });
    }
}
