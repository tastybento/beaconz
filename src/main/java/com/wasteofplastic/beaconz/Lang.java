package com.wasteofplastic.beaconz;

import java.io.File;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


/**
 * This class holds all the text strings to enabling localization.
 * @author tastybento
 *
 */
public class Lang extends BeaconzPluginDependent {
    private FileConfiguration locale = null;

    public Lang(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    public static HashMap<Material, String> defenseText;
    public static String actionsDistributionSettingTo;
    public static String actionsHitSign;
    public static String actionsSwitchedToTeam;
    public static String actionsYouAreInTeam;
    public static String adminDeletedGame;
    public static String adminDeletingGame;
    public static String adminForceEnd;
    public static String adminForceRestart;
    public static String adminGamesDefined;
    public static String adminGamesNoOthers;
    public static String adminGamesTheLobby;
    public static String adminKickAllPlayers;
    public static String adminKickPlayer;
    public static String adminListBeaconsInGame;
    public static String adminNewGameBuilding;
    public static String adminParmsArgumentsPairs;
    public static String adminParmsCountdown;
    public static String adminParmsDoesNotExist;
    public static String adminParmsGoal;
    public static String adminParmsGoalValue;
    public static String adminParmsMode;
    public static String adminParmsScoreTypes;
    public static String adminParmsTeams;
    public static String adminParmsUnlimited;
    public static String adminPaused;
    public static String adminRegenComplete;
    public static String adminRegeneratingGame;
    public static String adminReload;
    public static String adminRestart;
    public static String adminResume;
    public static String adminSetSpawnNeedToBeInGame;
    public static String beaconCannotBeExtended;
    public static String beaconCannotPlaceLiquids;
    public static String beaconClaimedForTeam;
    public static String beaconClaimingBeaconAt;
    public static String beaconDefensePlaced;
    public static String beaconDefenseRemoveTopDown;
    public static String beaconExtended;
    public static String beaconIsExhausted;
    public static String beaconLinkAlreadyExists;
    public static String beaconLinkBlockBroken;
    public static String beaconLinkBlockPlaced;
    public static String beaconLinkCannotCrossEnemy;
    public static String beaconLinkCouldNotBeCreated;
    public static String beaconLinkCreated;
    public static String beaconLinkLost;
    public static String beaconMapBeaconMap;
    public static String beaconMapUnknownBeacon;
    public static String beaconMaxLinks;
    public static String beaconNameCreateATriangle;
    public static String beaconNameCreatedALink;
    public static String beaconNameCreateTriangles;
    public static String beaconNowHasLinks;
    public static String beaconOriginNotOwned;
    public static String beaconPlayerDestroyed;
    public static String beaconTeamDestroyed;
    public static String beaconTheMapDisintegrates;
    public static String beaconTriangleCreated;
    public static String beaconYouCannotDestroyYourOwnBeacon;
    public static String beaconYouCannotLinkToSelf;
    public static String beaconYouCanOnlyExtend;
    public static String beaconYouCapturedABeacon;
    public static String beaconYouDestroyed;
    public static String beaconYouHaveAMap;
    public static String beaconYouHaveThisMuchExp;
    public static String beaconYouMustCapturedBeacon;
    public static String beaconYouNeedThisMuchExp;
    public static String beaconYouReceivedAReward;
    public static String errorAlreadyExists;
    public static String errorCanOnlyPlaceBlocks;
    public static String errorCanOnlyPlaceBlocksUpTo;
    public static String errorClearAboveBeacon;
    public static String errorClearAroundBeacon;
    public static String errorDistribution;
    public static String errorError;
    public static String errorInventoryFull;
    public static String errorNoBeaconThere;
    public static String errorNoGames;
    public static String errorNoSuchGame;
    public static String errorNoSuchTeam;
    public static String errorNotEnoughExperience;
    public static String errorNotInGame;
    public static String errorNotInRegister;
    public static String errorNotReady;
    public static String errorOnlyPlayers;
    public static String errorRegionLimit;
    public static String errorTooFar;
    public static String errorUnknownCommand;
    public static String errorUnknownPlayer;
    public static String errorYouCannotBuildThere;
    public static String errorYouCannotDoThat;
    public static String errorYouCannotRemoveOtherPlayersBlocks;
    public static String errorYouDoNotHavePermission;
    public static String errorYouHaveToBeStandingOnABeacon;
    public static String errorYouMustBeInAGame;
    public static String errorYouMustBeInATeam;
    public static String errorYouNeedToBeLevel;
    public static String generalFailure;
    public static String generalGame;
    public static String generalGames;
    public static String generalLevel;
    public static String generalLinks;
    public static String generalLocation;
    public static String generalMembers;
    public static String generalNone;
    public static String generalSuccess;
    public static String generalTeam;
    public static String generalTeams;
    public static String generalUnowned;
    public static String helpAdminClaim;
    public static String helpAdminDelete;
    public static String helpAdminDistribution;
    public static String helpAdminForceEnd;
    public static String helpAdminGames;
    public static String helpAdminJoin;
    public static String helpAdminKick;
    public static String helpAdminLink;
    public static String helpAdminList;
    public static String helpAdminListParms;
    public static String helpAdminNewGame;
    public static String helpAdminPause;
    public static String helpAdminRegenerate;
    public static String helpAdminReload;
    public static String helpAdminRestart;
    public static String helpAdminResume;
    public static String helpAdminSetGameParms;
    public static String helpAdminSetLobbySpawn;
    public static String helpAdminSetTeamSpawn;
    public static String helpAdminSwitch;
    public static String helpAdminTeams;
    public static String helpAdminTimerToggle;
    public static String helpAdminTitle;
    public static String helpHelp;
    public static String helpJoin;
    public static String helpLeave;
    public static String helpLine;
    public static String helpLobby;
    public static String helpLocation;
    public static String helpScore;
    public static String helpScoreboard;
    public static String scoreCongratulations;
    public static String scoreGameOver;
    public static String scoreGetTheMostGoal;
    public static String scoreGetValueGoal;
    public static String scoreNewScore;
    public static String scoreNoWinners;
    public static String scoreTeamWins;
    public static String startMostObjective;
    public static String startObjective;
    public static String startYoureAMember;
    public static String startYourePlaying;
    public static String teleportDoNotMove;
    public static String teleportYouMoved;
    public static String titleBeaconz;
    public static String titleBeaconzNews;
    public static String titleCmdLocation;
    public static String titleCmdYourePlaying;
    public static String titleLobbyInfo;
    public static String titleSubTitle;
    public static String titleSubTitleColor;
    public static String titleWelcome;
    public static String titleWelcomeBackToGame;
    public static String titleWelcomeColor;
    public static String titleWelcomeToGame;
    public static String triangleCouldNotMakeTriangle;
    public static String triangleCouldNotMakeTriangles;
    public static String triangleDroppingToLevel;
    public static String triangleEntering;
    public static String triangleLeaving;
    public static String triangleThisBelongsTo;
   
    public void loadLocale(String localeName) {
        File localeDir = new File(getBeaconzPlugin().getDataFolder() + File.separator + "locale");
        if (!localeDir.exists()) {
            localeDir.mkdirs();
        }
        File localeFile = new File(localeDir.getPath(), localeName + ".yml");
        if (localeFile.exists()) {
            //getBeaconzPlugin().getLogger().info("DEBUG: File exists!");
            locale = YamlConfiguration.loadConfiguration(localeFile);
        } else {
            // Look for defaults in the jar
            if (getBeaconzPlugin().getResource("locale/" + localeName + ".yml") != null) {
                getBeaconzPlugin().saveResource("locale/" + localeName + ".yml", true);
                localeFile = new File(getBeaconzPlugin().getDataFolder() + File.separator + "locale", localeName + ".yml");
                locale = YamlConfiguration.loadConfiguration(localeFile);
                //locale.setDefaults(defLocale);
            } else {
                // Use the default file
                localeFile = new File(getBeaconzPlugin().getDataFolder() + File.separator + "locale", "en-US.yml");
                if (localeFile.exists()) {
                    locale = YamlConfiguration.loadConfiguration(localeFile);
                } else {
                    // Look for defaults in the jar                    
                    if (getBeaconzPlugin().getResource("locale/en-US.yml") != null) {
                        getBeaconzPlugin().saveResource("locale/en-US.yml", true);
                        localeFile = new File(getBeaconzPlugin().getDataFolder() + File.separator + "locale", "en-US.yml");
                        locale = YamlConfiguration.loadConfiguration(localeFile);
                    } else {
                        getBeaconzPlugin().getLogger().severe("Could not find any locale file!");
                    }
                }
            }
        }
        // Load the defaults
        //welcome = ChatColor.translateAlternateColorCodes('&', locale.getString("welcome", "Welcome to Beaconz!"));
        actionsDistributionSettingTo = ChatColor.translateAlternateColorCodes('&', locale.getString("actions.DistributionSettingTo", "Setting beacon distribution to [value]"));
        actionsHitSign = ChatColor.translateAlternateColorCodes('&', locale.getString("actions.HitSign", "Hit sign to start game!"));
        actionsSwitchedToTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("actions.SwitchedToTeam", "Switched to [team]!"));
        actionsYouAreInTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("actions.youAreInTeam", "You are in [team]!" ));
        adminDeletedGame = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.DeletedGame", "Deleted [name]."));
        adminDeletingGame = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.DeletingGame", "Deleting game [name]... (This may take some time)"));
        adminForceEnd = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ForceEnd", "Game [name] has ended."));
        adminForceRestart = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ForceRestart", "To restart the game, use " +  "/[label] game restart <gamename>"));
        adminGamesDefined = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.GamesDefined", "The following games/regions are defined:"));
        adminGamesNoOthers = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.GamesNoOthers", "...and no others."));
        adminGamesTheLobby = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.GamesTheLobby", "The Lobby"));
        adminKickAllPlayers = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.KickAllPlayers", "All players were kicked from game [name]"));
        adminKickPlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.KickPlayer", "[player] was kicked from game [name]"));
        adminListBeaconsInGame = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ListBeaconsInGame", "Known beacons in game [name]:"));
        adminNewGameBuilding = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.NewGameBuilding", "Building a new game with given parameters. Please wait..."));
        adminParmsArgumentsPairs = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsArgumentsPairs", "Arguments must be given in pairs, separated by colons."));
        adminParmsCountdown = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsCountdown", "Countdown"));
        adminParmsDoesNotExist = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsDoesNotExist", "Parameter [name] does not exist."));
        adminParmsGoal = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsGoal", "Goal"));
        adminParmsGoalValue = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsGoalValue", "Goal Value"));
        adminParmsMode = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsMode", "Mode"));
        adminParmsScoreTypes = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsScoreTypes", "Score Types"));
        adminParmsTeams = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsTeams", "# of Teams"));
        adminParmsUnlimited = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.ParmsUnlimited", "Unlimited"));
        adminPaused = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.Paused", "Paused the game [name]. To restart, use /[label] resume <game>"));
        adminRegenComplete = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.RegenComplete", "Regenetation complete. Regenerated [number] chunks."));
        adminRegeneratingGame = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.RegeneratingGame", "Regenerating game [name]. This may take several minutes. Please wait for the 'regeneration complete' message."));
        adminReload = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.Reload", "Beaconz plugin reloaded. All existing games were preserved."));
        adminRestart = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.Restart", "Restarted game [name]"));
        adminResume = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.Resume", "Game [name] is back ON!!"));
        adminSetSpawnNeedToBeInGame = ChatColor.translateAlternateColorCodes('&', locale.getString("admin.SetSpawnNeedToBeInGame", "You need to be in the region of an active game"));
        beaconCannotBeExtended = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.CannotBeExtended", "Cannot be extended any further in this direction!"));
        beaconCannotPlaceLiquids = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.CannotPlaceLiquids", "You cannot place liquids above a beacon!"));
        beaconClaimedForTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.ClaimedForTeam", "Beacon claimed for team [team]"));
        beaconDefensePlaced = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.DefensePlaced", "Defense placed"));
        beaconDefenseRemoveTopDown = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.DefenseRemoveTopDown", "Remove blocks top-down"));
        beaconExtended = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.Extended", "You extended the beacon!"));
        beaconIsExhausted = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.IsExhausted", "beacon. is exhausted. Try again in [minutes] minute(s)"));
        beaconLinkAlreadyExists = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.LinkAlreadyExists", "Link already exists!"));
        beaconLinkBlockBroken = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.LinkBlockBroken", "Link amplifier broken! Link range decreased by [range]!"));
        beaconLinkBlockPlaced = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.LinkBlockPlaced", "Link amplifier placed! Link range increased by [range]!"));
        beaconLinkCannotCrossEnemy = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.LinkCannotCrossEnemy", "Link cannot cross enemy link!"));
        beaconLinkCouldNotBeCreated = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.LinkCouldNotBeCreated", "Link could not be created!"));
        beaconLinkCreated = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.LinkCreated", "Link created!"));
        beaconLinkLost = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.LinkLost", "The longest link was lost!"));
        beaconMapBeaconMap = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.MapBeaconMap", "Beacon Map"));
        beaconMapUnknownBeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.MapUnknownBeacon", "Unknown beacon"));
        beaconMaxLinks = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.MaxLinks", "This beacon already has [number] outbound links!"));
        beaconNameCreateATriangle = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.NameCreateATriangle", "[name] created a triangle!"));
        beaconNameCreatedALink = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.NameCreatedALink", "[name] created a link!"));
        beaconNameCreateTriangles = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.NameCreateTriangles", "[name] created [number] triangles!"));
        beaconNowHasLinks = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.NowHasLinks", "This beacon now has [number] links."));
        beaconOriginNotOwned = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.OriginNotOwned", "Origin beacon is not owned by [team]!"));
        beaconPlayerDestroyed = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.PlayerDestroyed", "[player] destroyed one of [team]'s beacons!"));
        beaconTeamDestroyed = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.TeamDestroyed", "[team1] destroyed one of [team2]'s beacons!"));
        beaconTheMapDisintegrates = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.TheMapDisintegrates", "The map disintegrates!"));
        beaconTriangleCreated = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.TriangleCreated", "Triangle created!"));
        beaconYouCannotDestroyYourOwnBeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouCannotDestroyYourOwnBeacon", "You cannot destroy your own beacon"));
        beaconYouCannotLinkToSelf = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouCannotLinkToSelf", "You cannot link a beacon to itself!"));
        beaconYouCanOnlyExtend = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouCanOnlyExtend", "You can only extend a captured beacon!"));
        beaconYouCapturedABeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouCapturedABeacon", "You captured a beacon! Mine the beacon for more beacon maps."));
        beaconYouDestroyed = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouDestroyed", "You destroyed [team] team's beacon!"));
        beaconYouHaveAMap = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouHaveAMap", "You have a beacon map! Take it to another beacon to link them up!"));
        beaconYouHaveThisMuchExp = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouHaveThisMuchExp", "You have [number] exp points."));
        beaconYouMustCapturedBeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouMustCapturedBeacon", "You must capture the beacon first!"));
        beaconYouNeedThisMuchExp = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouNeedThisMuchExp", "You need [number] exp points to link these beacons."));
        beaconYouReceivedAReward = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.YouReceivedAReward", "You received a reward!"));
        beaconClaimingBeaconAt = ChatColor.translateAlternateColorCodes('&', locale.getString("beacon.ClaimingBeaconAt", "Claiming beacon at [loction]"));
        errorAlreadyExists = ChatColor.translateAlternateColorCodes('&', locale.getString("error.AlreadyExists", "[name] already exists!"));
        errorCanOnlyPlaceBlocks = ChatColor.translateAlternateColorCodes('&', locale.getString("error.CanOnlyPlaceBlocks", "You can only place blocks on a captured beacon!"));
        errorCanOnlyPlaceBlocksUpTo = ChatColor.translateAlternateColorCodes('&', locale.getString("error.CanOnlyPlaceBlocksUpTo", "You can only place blocks up to [value] high around the beacon!"));
        errorClearAboveBeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("error.ClearAboveBeacon", "Clear blocks above before placing this block!"));
        errorClearAroundBeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("error.ClearAroundBeacon", "Clear around and above the beacon to capture!"));
        errorDistribution = ChatColor.translateAlternateColorCodes('&', locale.getString("error.Distribution", " distribution <fraction> - must be less than 1"));
        errorError = ChatColor.translateAlternateColorCodes('&', locale.getString("error.error", "Error: "));
        errorInventoryFull = ChatColor.translateAlternateColorCodes('&', locale.getString("error.InventoryFull", "You inventory is full! Dropping items!"));
        errorNoBeaconThere = ChatColor.translateAlternateColorCodes('&', locale.getString("error.NoBeaconThere", "There is no beacon there!"));
        errorNoGames = ChatColor.translateAlternateColorCodes('&', locale.getString("error.NoGames", "Could not find any games."));
        errorNoSuchGame = ChatColor.translateAlternateColorCodes('&', locale.getString("error.NoSuchGame", "No such game!"));
        errorNoSuchTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("error.NoSuchTeam", "Could not find team!"));
        errorNotEnoughExperience = ChatColor.translateAlternateColorCodes('&', locale.getString("error.NotEnoughExperience", "You do not have enough experience to do that!"));
        errorNotInGame = ChatColor.translateAlternateColorCodes('&', locale.getString("error.NotInGame", "You are not in the game '[game]'! Going to the lobby..."));
        errorNotInRegister = ChatColor.translateAlternateColorCodes('&', locale.getString("error.NotInRegister", "error.: block isBeacon() but is not in the Register: "));
        errorNotReady = ChatColor.translateAlternateColorCodes('&', locale.getString("error.notReady", "Sorry, that is not ready yet."));
        errorOnlyPlayers = ChatColor.translateAlternateColorCodes('&', locale.getString("error.OnlyPlayers", "Only players can do that!"));
        errorRegionLimit = ChatColor.translateAlternateColorCodes('&', locale.getString("error.RegionLimit", "That's the limit of the game region, you can't go any further that way."));
        errorTooFar = ChatColor.translateAlternateColorCodes('&', locale.getString("error.TooFar", "That beacon is too far away. To link over [max] blocks, use gold or diamond range extender blocks."));
        errorUnknownCommand = ChatColor.translateAlternateColorCodes('&', locale.getString("error.UnknownCommand", "Unknown command!"));
        errorUnknownPlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("error.UnknownPlayer", "Unknown or offline player!"));
        errorYouCannotBuildThere = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouCannotBuildThere", "You cannot build there!"));
        errorYouCannotDoThat = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouCannotDoThat", "You cannot do that!"));
        errorYouCannotRemoveOtherPlayersBlocks = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouCannotRemoveOtherPlayersBlocks", "You cannot remove other player's blocks!"));
        errorYouDoNotHavePermission = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouDoNotHavePermission", "You do not have permission to use this command!"));
        errorYouHaveToBeStandingOnABeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouHaveToBeStandingOnABeacon", "You have to be standing on a beacon"));
        errorYouMustBeInAGame = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouMustBeInAGame", "You must be in a game to do that!"  ));
        errorYouMustBeInATeam = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouMustBeInATeam", "You must be in a team to do that!"));
        errorYouNeedToBeLevel = ChatColor.translateAlternateColorCodes('&', locale.getString("error.YouNeedToBeLevel", "You need to be level [value] to do that!"));
        generalFailure = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Failure", "Failure!"));
        generalGame = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Game", "Game"));
        generalGames = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Games", "Games"));
        generalLevel = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Level", "Level"));
        generalLinks = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Links", "Links"));
        generalLocation = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Location", "Location"));
        generalMembers = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Members", "Members"));
        generalNone = ChatColor.translateAlternateColorCodes('&', locale.getString("general.None", "None"));
        generalSuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Success", "Success!"));
        generalTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Team", "Team"));
        generalTeams = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Teams", "Teams"));
        generalUnowned = ChatColor.translateAlternateColorCodes('&', locale.getString("general.Unowned", "Unowned"));
        helpAdminClaim = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminClaim", " - force-claims a beacon in a game"));
        helpAdminDelete = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminDelete", " - deletes the game"));
        helpAdminDistribution = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminDistribution", " - sets global beacon distribution temporarily"));
        helpAdminForceEnd = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminForceEnd", " - forces a game to end immediately"));
        helpAdminGames = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminGames", " - list existing games"));
        helpAdminJoin = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminJoin", " - join a team in an active game"));
        helpAdminKick = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminKick", "- kicks a player from the game"));
        helpAdminLink = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminLink", " - force-links a beacon you are standing on to one at x,z"));
        helpAdminList = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminList", " - lists all known beacons in the game | all games owned by team"));
        helpAdminListParms = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminListParms", " - lists game parameters"));
        helpAdminNewGame = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminNewGame", " - creates a new game in an empty region; parameters are optional - do /[label] newgame help for a list of the possible parameters"));
        helpAdminPause = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminPause", " - pauses the timer and scoreboard in a game"));
        helpAdminRegenerate = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminRegenerate", " - regenerates the game area and resets everything"));
        helpAdminReload = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminReload", " - reloads the plugin, preserving existing games"));
        helpAdminRestart = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminRestart", " - restarts the game with currently defined parameters - clears scoreboard, cleans out all beacons, restarts timer; teams aren't changed"));
        helpAdminResume = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminResume", " - resume a paused game"));
        helpAdminSetGameParms = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminSetGameParms", " - defines a game's parameters - DOES NOT restart the game (use restart for that) - do /[label] setgameparms help for a list of the possible parameters"));
        helpAdminSetLobbySpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminSetLobbySpawn", " - sets the lobby spawn point when in the lobby area"));
        helpAdminSetTeamSpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminSetTeamSpawn", " - sets the spawn point for team"));
        helpAdminSwitch = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminSwitch", " - switches your team when in a game"));
        helpAdminTeams = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminTeams", " - shows teams and team members for a game"));
        helpAdminTimerToggle = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminTimerToggle", " - toggles the scoreboard timer on and off"));
        helpAdminTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("help.AdminTitle", "beacon.z Admin Commands"));
        helpHelp = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Help", "- this help"));
        helpJoin = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Join", "- join an ongoing game"));
        helpLeave = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Leave", "- leave a game"));
        helpLine = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Line", "=================================================="));
        helpLobby = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Lobby", "- go the lobby area"));
        helpLocation = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Location", "- tells you where you are"));
        helpScore = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Score", "- show the team scores"));
        helpScoreboard = ChatColor.translateAlternateColorCodes('&', locale.getString("help.Scoreboard", "- toggles the scoreboard on and off"));
        scoreCongratulations = ChatColor.translateAlternateColorCodes('&', locale.getString("score.Congratulations", "Congratulations"));
        scoreGameOver = ChatColor.translateAlternateColorCodes('&', locale.getString("score.GameOver", "<< GAME OVER >>"));
        scoreGetTheMostGoal = ChatColor.translateAlternateColorCodes('&', locale.getString("score.GetTheMostGoal", "<< Get the most [goal]!! >>"));
        scoreGetValueGoal = ChatColor.translateAlternateColorCodes('&', locale.getString("score.GetValueGoal", "<< Get [value] [goal]!! >>"));
        scoreNewScore = ChatColor.translateAlternateColorCodes('&', locale.getString("score.NewScore", "New score"));
        scoreNoWinners = ChatColor.translateAlternateColorCodes('&', locale.getString("score.NoWinners", "There were no winners!"));
        scoreTeamWins = ChatColor.translateAlternateColorCodes('&', locale.getString("score.TeamWins", "[team] TEAM WINS!!!"));
        startMostObjective = ChatColor.translateAlternateColorCodes('&', locale.getString("start.MostObjective", "Your team's objective is to capture the most [goal]!"));
        startObjective = ChatColor.translateAlternateColorCodes('&', locale.getString("start.Objective", "Your team's objective is to capture [value] [goal]!"));
        startYoureAMember = ChatColor.translateAlternateColorCodes('&', locale.getString("start.YoureAMember", "You're a member of [name] team!"));
        startYourePlaying = ChatColor.translateAlternateColorCodes('&', locale.getString("start.YourePlaying", "You're playing game [name] in [mode] mode!"));
        teleportDoNotMove = ChatColor.translateAlternateColorCodes('&', locale.getString("teleport.DoNotMove", "Do not move, teleporting in [number] seconds!"));
        teleportYouMoved = ChatColor.translateAlternateColorCodes('&', locale.getString("teleport.YouMoved", "You moved! Cancelling teleport!"));
        titleBeaconz = ChatColor.translateAlternateColorCodes('&', locale.getString("title.Beaconz", "Beaconz"));
        titleBeaconzNews = ChatColor.translateAlternateColorCodes('&', locale.getString("title.BeaconzNews", "Beaconz News"));
        titleCmdLocation = ChatColor.translateAlternateColorCodes('&', locale.getString("title.CmdLocation", "You're in the Beaconz Lobby at"));
        titleCmdYourePlaying = ChatColor.translateAlternateColorCodes('&', locale.getString("title.CmdYourePlaying", "You're playing Beaconz game [game]"));
        titleLobbyInfo = ChatColor.translateAlternateColorCodes('&', locale.getString("title.LobbyInfo", "Welcome to Beaconz!|You are in the lobby area.|Hit a sign to start a game!|Beaconz is a team game where|you try to find, claim and link|naturally occuring beaconz in|the world. You can mine beaconz|for goodies and defend them|with blocks and traps."));
        titleSubTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("title.SubTitle", "Capture, link & defend beaconz!"));
        titleSubTitleColor = ChatColor.translateAlternateColorCodes('&', locale.getString("title.SubTitleColor", "gold"));
        titleWelcome = ChatColor.translateAlternateColorCodes('&', locale.getString("title.Welcome",  "Welcome to Beaconz!"));
        titleWelcomeBackToGame = ChatColor.translateAlternateColorCodes('&', locale.getString("title.WelcomeBackToGame", "Welcome back to Beaconz game [name]"));
        titleWelcomeColor = ChatColor.translateAlternateColorCodes('&', locale.getString("title.WelcomeColor", "gold"));
        titleWelcomeToGame = ChatColor.translateAlternateColorCodes('&', locale.getString("title.WelcomeToGame", "Welcome to Beaconz game [name]"));
        triangleCouldNotMakeTriangle = ChatColor.translateAlternateColorCodes('&', locale.getString("triangle.CouldNotMakeTriangle", "One triangle could not be created because of overlapping enemy elements!"));
        triangleCouldNotMakeTriangles = ChatColor.translateAlternateColorCodes('&', locale.getString("triangle.CouldNotMakeTriangles", "[number] triangles could not be created because of overlapping enemy elements!"));
        triangleDroppingToLevel = ChatColor.translateAlternateColorCodes('&', locale.getString("triangle.DroppingToLevel ",  ChatColor.GRAY + "[team]'s triangle area level dropping to [level]"));
        triangleEntering = ChatColor.translateAlternateColorCodes('&', locale.getString("triangle.Entering ",  ChatColor.GRAY + "Now entering [team]'s triangle area level [level]"));
        triangleLeaving = ChatColor.translateAlternateColorCodes('&', locale.getString("triangle.Leaving", "Leaving [team]'s triangle area"));
        triangleThisBelongsTo = ChatColor.translateAlternateColorCodes('&', locale.getString("triangle.ThisBelongsTo", "This belongs to [team]!"));
        // Defense text
        defenseText = new HashMap<Material,String>();
        for (String material : locale.getConfigurationSection("defenseText").getKeys(false)) {
            try {
                Material mat = Material.valueOf(material.toUpperCase());
                defenseText.put(mat,ChatColor.translateAlternateColorCodes('&', locale.getString("defenseText." + material,"")));
            } catch (Exception e) {
                getLogger().severe("No not know what defenseText." + material + " is in locale file " + localeName + ".yml, skipping...");
            }
        }
    }
}
