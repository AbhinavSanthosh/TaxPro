package com.megalithx.taxpro.tasks;

import com.megalithx.taxpro.TaxPro;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LandTaxTask {
    private final TaxPro plugin;

    public LandTaxTask(TaxPro plugin) {
        this.plugin = plugin;
    }

    public void startTaxing() {
        if (!plugin.getConfig().getBoolean("Integrations.GriefPrevention-Enabled", true)) return;
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") == null) return;

        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("Land-Tax.Broadcast-Start"));


        Map<UUID, Integer> playerBlocks = new HashMap<>();
        try {
            for (Claim claim : GriefPrevention.instance.dataStore.getClaims()) {
                if (claim.ownerID != null) {
                    playerBlocks.put(claim.ownerID, playerBlocks.getOrDefault(claim.ownerID, 0) + claim.getArea());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error accessing GriefPrevention claims: " + e.getMessage());
            return;
        }

        List<UUID> owners = new ArrayList<>(playerBlocks.keySet());

        if (owners.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aLand Tax collection complete! No claims found."));
            });
            return;
        }
        processBatch(owners, playerBlocks, 0, 50);
    }

    private void processBatch(List<UUID> owners, Map<UUID, Integer> playerBlocks, int index, int batchSize) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (owners.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String endMsg = plugin.getConfigManager().getMessage("Land-Tax.Broadcast-End");
                    if (endMsg != null && !endMsg.contains("Missing message")) {
                        Bukkit.broadcastMessage(endMsg);
                    }
                });
                return;
            }
            int max = Math.min(index + batchSize, owners.size());

            for (int i = index; i < max; i++) {
                try {
                    UUID uuid = owners.get(i);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    
                    if (player.getName() == null) continue;
                    if (player.isOnline() && player.getPlayer().hasPermission("tax.exempt.land")) continue;

                    int usedBlocks = playerBlocks.get(uuid);
                    if (usedBlocks == 0) continue;

                    int totalTaxableBlocks = usedBlocks;


                    String method = plugin.getConfig().getString("Land-Tax.Tax-Method", "USED").toUpperCase();
                    if (!method.equals("USED")) {
                        try {
                            me.ryanhamshire.GriefPrevention.PlayerData pd = GriefPrevention.instance.dataStore.getPlayerData(uuid);
                            if (pd != null) {
                                int total = pd.getAccruedClaimBlocks() + pd.getBonusClaimBlocks();
                                if (method.equals("TOTAL")) {
                                    totalTaxableBlocks = total;
                                } else if (method.equals("REMAINING")) {
                                    totalTaxableBlocks = Math.max(0, total - usedBlocks);
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    if (totalTaxableBlocks <= 0) continue;

                    double costPerBlock = plugin.getConfig().getDouble("Land-Tax.Cost-Per-Claim-Block", 0.01);
                    double totalCost = totalTaxableBlocks * costPerBlock;

                    double balance = plugin.getEconomy().getBalance(player);

                    if (balance >= totalCost) {
                        final int uBlocks = totalTaxableBlocks;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getEconomy().withdrawPlayer(player, totalCost);
                            plugin.getDataManager().resetStrikes(uuid);
                            plugin.getLoggerSystem().logTransaction(player.getName(), "Land Tax", totalCost);

                            if (player.isOnline()) {
                                double newBal = plugin.getEconomy().getBalance(player);
                                String rec = plugin.getConfigManager().getMessage("Land-Tax.Player-Receipt")
                                        .replace("%amount%", String.format("%.2f", totalCost))
                                        .replace("%blocks%", String.valueOf(uBlocks))
                                        .replace("%balance%", String.format("%.2f", newBal));
                                player.getPlayer().sendMessage(rec);
                            }
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> handleStrike(player));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not process Land Tax for index " + i + ": " + e.getMessage());
                }
            }

            if (max < owners.size()) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> processBatch(owners, playerBlocks, max, batchSize), 20L);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String endMsg = plugin.getConfigManager().getMessage("Land-Tax.Broadcast-End");
                    if (endMsg != null && !endMsg.contains("Missing message")) {
                        Bukkit.broadcastMessage(endMsg);
                    }
                });
            }
        });
    }

    private void handleStrike(OfflinePlayer player) {
        plugin.getDataManager().addStrike(player.getUniqueId());
        int strikes = plugin.getDataManager().getStrikes(player.getUniqueId());
        int maxStrikes = plugin.getConfig().getInt("Land-Tax.Penalty-System.Max-Strikes", 2);

        if (strikes >= maxStrikes) {
            String punishment = plugin.getConfig().getString("Land-Tax.Penalty-System.Punishment", "UNCLAIM_OLDEST");


            List<Claim> targetClaims = new ArrayList<>();
            for (Claim c : GriefPrevention.instance.dataStore.getClaims()) {
                if (player.getUniqueId().equals(c.ownerID)) {
                    targetClaims.add(c);
                }
            }

            if (targetClaims.isEmpty() && !punishment.equalsIgnoreCase("CUSTOM_COMMAND")) return;

            if (punishment.equalsIgnoreCase("UNCLAIM_ALL")) {
                for (Claim c : targetClaims) {
                    GriefPrevention.instance.dataStore.deleteClaim(c);
                }
            } else if (punishment.equalsIgnoreCase("CUSTOM_COMMAND")) {
                String cmd = plugin.getConfig().getString("Land-Tax.Penalty-System.Custom-Command", "cmi jail %player% 1h");
                cmd = cmd.replace("%player%", player.getName() != null ? player.getName() : player.getUniqueId().toString());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                if (!targetClaims.isEmpty()) {
                    Claim oldest = targetClaims.get(0);
                    GriefPrevention.instance.dataStore.deleteClaim(oldest);
                }
            }

            if (player.isOnline()) {
                player.getPlayer().sendMessage(plugin.getConfigManager().getMessage("Land-Tax.Strike-Action"));
            }
            plugin.getDataManager().resetStrikes(player.getUniqueId());
            plugin.getLoggerSystem().logTransaction(player.getName(), "Land Tax Seizure", 0.0);
        } else {
            if (player.isOnline()) {
                String warn = plugin.getConfigManager().getMessage("Land-Tax.Strike-Warning")
                        .replace("%strikes%", String.valueOf(strikes));
                player.getPlayer().sendMessage(warn);
            }
        }
    }
}
