# SimpleEconomy

A simple server-side economy and player shop system for Minecraft NeoForge 1.21.1.
No client mod required - works with vanilla clients!

## Features

### Economy System
- **Player Balances**: Each player has a coin balance that persists across sessions
- **Starting Balance**: New players receive a configurable starting amount
- **Transaction Tax**: Configurable tax on shop sales (default 5%)
- **Leaderboard**: See the richest players on the server

**Commands:**
| Command | Description |
|---------|-------------|
| `/bal` or `/balance` | Check your balance |
| `/bal <player>` | Check another player's balance |
| `/pay <player> <amount>` | Send coins to another player |
| `/baltop` or `/leaderboard` | View richest players |
| `/transactions` or `/history` | View your transaction history |

### Shop System
- **Player Shops**: Players can create and manage their own shops
- **Shop Browser GUI**: Browse all shops with a chest-based interface
- **Bulk Buying**: Adjust quantity before purchasing
- **Featured Shops**: Admins can feature shops to highlight them
- **Offline Sales**: Sellers earn money even when offline, with a summary on login
- **Sale Notifications**: Real-time notifications when someone buys from your shop

**Commands:**
| Command | Description |
|---------|-------------|
| `/shops` or `/marketplace` | Open the shop browser GUI |
| `/shop create <name>` | Create a new shop |
| `/shop list` | List your shops |
| `/shop add <quantity> <price>` | Add held item to your shop |
| `/shop manage` | Open shop management GUI |
| `/shop delete` | Delete your shop (items returned) |

### Daily Rewards
- **Daily Claim**: Earn coins every day with `/daily`
- **Streak System**: Consecutive daily claims increase your reward
- **Max Streak**: Configurable max streak with bonus scaling

**Commands:**
| Command | Description |
|---------|-------------|
| `/daily` or `/claim` | Claim your daily reward |
| `/streak` | View your current streak |

### Coinflip
- **PvP Gambling**: Challenge another player to a 50/50 coinflip
- **Clickable Buttons**: Accept or deny challenges with clickable chat buttons
- **Auto-timeout**: Challenges expire after 60 seconds

**Commands:**
| Command | Description |
|---------|-------------|
| `/coinflip <player> <amount>` | Challenge a player |
| `/coinflip accept` | Accept a challenge |
| `/coinflip deny` | Deny a challenge |
| `/coinflip cancel` | Cancel your outgoing challenge |

### Mob Drops
- Mobs drop coins on death (configurable per mob type)
- Bosses have higher drop rates
- PvP kill bounty system (configurable percentage)

### Weekly Interest
- Players earn interest on their balance weekly
- Configurable rate and maximum cap

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/eco add <player> <amount>` | Add coins to a player | OP (level 2) |
| `/eco remove <player> <amount>` | Remove coins from a player | OP (level 2) |
| `/eco set <player> <amount>` | Set a player's balance | OP (level 2) |
| `/shop feature <shop>` | Toggle featured status | OP (level 2) |
| `/shop setinfinite <shop>` | Set shop items to infinite stock | OP (level 2) |
| `/shop admindelete <shop>` | Delete any shop | OP (level 2) |
| `/shop listall` | List all shops on the server | OP (level 2) |

## Installation

1. Requires **NeoForge** for Minecraft **1.21.1**
2. Place the mod jar in your `mods` folder
3. Server-side only - no client mod needed, works with vanilla clients

## Configuration

Config file is generated at `config/simpleeconomy/config.json` on first run.

| Option | Default | Description |
|--------|---------|-------------|
| `currencyName` | `"coins"` | Name of the currency |
| `startingBalance` | `100.0` | Balance given to new players |
| `taxRate` | `0.05` | Tax rate on shop sales (5%) |
| `dailyBaseReward` | `100` | Base daily reward |
| `dailyRewardIncrement` | `50` | Extra reward per streak day |
| `maxStreak` | `7` | Maximum streak days |
| `weeklyInterestRate` | `0.10` | Weekly interest rate (10%) |
| `maxInterestAmount` | `500.0` | Maximum interest payout |
| `killRewardPercent` | `0.0` | PvP kill reward (% of victim's balance) |

## For Modpack Makers

### Quest Integration
Use the `/eco add` command in quest rewards to give players coins:
```
/eco add @p 100
```

### Admin Shops
Create admin shops with infinite stock using `/shop setinfinite <shopname>`.

## Building

**Requirements:** Java 21 (required by NeoForge 1.21.1)

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## License

[MIT License](LICENSE)
