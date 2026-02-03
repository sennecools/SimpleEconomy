# SimpleEconomy

A simple economy and player shop system for Minecraft NeoForge 1.21.1.

## Features

### Economy System
- **Player Balances**: Each player has a coin balance that persists across sessions
- **Commands**:
  - `/balance` or `/bal` - Check your current balance
  - `/balance <player>` - Check another player's balance (requires permission level 2)
  - `/pay <player> <amount>` - Send coins to another player
  - `/eco add <player> <amount>` - Add coins to a player (admin)
  - `/eco remove <player> <amount>` - Remove coins from a player (admin)
  - `/eco set <player> <amount>` - Set a player's balance (admin)

### Shop System
- **Player Shops**: Players can create up to 3 shops each
- **Shop Browser**: Use `/shops`, `/shop`, or `/marketplace` to open the GUI
- **Player Heads**: Each shop displays the owner's head as an icon
- **Categories**: Shops can be categorized (General, Tools, Weapons, Armor, Food, Blocks, Redstone, Materials, Magic, Modded)
- **Search**: Search for items or shops across the entire marketplace
- **Favorites**: Bookmark your favorite shops for quick access
- **Sorting**: Sort by Featured, Favorites, Most Sales, Newest, or Alphabetical

### Shop Management
- Create shops with `/shop create <name>` or via the GUI
- Add items from your inventory with custom prices
- Remove items (returns remaining stock to your inventory)
- Edit shop name, description, and category
- Delete shops when no longer needed

### Economy Features
- **5% Transaction Tax**: A small tax on all shop sales to act as a money sink
- **Offline Sales**: Earn money even when offline
- **Sale Notifications**: Get notified when someone buys from your shop
- **Transaction Log**: Track your purchase and sale history

## Installation

1. Requires NeoForge for Minecraft 1.21.1
2. Place the mod jar in your `mods` folder
3. The mod works on both client and server

## Building

**Requirements:** Java 21 (required by NeoForge 1.21.1)

```bash
# Make sure JAVA_HOME points to Java 21
export JAVA_HOME=/path/to/java21

# Build the mod
./gradlew build
```

The built jar will be in `build/libs/`.

**Note:** If you have a newer Java version (22+), you'll need to install Java 21 specifically for building NeoForge mods.

## For Modpack Makers

### Quest Integration
Use the `/eco add` command in quest rewards to give players coins:
```
/eco add @p 100
```

### Admin Shops
Server admins can create shops with infinite stock by manually editing the saved data or using datapacks.

## Configuration

Currently, the following values are hardcoded but can be modified in the source:
- Starting balance: 0 coins
- Transaction tax: 5%
- Max shops per player: 3
- Max items per shop: 27

## License

MIT License
