# Beaconz - Copilot Instructions

## Project Overview

**Beaconz** is a competitive territory control Minecraft plugin for Paper servers (1.21+). Teams capture beacons, create links between them, and form triangular control fields to score points. Think "Ingress meets Minecraft."

- **Language**: Java 21
- **Build Tool**: Maven 3.6+
- **Framework**: Paper API 1.21.11
- **Testing**: JUnit 5 + MockBukkit
- **Project Type**: Minecraft Paper plugin (JAR)
- **Repository Size**: ~50 Java source files, ~20 test files

## Building and Testing

### Prerequisites
- Java 21 JDK (required - uses modern Java features like switch expressions, records, pattern matching)
- Maven 3.6+ for building
- Paper 1.21.10+ server for runtime testing

### Build Commands

**ALWAYS run `mvn clean package` to build the project:**
```bash
mvn clean package
```
- Output: `target/Beaconz-2.0.0-SNAPSHOT-LOCAL.jar`
- Build time: ~10-20 seconds (excluding dependency downloads)
- The build uses maven-shade-plugin to relocate dependencies (HikariCP, SQLite)

**To run tests:**
```bash
mvn clean test
```
- Uses JUnit 5 with MockBukkit for Bukkit API mocking
- Test time: ~5-10 seconds
- Tests may fail if dependencies cannot be downloaded from JitPack/Maven Central

**Skip tests during build (useful for quick iterations):**
```bash
mvn clean package -DskipTests
```

### CI/CD Pipeline

The project uses GitHub Actions (`.github/workflows/maven.yml`):
- **Triggers**: Push/PR to `develop` branch
- **Steps**: 
  1. Checkout code
  2. Set up JDK 21 (Temurin distribution)
  3. Build with Maven (`mvn -B package`)
  4. Upload JAR artifact
- **Requirements**: All builds must pass on `develop` branch before merging

## Project Architecture

### Package Structure

```
com.wasteofplastic.beaconz/
├── Beaconz.java              # Main plugin class (initialization, dependency injection)
├── BeaconzPluginDependent.java # Base class for plugin-dependent components
├── commands/                 # Command handlers
│   ├── CmdHandler.java       # Player command processor (/beaconz)
│   ├── AdminCmdHandler.java  # Admin command processor (/badmin)
│   └── subcommands/          # Individual command implementations
├── listeners/                # Event listeners for game mechanics
│   ├── BeaconCaptureListener.java
│   ├── BeaconLinkListener.java
│   ├── BeaconProtectionListener.java
│   ├── PlayerMovementListener.java
│   └── [... 10+ other listeners]
├── game/                     # Core game logic
│   ├── Game.java             # Individual game instance
│   ├── GameMgr.java          # Multi-game manager
│   ├── Register.java         # Beacon & triangle registry (spatial indexing)
│   ├── Scorecard.java        # Team scoring & scoreboard management
│   ├── BeaconObj.java        # Beacon data model
│   └── TriangleField.java    # Geometric triangle calculations
├── map/                      # Territory map rendering system
│   ├── TerritoryMapRenderer.java  # Main map renderer (complex caching logic)
│   ├── MapCoordinateConverter.java
│   ├── BeaconMap.java
│   └── TeamCursor.java       # Record for cursor data
├── storage/                  # Data persistence layer
│   ├── BeaconzStore.java     # SQLite database manager
│   ├── Messages.java         # Message storage
│   └── NameDB.java           # Player name cache
├── config/                   # Configuration management
│   ├── Lang.java             # Language strings
│   └── Params.java           # Game parameters & settings
├── integration/              # Optional plugin integrations
│   └── dynmap/               # Dynmap web map integration
├── generator/                # Custom world generation
│   └── BeaconzWorldGen.java  # Flat world generator for game regions
└── util/                     # Utility classes
```

### Key Design Patterns

- **Registry Pattern**: `Register.java` - Central beacon/triangle registration with spatial indexing
- **Observer Pattern**: Event-driven game mechanics via Bukkit event listeners
- **State Pattern**: Game lifecycle management (lobby → active → ended)
- **Caching Pattern**: Extensive use in map rendering, triangle queries, color gradients
- **Dependency Injection**: `BeaconzPluginDependent` provides plugin instance to all components
- **Command Pattern**: Modular command handlers in `commands/subcommands/`

### Modern Java Features Used

This codebase leverages Java 21 capabilities extensively:
- **Switch Expressions**: Material → color mapping (see `TerritoryMapRenderer.java`)
- **Records**: `TeamCursor` for immutable cursor data
- **Pattern Matching**: instanceof with variable binding
- **Text Blocks**: Multi-line string literals for messages
- **Enhanced Type Inference**: Simplified generics

**IMPORTANT**: When modifying code, maintain consistency with existing Java 21 idioms.

## Configuration Files

- **`src/main/resources/config.yml`**: Main plugin configuration
- **`src/main/resources/plugin.yml`**: Bukkit plugin metadata (commands, permissions, dependencies)
- **`src/main/resources/dynmap.yml`**: Dynmap integration settings
- **`src/main/resources/locale/*.yml`**: Localization files (en-US primary)
- **`pom.xml`**: Maven build configuration

## Testing Guidelines

### Test Framework
- **JUnit 5** (`org.junit.jupiter.*`)
- **MockBukkit** (`org.mockbukkit.mockbukkit.*`) for Bukkit API mocking
- **Mockito** for additional mocking needs

### Test Structure
Tests follow the production package structure in `src/test/java/`:
```
com.wasteofplastic.beaconz/
├── BeaconzTest.java           # Main plugin tests
├── commands/
│   ├── CmdHandlerTest.java
│   └── AdminCmdHandlerTest.java
├── map/
│   ├── TerritoryMapRendererTest.java
│   └── BeaconMapTest.java
└── listeners/
    └── BeaconCaptureListenerTest.java
```

### Writing Tests
1. **Setup**: Use `MockBukkit.mock()` in `@BeforeEach` to initialize server mock
2. **Teardown**: ALWAYS call `MockBukkit.unmock()` in `@AfterEach` to prevent memory leaks
3. **Lang Initialization**: Many tests require initializing `Lang` static strings to prevent NPEs
4. **Test Organization**: Use `@Nested` classes for logical grouping (see `BeaconzTest.java`)

### Example Test Pattern
```java
@BeforeEach
void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.load(Beaconz.class);
    // Initialize Lang if needed
    Lang.setup();
}

@AfterEach
void tearDown() {
    MockBukkit.unmock();
}
```

## Code Style and Conventions

### General Style
- **Comments**: Extensive JavaDoc and inline comments explaining the "why", not just the "what"
- **Null Safety**: Use `@NotNull` and `@Nullable` annotations (from `org.jspecify.annotations`)
- **Method Names**: Descriptive, verb-based (e.g., `calculateTriangleArea`, `isBeaconOwned`)
- **Constants**: UPPER_SNAKE_CASE
- **Formatting**: Standard Java conventions (4-space indentation, no tabs)

### Key Coding Patterns
1. **Dependency Injection**: Extend `BeaconzPluginDependent` to access plugin instance
2. **Event Handling**: All listeners extend `BeaconzPluginDependent` and use Bukkit event annotations
3. **Error Handling**: Use `plugin.logError(message)` for error logging
4. **Messages**: Always use `Lang` or `Messages` for user-facing text (never hardcode strings)
5. **Database Operations**: All storage operations go through `BeaconzStore` (SQLite with HikariCP)

### Performance Considerations
- **Caching**: The map renderer uses aggressive caching - understand cache invalidation before modifying
- **Spatial Indexing**: `Register` maintains spatial indexes for fast triangle lookups - preserve data structures
- **Lazy Evaluation**: Many calculations are deferred until needed (e.g., triangle area computation)

## Common Pitfalls and Known Issues

### Build Issues
1. **JitPack Failures**: The build may fail if JitPack (used for MockBukkit, VaultAPI) is unreachable. This is expected in restricted environments.
2. **Dependency Resolution**: First build may take longer due to dependency downloads
3. **Shading**: HikariCP and SQLite are shaded - don't add conflicting versions

### Test Issues
1. **MockBukkit NPEs**: Some tests may fail if Lang strings aren't initialized
2. **World Initialization**: Full plugin initialization requires world creation, which may not complete in MockBukkit tests
3. **Async Operations**: Bukkit scheduler tasks may not execute as expected in tests

### Runtime Issues
1. **Beacon Registration**: Beacons must be registered in `Register` for territory calculations to work
2. **Triangle Computation**: Requires at least 3 linked beacons owned by the same team
3. **Map Updates**: Territory maps only refresh when beacon ownership changes (by design)

## Important Files to Understand

Before making changes to specific areas, review these key files:

1. **`TerritoryMapRenderer.java`** - Complex caching logic, coordinate transformations, color gradients
   - Most commented file in the project
   - Demonstrates Paper map API usage
   - Performance-critical code

2. **`Register.java`** - Central registry with spatial indexing
   - Beacon ownership tracking
   - Triangle computation algorithms
   - Graph algorithms for link validation

3. **`Game.java`** & **`GameMgr.java`** - Game state management
   - Lifecycle management
   - Player assignment
   - Victory conditions

4. **`Scorecard.java`** - Bukkit scoreboard integration
   - Team management
   - Score tracking
   - Timer systems

## Making Changes

### Before Starting
1. **Read the README.md** - Contains detailed architecture documentation
2. **Check existing tests** - Understand test patterns before adding new tests
3. **Review similar code** - Find similar functionality and maintain consistency

### Development Workflow
1. Make surgical, minimal changes
2. Build with `mvn clean package` to verify compilation
3. Run tests with `mvn test` to catch regressions
4. Test in a Paper server if making runtime changes
5. Update relevant JavaDoc if changing public APIs

### Areas Requiring Extra Care
- **Map Rendering** (`map/`): Complex caching logic, easy to break performance
- **Triangle Calculations** (`game/TriangleField.java`): Geometric math, test thoroughly
- **Database Operations** (`storage/`): Ensure proper connection handling
- **Event Listeners** (`listeners/`): Consider event priority and cancellation

## Dependencies

### Runtime Dependencies (Provided)
- Paper API 1.21.11 (provided by server)
- VaultAPI 1.7.1 (optional, for economy integration)
- Dynmap (optional, for web map overlay)

### Bundled Dependencies (Shaded)
- HikariCP 7.0.2 (connection pooling) → `com.wasteofplastic.beaconz.lib.hikari`
- SQLite JDBC 3.47.2.0 (database) → `com.wasteofplastic.beaconz.lib.sqlite`

### Test Dependencies
- JUnit 5.10.2
- MockBukkit v1.21-SNAPSHOT
- Mockito 5.11.0

**IMPORTANT**: Don't add new dependencies without strong justification. Prefer using existing Paper API or JDK functionality.

## Additional Notes

- **Branch Strategy**: Work on `develop` branch, not `master`
- **Version Numbering**: Currently 2.0.0-SNAPSHOT (rewrite of legacy 1.x version)
- **Paper API**: This is a Paper plugin, not a Spigot plugin - use Paper APIs where available
- **World Generation**: Custom flat world generator in `BeaconzWorldGen.java`
- **Localization**: All user-facing text goes through `Lang` or `Messages` - never hardcode strings

## Trust These Instructions

These instructions have been validated against the current codebase. Only search for additional information if these instructions are incomplete or you encounter unexpected behavior that contradicts what's documented here.
