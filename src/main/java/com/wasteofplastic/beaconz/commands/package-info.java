/**
 * Command handlers for player and administrative commands.
 * <p>
 * This package contains command executors that process player and admin commands
 * for interacting with the Beaconz game system.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.commands.CmdHandler} - Player commands ({@code /beaconz})</li>
 *   <li>{@link com.wasteofplastic.beaconz.commands.AdminCmdHandler} - Administrative commands ({@code /badmin})</li>
 * </ul>
 *
 * <h2>Player Commands ({@code /beaconz}):</h2>
 * Available to all players for game interaction:
 * <ul>
 *   <li><b>help:</b> Show available commands</li>
 *   <li><b>join [game]:</b> Join a game instance</li>
 *   <li><b>leave:</b> Leave current game and return to lobby</li>
 *   <li><b>team [team]:</b> Join a specific team</li>
 *   <li><b>score:</b> Show current game scores</li>
 *   <li><b>lobby:</b> Teleport to lobby</li>
 * </ul>
 *
 * <h2>Admin Commands ({@code /badmin}):</h2>
 * Require {@code beaconz.admin} permission:
 * <ul>
 *   <li><b>newgame [name]:</b> Create a new game instance</li>
 *   <li><b>delgame [name]:</b> Delete a game instance</li>
 *   <li><b>start [game]:</b> Start a game</li>
 *   <li><b>stop [game]:</b> Stop a game</li>
 *   <li><b>setspawn:</b> Set team spawn points</li>
 *   <li><b>addteam [color]:</b> Add a team to a game</li>
 *   <li><b>removeteam [color]:</b> Remove a team from a game</li>
 *   <li><b>reload:</b> Reload configuration</li>
 *   <li><b>save:</b> Save all game data</li>
 *   <li><b>listgames:</b> List all game instances</li>
 *   <li><b>listbeacons:</b> List beacons in a region</li>
 *   <li><b>tp [game|lobby]:</b> Teleport to game or lobby</li>
 * </ul>
 *
 * <h2>Command Processing:</h2>
 * Both handlers follow a similar pattern:
 * <ol>
 *   <li>Validate sender type (player vs console)</li>
 *   <li>Check permissions</li>
 *   <li>Parse sub-command and arguments</li>
 *   <li>Validate arguments</li>
 *   <li>Execute command logic</li>
 *   <li>Send feedback message</li>
 * </ol>
 *
 * <h2>Tab Completion:</h2>
 * Both command handlers provide tab completion for:
 * <ul>
 *   <li>Sub-command names</li>
 *   <li>Game names</li>
 *   <li>Team colors</li>
 *   <li>Player names (where applicable)</li>
 * </ul>
 *
 * <h2>Error Handling:</h2>
 * Commands validate input and provide clear error messages:
 * <ul>
 *   <li>Invalid sub-command → Show help</li>
 *   <li>Wrong arguments → Show usage</li>
 *   <li>Invalid game name → List available games</li>
 *   <li>Permission denied → Permission error message</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.wasteofplastic.beaconz.commands;
