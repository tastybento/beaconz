# 🔺 Beaconz
### _Strategic Territory Control for Minecraft_

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![Paper](https://img.shields.io/badge/Paper-1.21.10-blue)

---

## 📑 Table of Contents

- [What is Beaconz?](#-what-is-beaconz)
  - [Core Gameplay Loop](#-core-gameplay-loop)
  - [What Makes It Special](#-what-makes-it-special)
- [Architecture Overview](#-architecture-overview)
  - [Package Structure](#-package-structure)
  - [Key Components](#-key-components)
  - [Design Patterns Used](#-design-patterns-used)
  - [Modern Java Features](#-modern-java-features)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Building](#building)
  - [Running](#running)
  - [Quick Start Example](#quick-start-example)
- [Commands Reference](#-commands-reference)
  - [Player Commands](#-player-commands)
  - [Admin Commands](#-admin-commands)
- [Permissions](#-permissions)
- [For Developers](#-for-developers)
  - [Contributing](#contributing)
  - [Code Style](#code-style)
  - [Learning from the Code](#learning-from-the-code)
  - [API Highlights](#api-highlights)
- [History](#-history)
- [Credits](#-credits)
- [License](#-license)
- [Links](#-links)

---

## 🎮 What is Beaconz?

**Beaconz** is a competitive territory control minigame that transforms Minecraft beacons into strategic objectives. Born from the **Silicon Valley Minecraft Meetup** over a decade ago, this 2.0 rewrite brings the classic gameplay into the modern era with cutting-edge Paper API integration and Java 21 features.

Think **Ingress** meets **Minecraft** – teams compete to capture beacons, create links between them, and form triangular control fields that claim territory. The larger your triangular territories, the more points you score. Overlap territories for even greater strategic depth!

### 🎯 Core Gameplay Loop

1. **Capture Beacons** – Find and claim scattered beacons across the world
2. **Create Links** – Connect your beacons with other friendly beacons  
3. **Form Triangles** – Three linked beacons create a control field
4. **Control Territory** – The area inside your triangles scores points
5. **Defend & Attack** – Protect your network while disrupting enemies
6. **Win the Game** – Achieve the goal (most territory, triangles, or beacons)

### ✨ What Makes It Special

- **📊 Real-time Territory Visualization** – Custom map rendering shows colored territories with darker shading where triangles overlap
- **⚔️ Active Defense Systems** – Beacons fight back with defensive blocks and projectile launchers
- **🎨 16 Team Colors** – Support for wool, concrete, terracotta, and stained glass team materials
- **🗺️ Smart Map System** – Interactive maps display beacons, links, territories, and player positions
- **🏆 Flexible Scoring** – Win by area controlled, beacons captured, links created, or triangles formed
- **⚡ Performance Optimized** – Intelligent caching and lazy evaluation for smooth gameplay even with hundreds of beacons

---

## 🏗️ Architecture Overview

For plugin developers looking to understand, extend, or learn from the codebase:

### 📦 Package Structure

```
com.wasteofplastic.beaconz/
├── commands/           # Command handlers (player & admin)
├── listeners/          # Event listeners for game mechanics
├── map/               # Territory map rendering system
├── dynmap/            # Optional Dynmap integration
├── Game.java          # Core game instance & state
├── GameMgr.java       # Multi-game manager
├── Register.java      # Beacon & triangle registry
├── Scorecard.java     # Team scoring & scoreboard
├── TriangleField.java # Geometric triangle calculations
└── BeaconObj.java     # Beacon data & linking logic
```

### 🔑 Key Components

#### **Game Management Layer**
- **`Beaconz.java`** – Main plugin class, initialization, world setup
- **`GameMgr.java`** – Manages multiple concurrent games across regions
- **`Game.java`** – Individual game instance with players, settings, lifecycle
- **`Scorecard.java`** – Tracks team scores, manages Minecraft scoreboards, handles timers

#### **Territory System**
- **`Register.java`** – Central registry for beacons and triangle fields
  - Spatial indexing for fast "what triangle contains this point?" queries
  - Link validation and network integrity checking
  - Triangle computation from beacon networks
  
- **`TriangleField.java`** – Geometric representation of controlled territory
  - 2D polygon math for point-in-triangle tests
  - Area calculation for scoring
  - Line intersection detection for field interactions

- **`BeaconObj.java`** – Beacon data model
  - Ownership tracking
  - Link management (graph of connected beacons)
  - Passive defense block coordination

#### **Map Rendering Engine**
- **`TerritoryMapRenderer.java`** – The crown jewel 👑
  - Real-time territory visualization on Minecraft maps
  - Color gradient system (bright = 1 triangle, dark = many overlapping)
  - Intelligent caching (only recomputes when beacons change)
  - Supports all 16 dye colors across 4 material types
  - **See full documentation**: Extensively commented for learning

- **`MapCoordinateConverter.java`** – Handles world ↔ pixel ↔ cursor transformations

#### **Defense Systems**
- **`DefenseBlock.java`** – Passive defense block mechanics
- **`BeaconPassiveDefenseListener.java`** – Handles defense block placement/activation
- **`BeaconProjectileDefenseListener.java`** – Active projectile launching system

#### **Event Listeners**
| Listener | Purpose |
|----------|---------|
| `BeaconCaptureListener` | Handles beacon claiming/capturing |
| `BeaconLinkListener` | Manages link creation between beacons |
| `BeaconProtectionListener` | Protects beacons from griefing |
| `BeaconSurroundListener` | Enforces beacon placement rules |
| `PlayerMovementListener` | Region boundaries & movement restrictions |
| `PlayerDeathListener` | Death handling in game zones |
| `ChatListener` | Team-based chat filtering |
| `LobbyListener` | Lobby mechanics & game joining |

### 🎨 Design Patterns Used

- **Registry Pattern** – Central beacon/triangle registration
- **Observer Pattern** – Event-driven game mechanics
- **State Pattern** – Game lifecycle management (lobby → active → ended)
- **Caching Pattern** – Map rendering, triangle queries, color gradients
- **Dependency Injection** – `BeaconzPluginDependent` base class
- **Command Pattern** – Modular command handlers

### 🔧 Modern Java Features

This rewrite leverages **Java 21** capabilities:
- ✅ **Switch Expressions** – Cleaner material → color mapping
- ✅ **Records** – `TeamCursor` for immutable data
- ✅ **Enhanced Type Inference** – Less verbose generics
- ✅ **Text Blocks** – Cleaner multi-line strings
- ✅ **Pattern Matching** – instanceof with variable binding

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** or higher
- **Paper 1.21.10+** (or compatible fork)
- **Maven 3.6+** for building

### Building

```bash
git clone https://github.com/tastybento/beaconz.git
cd beaconz
mvn clean package
```

The compiled JAR will be in `target/Beaconz-2.0.0-SNAPSHOT.jar`

### Running

1. Copy the JAR to your Paper server's `plugins/` folder
2. Start/restart the server
3. Configure `plugins/Beaconz/config.yml` to your liking
4. Use `/badmin newgame <name>` to create a game (see [Commands Reference](#-commands-reference) for details)
5. Set team spawns with `/badmin setspawn <team>`
6. Players teleport to lobby with `/beaconz` and are auto-assigned to teams

### Optional Dependencies
- **Dynmap** – Territory overlay on Dynmap web interface
- **Vault** – Economy integration (planned)

### Quick Start Example

```bash
# As an admin, create a simple 2-team game
/badmin newgame quickmatch teams:2 goal:triangles goalvalue:5

# Set spawns for each team (stand at desired location)
/badmin setspawn red
/badmin setspawn blue

# As a player, join the game
/beaconz                    # Teleport to lobby
/beaconz score              # Check your team assignment and scores
```

See the complete [Commands Reference](#-commands-reference) below for all available commands and options.

---

## 📋 Commands Reference

### 🎮 Player Commands

Players use the `/beaconz` (or `/bz`) command to interact with the game:

| Command | Permission | Description |
|---------|-----------|-------------|
| `/beaconz` | `beaconz.player` | Teleport to the lobby spawn point |
| `/beaconz help` | `beaconz.player` | Display help for all available player commands |
| `/beaconz score` | `beaconz.player` | View current game scores and your team |
| `/beaconz sb` | `beaconz.player` | Toggle scoreboard display on/off |
| `/beaconz leave <game>` | `beaconz.player.leave` | Leave a game and return to lobby |
| `/beaconz join <game>` | Operator only | Admin bypass to force join any game (undocumented) |

**Examples:**
```
/beaconz                    # Teleport to lobby
/beaconz score              # Check your team and game scores
/beaconz sb                 # Show/hide scoreboard
/beaconz leave mygame       # Leave the game "mygame"
```

### 🛠️ Admin Commands

Admins use the `/badmin` (or `/bzadmin`) command to manage games and players:

#### Game Management

| Command | Description |
|---------|-------------|
| `/badmin newgame <name> [params...]` | Create a new game with optional custom parameters |
| `/badmin delete <gamename>` | Permanently delete a game (cannot be undone!) |
| `/badmin games` | List all active games and their regions |
| `/badmin listparms <gamename>` | Display all parameters for a specific game |
| `/badmin force_end <gamename>` | Immediately end a game and declare winner |
| `/badmin reload` | Save state and reload all configuration files |

**Game Creation Parameters:**

When creating a new game with `/badmin newgame`, you can specify these optional parameters:

- `gamemode:minigame|strategy` - Set game mode (default: minigame)
- `size:<number>` - Set region size (e.g., `size:500`)
- `teams:<number>` - Number of teams (e.g., `teams:2`)
- `goal:area|beacons|links|triangles` - Victory condition
- `goalvalue:<number>` - Target value for goal (0 = unlimited)
- `countdown:<seconds>` - Game timer (0 = count up, >0 = countdown)
- `scoretypes:<types>` - Scores to display (e.g., `area-triangles-beacons-links`)
- `distribution:<0.01-0.99>` - Beacon spawn probability per chunk

**Examples:**
```
/badmin newgame pvp1                                    # Create game with defaults
/badmin newgame ctf teams:2 goal:beacons goalvalue:10   # Capture 10 beacons to win
/badmin newgame mega size:1000 teams:4 goal:area        # Large 4-team area control
/badmin listparms pvp1                                  # View game settings
/badmin delete oldgame                                  # Remove a game
```

#### Player Management

| Command | Description |
|---------|-------------|
| `/badmin join <gamename> <team>` | Force yourself to join a specific team |
| `/badmin kick <player> <gamename>` | Remove a player from a game (sends to lobby) |
| `/badmin kick all <gamename>` | Remove all players from a game |
| `/badmin switch` | Switch yourself to another team in your current game |
| `/badmin switch <player>` | Switch another player to a different team |
| `/badmin teams all` | Display rosters for all games |
| `/badmin teams <gamename>` | Display team rosters for a specific game |

**Examples:**
```
/badmin join pvp1 red                # Join the red team in pvp1
/badmin kick PlayerName pvp1         # Kick a player from pvp1
/badmin switch                       # Switch to another team
/badmin teams pvp1                   # View team rosters
```

#### Beacon Management

| Command | Description |
|---------|-------------|
| `/badmin claim <team>` | Assign beacon you're standing on to a team |
| `/badmin claim unowned` | Mark beacon you're standing on as unowned |
| `/badmin list all <team>` | List all beacons, optionally filtered by team |
| `/badmin list <gamename> <team>` | List beacons in a game, optionally by team |
| `/badmin distribution <0.0-1.0>` | Set beacon spawn probability |

**Examples:**
```
/badmin claim red                    # Claim beacon for red team (stand on it)
/badmin claim unowned                # Unclaim the beacon
/badmin list pvp1                    # List all beacons in pvp1
/badmin list pvp1 blue               # List blue team's beacons in pvp1
/badmin distribution 0.3             # 30% chance per chunk
```

#### World & Spawn Management

| Command | Description |
|---------|-------------|
| `/badmin setspawn` | Set lobby spawn point (stand where you want spawn) |
| `/badmin setspawn <team>` | Set team spawn point for current game |

**Examples:**
```
/badmin setspawn                     # Set lobby spawn to your location
/badmin setspawn red                 # Set red team spawn (in game region)
```

---

## 🔐 Permissions

### Player Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `beaconz.player` | true | Basic player access - use `/beaconz` commands |
| `beaconz.player.leave` | op | Ability to leave games with `/beaconz leave` |

### Admin Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `beaconz.admin` | op | Full admin access - use all `/badmin` commands |

**Notes:**
- Server operators have all permissions by default
- Non-op players can participate in games with just `beaconz.player`
- The `leave` permission can be granted to all players if desired
- Use your permission plugin (LuckPerms, PermissionsEx, etc.) to customize access

**Example Permission Setup (LuckPerms):**
```bash
# Give all players basic access
/lp group default permission set beaconz.player true

# Allow players to leave games on their own
/lp group default permission set beaconz.player.leave true

# Grant admin access to moderators
/lp group moderator permission set beaconz.admin true
```

---

## 📚 For Developers

### Contributing

We welcome contributions! Areas of focus:
- 🐛 Bug fixes and stability improvements  
- ⚡ Performance optimizations
- 🎨 New game modes and victory conditions
- 🛠️ Admin tools and management features
- 📖 Documentation and examples

### Code Style
- **Modern Java practices** – Use Java 21 features where appropriate
- **Comprehensive comments** – Explain the "why", not just the "what"
- **Null safety** – Use `@NotNull` and `@Nullable` annotations
- **Testing** – Unit tests with JUnit 5 + MockBukkit

### Learning from the Code

**Best places to start:**
1. **`TerritoryMapRenderer.java`** – Excellent example of caching, coordinate systems, and Paper map API
2. **`TriangleField.java`** – Clean geometric computation with Java AWT
3. **`Register.java`** – Spatial indexing and graph algorithms
4. **`Scorecard.java`** – Bukkit scoreboard API usage and team management

### API Highlights

The codebase demonstrates modern Paper API usage:
- ✅ Map rendering with `MapCanvas` and `MapView`
- ✅ Scoreboard teams and objectives
- ✅ Custom world generation (`ChunkGenerator`)
- ✅ Particle effects and visual feedback
- ✅ Persistent data storage with YAML
- ✅ Event-driven architecture

---

## 📜 History

**2015** – Created by the **Silicon Valley Minecraft Meetup** community  
**2015-2020** – Active development and gameplay refinement  
**2021-2024** – Maintenance mode as Minecraft APIs evolved  
**2025** – Complete rewrite for **Minecraft 1.21+** with modern Java and Paper API

The original vision was to create a game that combined strategic thinking with Minecraft's creativity. This rewrite honors that vision while bringing it into the modern Minecraft ecosystem.

---

## 🙏 Credits

- **Original Concept** – Silicon Valley Minecraft Meetup
- **Original Development** – tastybento & community contributors  
- **2.0 Rewrite** – tastybento
- **Inspired by** – Niantic's Ingress

---

## 📄 License

MIT License – See [LICENSE.txt](LICENSE.txt) for details

---

## 🔗 Links

- **Issues & Bugs**: [GitHub Issues](https://github.com/tastybento/beaconz/issues)
- **Discussions**: [GitHub Discussions](https://github.com/tastybento/beaconz/discussions)
- **SpigotMC**: _(coming soon)_

---

### 💡 "Capture. Link. Control. Conquer."
