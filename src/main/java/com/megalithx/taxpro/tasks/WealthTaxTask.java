package com.megalithx.taxpro.tasks;

import com.megalithx.taxpro.TaxPro;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class WealthTaxTask {
    private final TaxPro plugin;

    public WealthTaxTask(TaxPro plugin) {
        this.plugin = plugin;
    }

    public void startTaxing() {
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("Wealth-Tax.Broadcast-Start"));

        List<OfflinePlayer> players = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            players.add(p);
        }

        processBatch(players, 0, 50); // Process 50 players max at a time asynchronously
    }

    private void processBatch(List<OfflinePlayer> players, int index, int batchSize) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int max = Math.min(index + batchSize, players.size());

            for (int i = index; i < max; i++) {
                try {
                    OfflinePlayer player = players.get(i);

                    if (player.getName() == null) continue;
                    if (player.isOnline() && player.getPlayer().hasPermission("tax.exempt.wealth")) continue;

                    double balance = plugin.getEconomy().getBalance(player);
                    if (balance <= 0) continue;

                    double taxToPay = calculateTax(balance);

                    if (taxToPay > 0) {
                        // Process vault deductions on main thread
                        final double deduction = taxToPay;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getEconomy().withdrawPlayer(player, deduction);
                            plugin.getLoggerSystem().logTransaction(player.getName(), "Wealth Tax", deduction);

                            if (player.isOnline()) {
                                double newBal = plugin.getEconomy().getBalance(player);
                                String rec = plugin.getConfigManager().getMessage("Wealth-Tax.Player-Receipt")
                                        .replace("%amount%", String.format("%.2f", deduction))
                                        .replace("%balance%", String.format("%.2f", newBal));
                                player.getPlayer().sendMessage(rec);
                            }
                        });
                    } else {
                        if (player.isOnline()) {
                            player.getPlayer().sendMessage(plugin.getConfigManager().getMessage("Wealth-Tax.Exempt"));
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not process Wealth Tax for index " + i + ": " + e.getMessage());
                }
            }

            if (max < players.size()) {
                // Delay 1 second before doing the next batch
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> processBatch(players, max, batchSize), 20L);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("Wealth-Tax.Broadcast-End"));
                });
            }
        });
    }

    public double calculateTax(double balance) {
        ConfigurationSection brackets = plugin.getConfig().getConfigurationSection("Wealth-Tax.Brackets");
        if (brackets == null) return 0.0;

        for (String key : brackets.getKeys(false)) {
            double min = brackets.getDouble(key + ".Min");
            double max = brackets.getDouble(key + ".Max");

            if (balance >= min && (balance <= max || max == -1)) {
                double rate = brackets.getDouble(key + ".Rate");
                double baseFee = brackets.getDouble(key + ".Base-Fee");
                
                // Progressive bracket calculation
                double threshold = Math.max(0, min - 1);

                return baseFee + ((balance - threshold) * rate);
            }
        }
        return 0.0;
    }
}
