package com.megalithx.taxpro.hooks;

import com.megalithx.taxpro.TaxPro;
import com.megalithx.taxpro.tasks.WealthTaxTask;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TaxProExpansion extends PlaceholderExpansion {

    private final TaxPro plugin;

    public TaxProExpansion(TaxPro plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier() {
        return "taxpro";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equalsIgnoreCase("wealth_projected")) {
            double balance = plugin.getEconomy().getBalance(player);
            double wealthTax = new WealthTaxTask(plugin).calculateTax(balance);
            if (wealthTax <= 0) {
                return plugin.getConfigManager().getMessage("Stats.Tax-Exempt-Format");
            }
            return String.format("%.2f", wealthTax);
        }

        if (identifier.equalsIgnoreCase("land_projected")) {
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
            if (curLandTx <= 0) {
                return plugin.getConfigManager().getMessage("Stats.Tax-Exempt-Format");
            }
            return String.format("%.2f", curLandTx);
        }

        if (identifier.equalsIgnoreCase("strikes")) {
            return String.valueOf(plugin.getDataManager().getStrikes(player.getUniqueId()));
        }

        if (identifier.equalsIgnoreCase("strikes_max")) {
            return String.valueOf(plugin.getConfig().getInt("Land-Tax.Penalty-System.Max-Strikes", 2));
        }

        return null;
    }
}
