package com.megalithx.taxpro.listeners;

import com.megalithx.taxpro.TaxPro;
import com.megalithx.taxpro.manager.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandPreprocessListener implements Listener {
    private final TaxPro plugin;

    public CommandPreprocessListener(TaxPro plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("Command-Fees.Enabled", true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("tax.exempt.commandfees")) return;

        String message = event.getMessage().substring(1);
        String[] args = message.split(" ");
        String label = args[0].toLowerCase();


        if (label.contains(":")) {
            String[] split = label.split(":");
            if (split.length > 1) {
                label = split[1];
            }
        }

        ConfigManager.CommandFee fee = null;
        if (args.length > 1) {
            fee = plugin.getConfigManager().getCommandFee(label + " %");
        }
        if (fee == null) {
            fee = plugin.getConfigManager().getCommandFee(label);
        }

        if (fee != null && fee.price > 0) {
            if (fee.group != null && player.hasPermission("tax.exempt.commandfees." + fee.group.toLowerCase())) return;

            double balance = plugin.getEconomy().getBalance(player);
            double exemptBelow = plugin.getConfig().getDouble("Command-Fees.Exempt-Below", 0.0);
            

            if (balance < exemptBelow) return;

            if (balance < fee.price) {
                event.setCancelled(true);
                String msg = plugin.getConfigManager().getMessage("Command-Fees.Insufficient-Funds")
                        .replace("%amount%", String.format("%.2f", fee.price))
                        .replace("%balance%", String.format("%.2f", balance));
                player.sendMessage(msg);
            } else {
                plugin.getEconomy().withdrawPlayer(player, fee.price);
                plugin.getLoggerSystem().logTransaction(player.getName(), "Command Fee (/" + label + ")", fee.price);

            }
        }
    }
}
