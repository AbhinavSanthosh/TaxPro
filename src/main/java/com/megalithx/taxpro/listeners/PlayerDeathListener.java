package com.megalithx.taxpro.listeners;

import com.megalithx.taxpro.TaxPro;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    private final TaxPro plugin;

    public PlayerDeathListener(TaxPro plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("Death-Penalty.Enabled", true)) return;

        Player player = event.getEntity();
        if (player.hasPermission("tax.exempt.deathpenalty")) return;

        // Check if the world is exempt
        java.util.List<String> exemptWorlds = plugin.getConfig().getStringList("Death-Penalty.Exempt-Worlds");
        if (exemptWorlds.contains(player.getWorld().getName())) return;

        double balance = plugin.getEconomy().getBalance(player);
        double exemptBelow = plugin.getConfig().getDouble("Death-Penalty.Exempt-Below", 1000.0);

        if (balance <= exemptBelow) return;

        double percentage = plugin.getConfig().getDouble("Death-Penalty.Wallet-Percentage", 0.05);
        double maxPenalty = plugin.getConfig().getDouble("Death-Penalty.Max-Penalty", 50000.0);

        double penalty = balance * percentage;
        if (maxPenalty != -1 && penalty > maxPenalty) {
            penalty = maxPenalty;
        }
        
        if (penalty > 0) {
            plugin.getEconomy().withdrawPlayer(player, penalty);
            plugin.getLoggerSystem().logTransaction(player.getName(), "Death Penalty", penalty);

            String msg = plugin.getConfigManager().getMessage("Death-Penalty.Death-Message")
                    .replace("%amount%", String.format("%.2f", penalty));
            player.sendMessage(msg);
        }
    }
}
