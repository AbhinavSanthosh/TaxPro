package com.megalithx.taxpro.manager;

import com.megalithx.taxpro.TaxPro;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final TaxPro plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;


    public static class CommandFee {
        public double price;
        public String group;

        public CommandFee(double price, String group) {
            this.price = price;
            this.group = group;
        }
    }

    private final Map<String, CommandFee> commandPrices = new HashMap<>();

    public ConfigManager(TaxPro plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        

        try {
            java.io.InputStream in = plugin.getResource("config.yml");
            if (in != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, "UTF-8"));
                boolean configUpdated = false;
                for (String key : defConfig.getKeys(true)) {
                    if (!plugin.getConfig().contains(key)) {
                        plugin.getConfig().set(key, defConfig.get(key));
                        configUpdated = true;
                    }
                }
                if (configUpdated) plugin.saveConfig();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to auto-update config.yml");
        }

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        

        try {
            java.io.InputStream in = plugin.getResource("messages.yml");
            if (in != null) {
                YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, "UTF-8"));
                boolean messagesUpdated = false;
                for (String key : defMessages.getKeys(true)) {
                    if (!messagesConfig.contains(key)) {
                        messagesConfig.set(key, defMessages.get(key));
                        messagesUpdated = true;
                    }
                }
                if (messagesUpdated) messagesConfig.save(messagesFile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to auto-update messages.yml");
        }

        loadCommandPrices();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public String getMessage(String path) {
        String msg = messagesConfig.getString(path);
        if (msg == null) return "Missing message: " + path;
        String prefix = plugin.getConfig().getString("General.Prefix", "");
        msg = msg.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public List<String> getMessageList(String path) {
        List<String> list = messagesConfig.getStringList(path);
        List<String> coloredList = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            coloredList.add("Missing message list: " + path);
            return coloredList;
        }
        String prefix = plugin.getConfig().getString("General.Prefix", "");
        for (String msg : list) {
            msg = msg.replace("%prefix%", prefix);
            coloredList.add(ChatColor.translateAlternateColorCodes('&', msg));
        }
        return coloredList;
    }

    public CommandFee getCommandFee(String commandLabel) {
        return commandPrices.get(commandLabel.toLowerCase());
    }

    public void loadCommandPrices() {
        commandPrices.clear();
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("Command-Fees.Enabled", false)) return;

        ConfigurationSection groups = config.getConfigurationSection("Command-Fees.Groups");
        if (groups != null) {
            for (String groupName : groups.getKeys(false)) {
                double price = config.getDouble("Command-Fees.Groups." + groupName + ".Price", 0.0);
                List<String> commands = config.getStringList("Command-Fees.Groups." + groupName + ".Commands");
                for (String cmd : commands) {
                    commandPrices.put(cmd.toLowerCase(), new CommandFee(price, groupName));
                }
            }
        }

        ConfigurationSection individuals = config.getConfigurationSection("Command-Fees.Individual");
        if (individuals != null) {
            for (String cmd : individuals.getKeys(false)) {
                double price = individuals.getDouble("Command-Fees.Individual." + cmd, 0.0);
                commandPrices.put(cmd.toLowerCase(), new CommandFee(price, null));
            }
        }
    }
}
