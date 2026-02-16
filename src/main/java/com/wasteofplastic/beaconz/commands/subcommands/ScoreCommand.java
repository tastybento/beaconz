package com.wasteofplastic.beaconz.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.commands.SubCommand;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;
import com.wasteofplastic.beaconz.game.Game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Handles the /beaconz score command.
 * Displays scores in a GUI inventory with icons for each score type.
 */
public class ScoreCommand extends BeaconzPluginDependent implements SubCommand {

    // Icons for different score types
    private static final Material ICON_BEACONS = Material.BEACON;
    private static final Material ICON_LINKS = Material.IRON_BARS; // Represents connections
    private static final Material ICON_TRIANGLES = Material.PRISMARINE_SHARD;
    private static final Material ICON_AREA = Material.MAP;

    // Fallback if a team doesn't have a color
    private static final Material TEAM_ICON_FALLBACK = Material.WHITE_BANNER;

    public ScoreCommand(Beaconz plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, Player player, String[] args) {
        if (player == null) {
            sender.sendMessage(Lang.errorOnlyPlayers);
            return true;
        }

        Game game = getGameMgr().getGame(player.getLocation());
        if (game == null || game.getScorecard() == null) {
            sender.sendMessage(Lang.errorYouMustBeInAGame);
            return true;
        }

        // Show the score GUI
        showScoreGUI(player, game);
        return true;
    }

    @Override
    public String getName() {
        return "score";
    }

    @Override
    public String getPermission() {
        return null; // No special permission required
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Player player, String[] args) {
        return new ArrayList<>();
    }

    /**
     * Shows the score GUI to the player.
     * Layout dynamically scales based on number of teams to ensure all teams are shown.
     */
    private void showScoreGUI(Player player, Game game) {
        // Refresh scores first
        game.getScorecard().refreshScores();

        List<Team> teams = new ArrayList<>(game.getScorecard().getScoreboard().getTeams());
        if (teams.isEmpty()) {
            player.sendMessage(Lang.errorNoTeams);
            return;
        }

        // Calculate inventory size and scores to show per team
        int teamCount = teams.size();
        InventoryLayout layout = calculateLayout(teamCount, game);

        // Create inventory
        Component title = MiniMessage.miniMessage().deserialize(Lang.scoreGuiTitle,
            Placeholder.component("game", game.getName()));
        Inventory inv = Bukkit.createInventory(null, layout.size, title);

        // Populate inventory with team scores
        int slot = 0;
        for (Team team : teams) {
            // Add team header item
            ItemStack teamItem = createTeamHeaderItem(team, game);
            inv.setItem(slot++, teamItem);

            // Add score items for this team
            for (int i = 0; i < layout.scoresPerTeam && i < layout.scoreTypes.length; i++) {
                GameScoreGoal scoreType = layout.scoreTypes[i];
                int score = game.getScorecard().getScore(team, scoreType);
                getLogger().info("DEBUG: Adding score " + score + " for score type " + scoreType + " for team " + team.getName());
                ItemStack scoreItem = createScoreItem(scoreType, score, team);
                inv.setItem(slot++, scoreItem);
            }
        }

        player.openInventory(inv);
    }

    /**
     * Calculate the optimal inventory layout based on team count and game score types.
     */
    private InventoryLayout calculateLayout(int teamCount, Game game) {
        // Get the score types being tracked by this game
        List<GameScoreGoal> gameScoreTypes = game.getScoretypes();
        GameScoreGoal[] scoreTypesToShow = gameScoreTypes.toArray(new GameScoreGoal[0]);
        int maxScoresAvailable = scoreTypesToShow.length;

        // Inventory sizes must be multiples of 9 (up to 54)
        int slotsPerTeam;
        int inventorySize;
        int scoresPerTeam;

        if (teamCount <= 2) {
            // 2 teams: Show all available scores (up to 4) per team
            scoresPerTeam = Math.min(maxScoresAvailable, 4);
            slotsPerTeam = 1 + scoresPerTeam; // 1 header + scores
            inventorySize = 18; // 2 rows
        } else if (teamCount <= 3) {
            // 3 teams: Show all available scores (up to 4) per team
            scoresPerTeam = Math.min(maxScoresAvailable, 4);
            slotsPerTeam = 1 + scoresPerTeam;
            inventorySize = 18; // 2 rows
        } else if (teamCount <= 5) {
            // 4-5 teams: Show all available scores (up to 4) per team
            scoresPerTeam = Math.min(maxScoresAvailable, 4);
            slotsPerTeam = 1 + scoresPerTeam;
            inventorySize = 27; // 3 rows
        } else if (teamCount <= 8) {
            // 6-8 teams: Show up to 3 scores per team
            scoresPerTeam = Math.min(maxScoresAvailable, 3);
            slotsPerTeam = 1 + scoresPerTeam;
            inventorySize = 36; // 4 rows
        } else if (teamCount <= 13) {
            // 9-13 teams: Show up to 3 scores per team
            scoresPerTeam = Math.min(maxScoresAvailable, 3);
            slotsPerTeam = 1 + scoresPerTeam;
            inventorySize = 54; // 6 rows
        } else {
            // 14-18 teams: Show up to 2 scores per team
            scoresPerTeam = Math.min(maxScoresAvailable, 2);
            slotsPerTeam = 1 + scoresPerTeam;
            inventorySize = 54; // 6 rows
        }

        return new InventoryLayout(inventorySize, scoresPerTeam, scoreTypesToShow, slotsPerTeam);
    }

    /**
     * Creates the team header item showing team name and color.
     */
    private ItemStack createTeamHeaderItem(Team team, Game game) {
        // Try to get colored banner based on team color
        Material bannerMaterial = getBannerMaterialForTeam(team);
        ItemStack item = new ItemStack(bannerMaterial);
        ItemMeta meta = item.getItemMeta();

        // Set team name as display name
        Component displayName = MiniMessage.miniMessage().deserialize(Lang.scoreGuiTeamHeader,
            Placeholder.component("team", team.displayName()));
        meta.displayName(displayName);

        // Add lore with team info
        List<Component> lore = new ArrayList<>();

        // Player count
        @SuppressWarnings("deprecation")
        int playerCount = game.getScorecard().getScoreboard().getPlayers().stream()
            .filter(p -> team.equals(game.getScorecard().getScoreboard().getPlayerTeam(p)))
            .toList().size();
        lore.add(MiniMessage.miniMessage().deserialize(Lang.scoreGuiTeamPlayers,
            Placeholder.component("count", Component.text(playerCount))));

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates a score item for a specific score type.
     */
    private ItemStack createScoreItem(GameScoreGoal scoreType, int score, Team team) {
        Material material = getIconForScoreType(scoreType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        Component displayName = MiniMessage.miniMessage().deserialize(Lang.scoreGuiScoreName,
            Placeholder.component("type", Component.text(scoreType.getName())),
            Placeholder.component("score", Component.text(score)));
        meta.displayName(displayName);

        // Add lore
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessage.miniMessage().deserialize(Lang.scoreGuiScoreTeam,
            Placeholder.component("team", team.displayName())));
        lore.add(Component.text(Lang.scoreGuiScoreValue + score).color(NamedTextColor.YELLOW));

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Get the icon material for a score type.
     */
    private Material getIconForScoreType(GameScoreGoal scoreType) {
        return switch (scoreType) {
            case BEACONS -> ICON_BEACONS;
            case LINKS -> ICON_LINKS;
            case TRIANGLES -> ICON_TRIANGLES;
            case AREA -> ICON_AREA;
            default -> Material.PAPER;
        };
    }

    /**
     * Get a colored banner material based on team name.
     * Team names are color names (e.g., "red", "blue", "orange", etc.)
     */
    private Material getBannerMaterialForTeam(Team team) {
        // Get team name and convert to uppercase for comparison
        String teamName = team.getName().toUpperCase();

        // Map team name to banner color
        return switch (teamName) {
            case "WHITE" -> Material.WHITE_BANNER;
            case "GRAY", "LIGHTGRAY" -> Material.LIGHT_GRAY_BANNER;
            case "BLACK" -> Material.BLACK_BANNER;
            case "RED" -> Material.RED_BANNER;
            case "ORANGE" -> Material.ORANGE_BANNER;
            case "YELLOW" -> Material.YELLOW_BANNER;
            case "LIME" -> Material.LIME_BANNER;
            case "GREEN" -> Material.GREEN_BANNER;
            case "CYAN", "LIGHTBLUE" -> Material.CYAN_BANNER;
            case "BLUE" -> Material.BLUE_BANNER;
            case "PURPLE" -> Material.PURPLE_BANNER;
            case "MAGENTA", "PINK" -> Material.MAGENTA_BANNER;
            case "BROWN" -> Material.BROWN_BANNER;
            default -> TEAM_ICON_FALLBACK; // White banner as fallback
        };
    }

    /**
         * Helper class to store inventory layout calculations.
         */
        private record InventoryLayout(int size, int scoresPerTeam, GameScoreGoal[] scoreTypes, int slotsPerTeam) {
    }
}
