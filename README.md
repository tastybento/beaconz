# 🔺 Beaconz
### _Strategic Territory Control for Minecraft_

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![Paper](https://img.shields.io/badge/Paper-1.21.10-blue)

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
4. Use `/badmin newgame <name>` to create a game
5. Players join with `/beaconz join <game>`

### Optional Dependencies
- **Dynmap** – Territory overlay on Dynmap web interface
- **Vault** – Economy integration (planned)

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
