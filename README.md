# 🏦 TaxPro


**Supported Java Version:** Java 17+  
**Supported Minecraft Versions:** 1.13 - 1.20+  

A high-performance, completely lag-free economy and taxation plugin for Spigot/Paper servers. Built to handle massive player bases with seamless GriefPrevention and Vault integration.

---

## 📑 Table of Contents
1. [✨ Features](#-features)
2. [💻 Commands](#-commands)
3. [🔐 Permissions](#-permissions)
4. [🏷️ Placeholders](#️-placeholders)
5. [⚙️ Default Configuration (`config.yml`)](#️-default-configuration-configyml)
6. [💬 Default Messages (`messages.yml`)](#-default-messages-messagesyml)
7. [📥 Installation](#-installation)
8. [🛠️ Building](#️-building)

---

## ✨ Features
- **📈 Progressive Wealth Tax**: Automatically runs margin-based wealth tax deductions based on configured tiered brackets.
- **🏡 Land Tax**: Taxes players for their active or unused GriefPrevention claim blocks. Includes a sophisticated strike-system that automatically unclaims abandoned properties.
- **💀 Death Penalties**: Highly configurable wallet percentage deductions with world exemptions (e.g., bypass in PvP Arenas).
- **💸 Command Fees**: Charge players in-game money for running commands. Supports custom grouped fees and individual granular overrides with dynamic bypass permissions.
- **⏱️ Dynamic Data Persistence**: All timers use persistent Unix timestamps. Even if your server crashes or goes offline for a week, it perfectly tracks exactly when the next tax is due!
- **⚡ Asynchronous Optimization**: Implements background batch processing. TaxPro safely executes heavy calculations on separate threads, guaranteeing it will never cause a server TPS drop.

---

## 💻 Commands

| Command | Description | Permission Required |
|---------|-------------|---------------------|
| `/tax help` | Shows the plugin help menu | `tax.admin` |
| `/tax stats` | View your personal projected taxes, due dates, and strikes | `tax.command.stats` |
| `/tax forcewealth` | Force the wealth tax cycle and reset the timer | `tax.admin` |
| `/tax forceland` | Force the daily land tax cycle and reset the timer | `tax.admin` |
| `/tax reload` | Reloads all configurations and messages | `tax.admin` |

---

## 🔐 Permissions

| Permission Node | Description | Default |
|-----------------|-------------|---------|
| `tax.admin` | Access to administrative commands | OP |
| `tax.command.stats` | Access to view personal stats | True |
| `tax.exempt.wealth` | Exempts the user from the wealth tax | False |
| `tax.exempt.land` | Exempts the user from the land tax | False |
| `tax.exempt.deathpenalty` | Exempts the user from dropping money on death | False |
| `tax.exempt.commandfees` | Master bypass for all command fees | False |
| `tax.exempt.commandfees.<group>`| Granular bypass for a specific command fee group | False |

---

## 🏷️ Placeholders
*(Requires PlaceholderAPI)*

| Placeholder | Returns |
|-------------|---------|
| `%taxpro_wealth_projected%` | Projected wealth tax cost (e.g., `$150.00` or `Exempt ($0.00)`) |
| `%taxpro_land_projected%` | Projected land tax cost based on current claims |
| `%taxpro_strikes%` | Current land tax penalty strikes |
| `%taxpro_strikes_max%` | The server's configured maximum strikes |

---

## ⚙️ Default Configuration (`config.yml`)

```yaml
# ==========================================
#        TaxPro - Main Config
# ==========================================

General:
  Prefix: "&b&lTaxPro &7» &f"
  Debug-Mode: false
  # Enable logging tax deductions and payments to taxes.log
  Log-Transactions: true

Integrations:
  # Requires Vault and a valid Economy provider (e.g., EssentialsX, CMI)
  Vault-Enabled: true
  # Requires GriefPrevention
  GriefPrevention-Enabled: true

Wealth-Tax:
  Enabled: true
  # Should the tax run automatically based on its frequency? Set to false to only allow manual /tax forcewealth
  Auto-Run: true
  # Frequency in hours (168 = 1 week)
  Frequency-Hours: 168
  Brackets:
    # Marginal tax system: Only taxes the amount within the bracket
    Tier1:
      Min: 0
      Max: 25000
      Rate: 0.0
      Base-Fee: 0.0
    Tier2:
      Min: 25001
      Max: 100000
      Rate: 0.02
      Base-Fee: 500.0
    Tier3:
      Min: 100001
      Max: 500000
      Rate: 0.05
      Base-Fee: 5000.0
    Tier4:
      Min: 500001
      Max: 2000000
      Rate: 0.08
      Base-Fee: 40000.0
    Tier5:
      Min: 2000001
      Max: -1 # -1 represents infinity
      Rate: 0.10
      Base-Fee: 200000.0

Land-Tax:
  Enabled: true
  # Should the tax run automatically based on its frequency? Set to false to only allow manual /tax forceland
  Auto-Run: true
  # Frequency in hours (24 = 1 day)
  Frequency-Hours: 24
  cost-Per-Claim-Block: 0.01
  # Tax-Method can be:
  # 'USED' (only blocks currently physically placed/claimed)
  # 'REMAINING' (blocks sitting in their pool that are NOT physically claimed yet)
  # 'TOTAL' (their entire block pool unconditionally: Used + Remaining combined)
  Tax-Method: "USED"
  Penalty-System:
    # Max strikes before action is taken
    Max-Strikes: 2
    # Action on max strikes: 'UNCLAIM_OLDEST', 'UNCLAIM_ALL', or 'CUSTOM_COMMAND'
    Punishment: "CUSTOM_COMMAND"
    # The command to dispatch from console if Punishment is set to CUSTOM_COMMAND
    Custom-Command: "cmi jail %player% 1h not paying taxes"

Death-Penalty:
  Enabled: true
  # Percentage of current wallet balance lost on death
  Wallet-Percentage: 0.01
  # Minimum balance required for the penalty to apply (protects new players)
  Exempt-Below: 1000.0
  # Maximum amount that can be lost in a single death (set to -1 for no cap)
  Max-Penalty: -1
  # List of worlds where the death penalty does NOT apply
  Exempt-Worlds:
    - "spawn"

Command-Fees:
  Enabled: true
  # Players only receive a message if they have insufficient funds.
  Exempt-Below: 500.0
  
  # Grouped prices: Apply the same fee to a list of commands
  Groups:
    Teleportation:
      Price: 25.0
      Commands:
        - "tpa %" # % represents wildcard
        - "tpaccept"
        - "home"
        
  # Individual prices: Overrides group prices if a command appears in both
  Individual:
    "ec": 50.0
    "feed": 15.0
    "repair": 500.0

```

---

## 💬 Default Messages (`messages.yml`)

```yaml
Wealth-Tax:
  Broadcast-Start: "%prefix%&eThe weekly Wealth Tax is now being processed..."
  Broadcast-End: "%prefix%&aWealth Tax collection complete. Thank you for supporting the economy!"
  Player-Receipt: "%prefix%&7The Taxman has collected &c$%amount% &7based on your wealth bracket. New Balance: &a$%balance%. &7Run [/tax stats] for more information."
  Exempt: "%prefix%&a[Tax] &7Your wealth falls under the taxation threshold. You are exempt from the Wealth Tax this week."

Land-Tax:
  Broadcast-Start: "%prefix%&bThe daily Land Tax is now being processed..."
  Broadcast-End: "%prefix%&aLand Tax collection complete. All properties evaluated! test"
  Player-Receipt: "%prefix%&7Paid &c$%amount% &7upkeep to taxman for &b%blocks% &7claim blocks. New Balance: &a$%balance%. &7Run [/tax stats] for more information."
  Strike-Warning: "%prefix%&e[Warning] &cYou had insufficient funds to pay your Land Tax! &7Strike: &c%strikes%/2&7. Fund your account to avoid claim deletion!"
  Strike-Action: "%prefix%&4[ALERT] &cYou failed to pay your Land Tax for multiple days."

Death-Penalty:
  Death-Message: "%prefix%&8[&4Death&8] &7The Styx toll has been collected. You lost &c$%amount% &7from your wallet."

Command-Fees:
  Insufficient-Funds: "&cYou need $%amount% to use this command. Your balance: $%balance%"

Errors:
  No-Permission: "&cYou do not have permission to do this."
  Vault-Error: "&4Critical Error: Vault integration failed. Please contact an administrator."

Stats:
  Player-Stats:
    - "&8&m--------&r &eYour Tax Stats &8&m--------"
    - "&eProjected Wealth Tax: &c-$%wealth_tax_amount% &7(Due in: %wealth_time_remaining%)"
    - "&eProjected Land Tax: &c-$%land_tax_amount% &7(Due in: %land_time_remaining%)"
    - "&cActive Penalties: %strikes% Strike(s)"
    - "&8&m-----------------------------"

  Tax-Exempt-Format: "&aExempt ($0.00)"
```

---

## 📥 Installation
1. Drop the compiled `TaxPro-1.0-SNAPSHOT.jar` into your server's `/plugins` folder.
2. Ensure you have [Vault](https://www.spigotmc.org/resources/vault.34315/) installed along with an economy provider (like EssentialsX or CMI).
3. Optionally, install [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) to utilize the Land Tax feature.
4. Restart your server.

---

## 🛠️ Building
Compile the plugin using Maven:
```bash
mvn clean package
```
