package com.megalithx.taxpro.commands;

import com.megalithx.taxpro.TaxPro;
import com.megalithx.taxpro.tasks.LandTaxTask;
import com.megalithx.taxpro.tasks.WealthTaxTask;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaxCommand implements CommandExecutor, TabCompleter {
    private final TaxPro plugin;

    public TaxCommand(TaxPro plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("tax.admin")) {
                sendHelp(sender);
            } else if (sender.hasPermission("tax.command.stats")) {
                sendStats(sender);
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessage("Errors.No-Permission"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            if (!sender.hasPermission("tax.command.stats") && !sender.hasPermission("tax.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("Errors.No-Permission"));
                return true;
            }
            sendStats(sender);
            return true;
        }

        if (!sender.hasPermission("tax.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("Errors.No-Permission"));
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("forcewealth")) {
            if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
                sender.sendMessage(ChatColor.GREEN + "Forcing Wealth Tax cycle...");
                plugin.getDataManager().setLastWealthTaxRun(System.currentTimeMillis());
                new WealthTaxTask(plugin).startTaxing();
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Are you sure? Run " + ChatColor.YELLOW + "/tax forcewealth confirm");
            return true;
        }

        if (args[0].equalsIgnoreCase("forceland")) {
            sender.sendMessage(ChatColor.GREEN + "Forcing Land Tax cycle...");
            plugin.getDataManager().setLastLandTaxRun(System.currentTimeMillis());
            new LandTaxTask(plugin).startTaxing();
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().loadAll();
            sender.sendMessage(ChatColor.GREEN + "TaxPro configuration and messages reloaded!");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("tax.command.stats")) {
                if ("stats".startsWith(args[0].toLowerCase())) completions.add("stats");
            }
            if (sender.hasPermission("tax.admin")) {
                List<String> subCommands = Arrays.asList("help", "forcewealth", "forceland", "reload");
                for (String sub : subCommands) {
                    if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("forcewealth") && sender.hasPermission("tax.admin")) {
            if ("confirm".startsWith(args[1].toLowerCase())) completions.add("confirm");
        }
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------&r &eTaxPro Help &8&m--------"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/tax help &8- &7Shows this menu."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/tax forcewealth &8- &7Force weekly wealth tax."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/tax forceland &8- &7Force daily land tax."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/tax stats &8- &7Show plugin statistics."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/tax reload &8- &7Reload config and messages."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
    }

    private String formatTimeRemaining(long msRemaining) {
        if (msRemaining <= 0) return "Moments";
        long seconds = msRemaining / 1000;
        long days = seconds / (24 * 3600);
        seconds = seconds % (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");
        
        String res = sb.toString().trim();
        return res.isEmpty() ? "Moments" : res;
    }

    private void sendStats(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can view personal tax stats.");
            return;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Wealth Tax
            double balance = plugin.getEconomy().getBalance(player);
            double wealthTax = new WealthTaxTask(plugin).calculateTax(balance);
            long now = System.currentTimeMillis();
            long wFreqMs = plugin.getConfig().getInt("Wealth-Tax.Frequency-Hours", 168) * 3600000L;
            long wLast = plugin.getDataManager().getLastWealthTaxRun();
            long wRemainingMs = wFreqMs - (now - wLast);
            String wealthTimeRem = formatTimeRemaining(wRemainingMs);

            // Land Tax
            int totalBlocksForTax = 0;
            if (plugin.getConfig().getBoolean("Integrations.GriefPrevention-Enabled", true) && Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
                String method = plugin.getConfig().getString("Land-Tax.Tax-Method", "USED").toUpperCase();
                try {
                    me.ryanhamshire.GriefPrevention.PlayerData pd = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
                    if (pd != null) {
                        int accrued = pd.getAccruedClaimBlocks() + pd.getBonusClaimBlocks();
                        int used = 0;
                        for (Claim c : pd.getClaims()) used += c.getArea();

                        if (method.equals("TOTAL")) {
                            totalBlocksForTax = accrued;
                        } else if (method.equals("REMAINING")) {
                            totalBlocksForTax = Math.max(0, accrued - used);
                        } else {
                            totalBlocksForTax = used;
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            double costPerBlock = plugin.getConfig().getDouble("Land-Tax.Cost-Per-Claim-Block", 0.01);
            double curLandTx = totalBlocksForTax * costPerBlock;
            long lFreqMs = plugin.getConfig().getInt("Land-Tax.Frequency-Hours", 24) * 3600000L;
            long lLast = plugin.getDataManager().getLastLandTaxRun();
            long lRemainingMs = lFreqMs - (now - lLast);
            String landTimeRem = formatTimeRemaining(lRemainingMs);

            // Strikes
            int strikes = plugin.getDataManager().getStrikes(player.getUniqueId());

            String exemptStr = plugin.getConfigManager().getMessage("Stats.Tax-Exempt-Format");
            
            for (String line : plugin.getConfigManager().getMessageList("Stats.Player-Stats")) {
                String wTaxStr = wealthTax > 0 ? String.format("%.2f", wealthTax) : exemptStr;
                String lTaxStr = curLandTx > 0 ? String.format("%.2f", curLandTx) : exemptStr;
                
                // Remove the extra characters if exempt is used to make it look clean
                if (wealthTax <= 0) line = line.replace("-$%wealth_tax_amount%", "%wealth_tax_amount%");
                if (curLandTx <= 0) line = line.replace("-$%land_tax_amount%", "%land_tax_amount%");

                sender.sendMessage(line
                        .replace("%wealth_tax_amount%", wTaxStr)
                        .replace("%wealth_time_remaining%", wealthTimeRem)
                        .replace("%land_tax_amount%", lTaxStr)
                        .replace("%land_time_remaining%", landTimeRem)
                        .replace("%strikes%", String.valueOf(strikes)));
            }
        });
    }
}
