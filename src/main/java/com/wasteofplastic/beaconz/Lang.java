package com.wasteofplastic.beaconz;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;


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

    public static HashMap<Material, Component> defenseText;
    public static Component actionsDistributionSettingTo;
    public static Component actionsHitSign;
    public static Component actionsSwitchedToTeam;
    public static Component actionsYouAreInTeam;
    public static Component adminDeletedGame;
    public static Component adminDeletingGame;
    public static Component adminForceEnd;
    public static Component adminForceRestart;
    public static Component adminGamesDefined;
    public static Component adminGamesNoOthers;
    public static Component adminGameSignPlaced;
    public static Component adminGamesTheLobby;
    public static Component adminKickAllPlayers;
    public static Component adminKickPlayer;
    public static Component adminListBeaconsInGame;
    public static Component adminNewGameBuilding;
    public static Component adminParmsArgumentsPairs;
    public static Component adminParmsCountdown;
    public static Component adminParmsDoesNotExist;
    public static Component adminParmsGoal;
    public static Component adminParmsGoalValue;
    public static Component adminParmsMode;
    public static Component adminParmsScoreTypes;
    public static Component adminParmsTeams;
    public static Component adminParmsUnlimited;
    public static Component adminPaused;
    public static Component adminRegenComplete;
    public static Component adminRegeneratingGame;
    public static Component adminReload;
    public static Component adminRestart;
    public static Component adminResume;
    public static Component adminSetSpawnNeedToBeInGame;
    public static Component adminSignKeyword;
    public static Component adminUseSurvival;
    public static Component beaconCannotBeExtended;
    public static Component beaconCannotPlaceLiquids;
    public static Component beaconClaimedForTeam;
    public static Component beaconClaimingBeaconAt;
    public static Component beaconDefensePlaced;
    public static Component beaconDefenseRemoveTopDown;
    public static Component beaconExtended;
    public static Component beaconAmplifierBlocksCannotBeRecovered;
    public static Component beaconIsExhausted;
    public static Component beaconLinkAlreadyExists;
    public static Component beaconLinkBlockBroken;
    public static Component beaconLinkBlockPlaced;
    public static Component beaconLinkCannotCrossEnemy;
    public static Component beaconLinkCouldNotBeCreated;
    public static Component beaconLinkCreated;
    public static Component beaconLinkLost;
    public static Component beaconLocked;
    public static Component beaconLockedAlready;
    public static Component beaconLockedJustNow;
    public static Component beaconLockedWithNMoreBlocks;
    public static String beaconMapBeaconMap;
    public static String beaconMapUnknownBeacon;
    public static Component beaconMaxLinks;
    public static Component beaconNameCreateATriangle;
    public static Component beaconNameCreatedALink;
    public static Component beaconNameCreateTriangles;
    public static Component beaconNowHasLinks;
    public static Component beaconOriginNotOwned;
    public static Component beaconPlayerDestroyed;
    public static Component beaconTeamDestroyed;
    public static Component beaconTheMapDisintegrates;
    public static Component beaconTriangleCreated;
    public static Component beaconYouCannotDestroyYourOwnBeacon;
    public static Component beaconYouCannotLinkToSelf;
    public static Component beaconYouCanOnlyExtend;
    public static Component beaconYouCapturedABeacon;
    public static Component beaconYouDestroyed;
    public static Component beaconYouHaveAMap;
    public static Component beaconYouHaveThisMuchExp;
    public static Component beaconYouMustCapturedBeacon;
    public static Component beaconYouNeedThisMuchExp;
    public static Component beaconYouReceivedAReward;
    public static Component errorAlreadyExists;
    public static Component errorCanOnlyPlaceBlocks;
    public static Component errorCanOnlyPlaceBlocksUpTo;
    public static Component errorClearAboveBeacon;
    public static Component errorClearAroundBeacon;
    public static Component errorDistribution;
    public static Component errorError;
    public static Component errorInventoryFull;
    public static Component errorNoBeaconThere;
    public static Component errorNoGames;
    public static Component errorNoSuchGame;
    public static Component errorNoSuchTeam;
    public static Component errorNotEnoughExperience;
    public static Component errorNotInGame;
    public static Component errorNotInRegister;
    public static Component errorNotReady;
    public static Component errorOnlyPlayers;
    public static Component errorRegionLimit;
    public static Component errorTooFar;
    public static Component errorUnknownCommand;
    public static Component errorUnknownPlayer;
    public static Component errorYouCannotBuildThere;
    public static Component errorYouCannotDoThat;
    public static Component errorYouCannotRemoveOtherPlayersBlocks;
    public static Component errorYouDoNotHavePermission;
    public static Component errorYouHaveToBeStandingOnABeacon;
    public static Component errorYouMustBeInAGame;
    public static Component errorYouMustBeInATeam;
    public static Component errorYouNeedToBeLevel;
    public static Component generalFailure;
    public static Component generalGame;
    public static Component generalGames;
    public static Component generalLevel;
    public static Component generalLinks;
    public static String generalLocation;
    public static Component generalMembers;
    public static Component generalNone;
    public static Component generalSuccess;
    public static Component generalTeam;
    public static Component generalTeams;
    public static Component generalUnowned;
    public static Component helpAdminClaim;
    public static Component helpAdminDelete;
    public static Component helpAdminDistribution;
    public static Component helpAdminForceEnd;
    public static Component helpAdminGames;
    public static Component helpAdminJoin;
    public static Component helpAdminKick;
    public static Component helpAdminLink;
    public static Component helpAdminList;
    public static Component helpAdminListParms;
    public static Component helpAdminNewGame;
    public static Component helpAdminPause;
    public static Component helpAdminRegenerate;
    public static Component helpAdminReload;
    public static Component helpAdminRestart;
    public static Component helpAdminResume;
    public static Component helpAdminSetGameParms;
    public static Component helpAdminSetLobbySpawn;
    public static Component helpAdminSetTeamSpawn;
    public static Component helpAdminSwitch;
    public static Component helpAdminTeams;
    public static Component helpAdminTimerToggle;
    public static Component helpAdminTitle;
    public static Component helpHelp;
    public static Component helpJoin;
    public static Component helpLeave;
    public static Component helpLine;
    public static Component helpLobby;
    public static Component helpLocation;
    public static Component helpScore;
    public static Component helpScoreboard;
    public static Component scoreCongratulations;
    public static String scoreGameOver;
    public static Component scoreGetTheMostGoal;
    public static Component scoreGetValueGoal;
    public static Component scoreNewScore;
    public static Component scoreNoWinners;
    public static Component scoreScores;
    public static Component scoreTeamWins;
    public static Component startMostObjective;
    public static Component startObjective;
    public static Component startYoureAMember;
    public static Component startYourePlaying;
    public static Component teleportDoNotMove;
    public static Component teleportYouMoved;
    public static Component titleBeaconz;
    public static Component titleBeaconzNews;
    public static Component titleCmdLocation;
    public static Component titleCmdYourePlaying;
    public static Component titleLobbyInfo;
    public static Component titleSubTitle;
    public static TextColor titleSubTitleColor;
    public static Component titleWelcome;
    public static Component titleWelcomeBackToGame;
    public static TextColor titleWelcomeColor;
    public static Component titleWelcomeToGame;
    public static Component triangleCouldNotMakeTriangle;
    public static Component triangleCouldNotMakeTriangles;
    public static Component triangleDroppingToLevel;
    public static Component triangleEntering;
    public static Component triangleLeaving;
    public static Component triangleThisBelongsTo;
    public static Component triangleYourTeamLostATriangle;
    public static Component triangleTeamLostATriangle;
    public static Component errorNoLobbyYet;
    public static Component errorRequestCanceled;
    public static Component adminDeleteGameConfirm;
    public static String scoreStrategy;
    public static String scoreGameModeMiniGame;
    public static String scoreGoalArea;
    public static String scoreGoalBeacons;
    public static String scoreGoalTime;
    public static String scoreGoalTriangles;
    public static String scoreGoalLinks;
    public static Component scoreGame;
    public static Component scoreTeam;
    public static Component linkLostLink;
    public static Component linkLostLinks;
    public static Component linkTeamLostLink;
    public static Component linkTeamLostLinks;
    
   
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
        //welcome = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("welcome", "Welcome to Beaconz!"));
        actionsDistributionSettingTo =  LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("actions.DistributionSettingTo", "Setting beacon distribution to [value]"));
        actionsHitSign = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("actions.HitSign", "Hit sign to start game!"));
        actionsSwitchedToTeam = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("actions.SwitchedToTeam", "Switched to [team]!"));
        actionsYouAreInTeam = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("actions.youAreInTeam", "You are in [team]!" ));
        adminDeletedGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.DeletedGame", "Deleted [name]."));
        adminDeletingGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.DeletingGame", "Deleting game [name]..."));
        adminDeleteGameConfirm = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.DeleteGameConfirm", "Enter again to confirm within 10s."));
        adminForceEnd = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ForceEnd", "Game [name] has ended."));
        adminForceRestart = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ForceRestart", "To restart the game, use " +  "/[label] restart <gamename>"));
        adminGamesDefined = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.GamesDefined", "The following games/regions are defined:"));
        adminGamesNoOthers = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.GamesNoOthers", "...and no others."));
        adminGameSignPlaced = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.GamesSignPlaced", "Game sign placed successfully."));
        adminGamesTheLobby = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.GamesTheLobby", "The Lobby"));
        adminKickAllPlayers = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.KickAllPlayers", "All players were kicked from game [name]"));
        adminKickPlayer = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.KickPlayer", "[player] was kicked from game [name]"));
        adminListBeaconsInGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ListBeaconsInGame", "Known beacons in game [name]:"));
        adminNewGameBuilding = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.NewGameBuilding", "Building a new game with given parameters. Please wait..."));
        adminParmsArgumentsPairs = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsArgumentsPairs", "Arguments must be given in pairs, separated by colons."));
        adminParmsCountdown = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsCountdown", "Countdown"));
        adminParmsDoesNotExist = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsDoesNotExist", "Parameter [name] does not exist."));
        adminParmsGoal = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsGoal", "Goal"));
        adminParmsGoalValue = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsGoalValue", "Goal Value"));
        adminParmsMode = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsMode", "Mode"));
        adminParmsScoreTypes = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsScoreTypes", "Score Types"));
        adminParmsTeams = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsTeams", "# of Factions"));
        adminParmsUnlimited = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.ParmsUnlimited", "Unlimited"));
        adminPaused = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.Paused", "Paused the game [name]. To restart, use /[label] resume <game>"));
        adminRegenComplete = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.RegenComplete", "Regenetation complete."));
        adminRegeneratingGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.RegeneratingGame", "Regenerating game [name]."));
        adminReload = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.Reload", "Beaconz plugin reloaded. All existing games were preserved."));
        adminRestart = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.Restart", "Restarted game [name]"));
        adminResume = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.Resume", "Game [name] is back ON!!"));
        adminSetSpawnNeedToBeInGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.SetSpawnNeedToBeInGame", "You need to be in the region of an active game"));
        adminSignKeyword = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.SignKeyword", "[beaconz]"));
        adminUseSurvival = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("admin.UseSurvival", "Use Survival mode to break signs in lobby."));
        beaconAmplifierBlocksCannotBeRecovered = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.AmplifierBlocksCannotBeRecovered", "Link amplifier blocks cannot be recovered!"));
        beaconCannotBeExtended = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.CannotBeExtended", "Cannot be extended any further in this direction!"));
        beaconCannotPlaceLiquids = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.CannotPlaceLiquids", "You cannot place liquids above a beacon!"));
        beaconClaimedForTeam = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.ClaimedForTeam", "Beacon claimed for [team] faction"));
        beaconDefensePlaced = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.DefensePlaced", "Defense placed"));
        beaconDefenseRemoveTopDown = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.DefenseRemoveTopDown", "Remove blocks top-down"));
        beaconExtended = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.Extended", "You extended the beacon!"));
        beaconIsExhausted = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.IsExhausted", "beacon. is exhausted. Try again in [minutes] minute(s)"));
        beaconLinkAlreadyExists = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LinkAlreadyExists", "Link already exists!"));
        beaconLinkBlockBroken = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LinkBlockBroken", "Link amplifier broken! Link range decreased by [range]!"));
        beaconLinkBlockPlaced = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LinkBlockPlaced", "Link amplifier placed! Link range increased by [range]!"));
        beaconLinkCannotCrossEnemy = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LinkCannotCrossEnemy", "Link cannot cross enemy link!"));
        beaconLinkCouldNotBeCreated = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LinkCouldNotBeCreated", "Link could not be created!"));
        beaconLinkCreated = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LinkCreated", "Link created!"));
        beaconLinkLost = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LinkLost", "The longest link was lost!"));
        beaconLocked = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.Locked", "This beacon is locked!"));
        beaconLockedAlready = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LockedAlready", "This beacon is already locked. Don't waste [lockingBlock]s!"));
        beaconLockedJustNow = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LockedJustNow", "This beacon is now locked. Break an [lockingBlock] to unlock it!"));
        beaconLockedWithNMoreBlocks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.LockedWithNMoreBlocks", "[number] additional locking block(s) on this level will lock the beacon."));        
        beaconMapBeaconMap = locale.getString("beacon.MapBeaconMap", "Beacon Map");
        beaconMapUnknownBeacon = locale.getString("beacon.MapUnknownBeacon", "Unknown beacon");
        beaconMaxLinks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.MaxLinks", "This beacon already has [number] outbound links!"));
        beaconNameCreateATriangle = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.NameCreateATriangle", "[name] created a triangle!"));
        beaconNameCreatedALink = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.NameCreatedALink", "[name] created a link!"));
        beaconNameCreateTriangles = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.NameCreateTriangles", "[name] created [number] triangles!"));
        beaconNowHasLinks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.NowHasLinks", "This beacon now has [number] links."));
        beaconOriginNotOwned = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.OriginNotOwned", "Origin beacon is not owned by [team]!"));
        beaconPlayerDestroyed = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.PlayerDestroyed", "[player] destroyed one of [team]'s beacons!"));
        beaconTeamDestroyed = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.TeamDestroyed", "[team1] destroyed one of [team2]'s beacons!"));
        beaconTheMapDisintegrates = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.TheMapDisintegrates", "The map disintegrates!"));
        beaconTriangleCreated = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.TriangleCreated", "Triangle created!"));
        beaconYouCannotDestroyYourOwnBeacon = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouCannotDestroyYourOwnBeacon", "You cannot destroy your own beacon"));
        beaconYouCannotLinkToSelf = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouCannotLinkToSelf", "You cannot link a beacon to itself!"));
        beaconYouCanOnlyExtend = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouCanOnlyExtend", "You can only extend a captured beacon!"));
        beaconYouCapturedABeacon = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouCapturedABeacon", "You captured a beacon! Mine the beacon for more beacon maps."));
        beaconYouDestroyed = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouDestroyed", "You destroyed [team] faction's beacon!"));
        beaconYouHaveAMap = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouHaveAMap", "You have a beacon map! Take it to another beacon to link them up!"));
        beaconYouHaveThisMuchExp = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouHaveThisMuchExp", "You have [number] exp points."));
        beaconYouMustCapturedBeacon = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouMustCapturedBeacon", "You must capture the beacon first!"));
        beaconYouNeedThisMuchExp = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouNeedThisMuchExp", "You need [number] exp points to link these beacons."));
        beaconYouReceivedAReward = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.YouReceivedAReward", "You received a reward!"));
        beaconClaimingBeaconAt = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("beacon.ClaimingBeaconAt", "Claiming beacon at [loction]"));
        errorAlreadyExists = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.AlreadyExists", "[name] already exists!"));
        errorCanOnlyPlaceBlocks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.CanOnlyPlaceBlocks", "You can only place blocks on a captured beacon!"));
        errorCanOnlyPlaceBlocksUpTo = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.CanOnlyPlaceBlocksUpTo", "You can only place blocks up to [value] high around the beacon!"));
        errorClearAboveBeacon = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.ClearAboveBeacon", "Clear blocks above before placing this block!"));
        errorClearAroundBeacon = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.ClearAroundBeacon", "Clear around and above the beacon to capture!"));
        errorDistribution = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.Distribution", " distribution <fraction> - must be less than 1"));
        errorError = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.error", "Error: "));
        errorInventoryFull = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.InventoryFull", "You inventory is full! Dropping items!"));
        errorNoBeaconThere = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NoBeaconThere", "There is no beacon there!"));
        errorNoGames = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NoGames", "Could not find any games."));
        errorNoLobbyYet = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NoLobbyYet", "Hmm, there is no lobby yet..."));
        errorNoSuchGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NoSuchGame", "No such game!"));
        errorNoSuchTeam = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NoSuchTeam", "Could not find faction!"));
        errorNotEnoughExperience = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NotEnoughExperience", "You do not have enough experience to do that!"));
        errorNotInGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NotInGame", "You are not in the game '[game]'! Going to the lobby..."));
        errorNotInRegister = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.NotInRegister", "error.: block isBeacon() but is not in the Register: "));
        errorNotReady = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.notReady", "Sorry, that is not ready yet."));
        errorOnlyPlayers = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.OnlyPlayers", "Only players can do that!"));
        errorRegionLimit = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.RegionLimit", "That's the limit of the game region, you can't go any further that way."));
        errorRequestCanceled = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.RequestCanceled", "Request canceled."));
        errorTooFar = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.TooFar", "That beacon is too far away. To link over [max] blocks, use gold or diamond range extender blocks."));
        errorUnknownCommand = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.UnknownCommand", "Unknown command!"));
        errorUnknownPlayer = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.UnknownPlayer", "Unknown or offline player!"));
        errorYouCannotBuildThere = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouCannotBuildThere", "You cannot build there!"));
        errorYouCannotDoThat = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouCannotDoThat", "You cannot do that!"));
        errorYouCannotRemoveOtherPlayersBlocks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouCannotRemoveOtherPlayersBlocks", "You cannot remove other player's blocks!"));
        errorYouDoNotHavePermission = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouDoNotHavePermission", "You do not have permission to use this command!"));
        errorYouHaveToBeStandingOnABeacon = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouHaveToBeStandingOnABeacon", "You have to be standing on a beacon"));
        errorYouMustBeInAGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouMustBeInAGame", "You must be in a game to do that!"  ));
        errorYouMustBeInATeam = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouMustBeInATeam", "You must be in a faction to do that!"));
        errorYouNeedToBeLevel = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("error.YouNeedToBeLevel", "You need to be level [value] to do that!"));
        generalFailure = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Failure", "Failure!"));
        generalGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Game", "Game"));
        generalGames = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Games", "Games"));
        generalLevel = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Level", "Level"));
        generalLinks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Links", "Links"));
        generalLocation = locale.getString("general.Location", "Location");
        generalMembers = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Members", "Members"));
        generalNone = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.None", "None"));
        generalSuccess = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Success", "Success!"));
        generalTeam = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Team", "Faction"));
        generalTeams = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Teams", "Factions"));
        generalUnowned = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("general.Unowned", "Unowned"));
        helpAdminClaim = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminClaim", " - force-claims a beacon in a game"));
        helpAdminDelete = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminDelete", " - deletes the game and regenerates chunks"));
        helpAdminDistribution = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminDistribution", " - sets global beacon distribution temporarily"));
        helpAdminForceEnd = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminForceEnd", " - forces a game to end immediately"));
        helpAdminGames = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminGames", " - list existing games"));
        helpAdminJoin = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminJoin", " - join a faction in an active game"));
        helpAdminKick = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminKick", "- kicks a player from the game"));
        helpAdminLink = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminLink", " - force-links a beacon you are standing on to one at x,z"));
        helpAdminList = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminList", " - lists all known beacons in the game | all games owned by faction"));
        helpAdminListParms = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminListParms", " - lists game parameters"));
        helpAdminNewGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminNewGame", " - creates a new game in an empty region; parameters are optional - do /[label] newgame help for a list of the possible parameters"));
        helpAdminPause = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminPause", " - pauses the timer and scoreboard in a game"));
        helpAdminRegenerate = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminRegenerate", " - regenerates game area chunks and resets game"));
        helpAdminReload = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminReload", " - reloads the plugin, preserving existing games"));
        helpAdminRestart = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminRestart", " - restarts the game with currently defined parameters - clears scoreboard, cleans out all beacons, restarts timer; factions aren't changed"));
        helpAdminResume = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminResume", " - resume a paused game"));
        helpAdminSetGameParms = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminSetGameParms", " - defines a game's parameters - DOES NOT restart the game (use restart for that) - do /[label] setgameparms help for a list of the possible parameters"));
        helpAdminSetLobbySpawn = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminSetLobbySpawn", " - sets the lobby spawn point when in the lobby area"));
        helpAdminSetTeamSpawn = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminSetTeamSpawn", " - sets the spawn point for faction"));
        helpAdminSwitch = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminSwitch", " - switches faction when in a game"));
        helpAdminTeams = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminTeams", " - shows factions and faction members for a game"));
        helpAdminTimerToggle = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminTimerToggle", " - toggles the scoreboard timer on and off"));
        helpAdminTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.AdminTitle", "beacon.z Admin Commands"));
        helpHelp = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Help", "- this help"));
        helpJoin = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Join", "- join an ongoing game"));
        helpLeave = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Leave", "- leave a game"));
        helpLine = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Line", "=================================================="));
        helpLobby = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Lobby", "- go the lobby area"));
        helpLocation = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Location", "- tells you where you are"));
        helpScore = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Score", "- show the faction scores"));
        helpScoreboard = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("help.Scoreboard", "- toggles the scoreboard on and off"));
        scoreCongratulations = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.Congratulations", "Congratulations"));
        scoreGameOver = locale.getString("score.GameOver", "<< GAME OVER >>");
        scoreGetTheMostGoal = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.GetTheMostGoal", "<< Get the most [goal]!! >>"));
        scoreGetValueGoal = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.GetValueGoal", "<< Get [value] [goal]!! >>"));
        scoreGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.game", "[score] [unit]"));
        scoreGameModeMiniGame = locale.getString("score.gamemode.minigame", "Minigame");
        scoreGoalArea = locale.getString("score.goals.area", "Area");
        scoreGoalBeacons = locale.getString("score.goals.beacons", "Beacons");
        scoreGoalTime = locale.getString("score.goals.time", "Time");
        scoreGoalTriangles = locale.getString("score.goals.triangles", "Triangles");
        scoreGoalLinks = locale.getString("score.goals.links", "Links");
        scoreNewScore = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.NewScore", "New score"));
        scoreNoWinners = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.NoWinners", "There were no winners!"));
        scoreScores = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.Scores", "Scores:"));
        scoreTeam = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.team", "[team]"));
        scoreStrategy = locale.getString("score.gamemode.strategy", "Strategy");
        scoreTeamWins = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("score.TeamWins", "[team] FACTION WINS!!!"));
        startMostObjective = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("start.MostObjective", "Your faction's objective is to capture the most [goal]!"));
        startObjective = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("start.Objective", "Your faction's objective is to capture [value] [goal]!"));
        startYoureAMember = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("start.YoureAMember", "You're a member of [name] faction!"));
        startYourePlaying = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("start.YourePlaying", "You're playing game [name] in [mode] mode!"));
        teleportDoNotMove = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("teleport.DoNotMove", "Do not move, teleporting in [number] seconds!"));
        teleportYouMoved = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("teleport.YouMoved", "You moved! Cancelling teleport!"));
        titleBeaconz = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.Beaconz", "Beaconz"));
        titleBeaconzNews = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.BeaconzNews", "Beaconz News"));
        titleCmdLocation = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.CmdLocation", "You're in the Beaconz Lobby at"));
        titleCmdYourePlaying = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.CmdYourePlaying", "You're playing Beaconz game [game]"));
        titleLobbyInfo = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.LobbyInfo", "Welcome to Beaconz!|You are in the lobby area.|Hit a sign to start a game!|Beaconz is a faction game where|you try to find, claim and link|naturally occuring beaconz in|the world. You can mine beaconz|for goodies and defend them|with blocks and traps."));
        titleSubTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.SubTitle", "Capture, link & defend beaconz!"));
        titleSubTitleColor = getTextColor(locale.getString("title.SubTitleColor", "gold"));
        titleWelcome = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.Welcome",  "Welcome to Beaconz!"));
        titleWelcomeBackToGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.WelcomeBackToGame", "Welcome back to Beaconz game [name]"));
        titleWelcomeColor = getTextColor(locale.getString("title.WelcomeColor", "gold"));
        titleWelcomeToGame = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("title.WelcomeToGame", "Welcome to Beaconz game [name]"));
        triangleCouldNotMakeTriangle = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.CouldNotMakeTriangle", "One triangle could not be created because of overlapping enemy elements!"));
        triangleCouldNotMakeTriangles = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.CouldNotMakeTriangles", "[number] triangles could not be created because of overlapping enemy elements!"));
        triangleDroppingToLevel = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.DroppingToLevel ",  "[team]'s triangle area level dropping to [level]"));
        triangleEntering = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.Entering ", "Now entering [team]'s triangle area level [level]"));
        triangleLeaving = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.Leaving", "Leaving [team]'s triangle area"));
        triangleThisBelongsTo = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.ThisBelongsTo", "This belongs to [team]!"));
        triangleYourTeamLostATriangle = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.YourTeamLostATriangle", "Your faction lost a triangle!"));
        triangleTeamLostATriangle = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("triangle.TeamLostATriangle", "[team] lost a triangle!"));
        linkLostLink = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("link.LostLink", "Your team lost a link!"));
        linkLostLinks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("link.LostLinks", "Your team lost [number] links!"));
        linkTeamLostLink = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("link.TeamLostLink", "[team] lost a link!"));
        linkTeamLostLinks = LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("link.TeamLostLinks", "[team] lost [number links!"));

        // Defense text
        defenseText = new HashMap<>();
        for (String material : locale.getConfigurationSection("defenseText").getKeys(false)) {
            try {
                Material mat = Material.valueOf(material.toUpperCase());
                defenseText.put(mat,LegacyComponentSerializer.legacyAmpersand().deserialize(locale.getString("defenseText." + material,"")));
            } catch (Exception e) {
                getLogger().severe("No not know what defenseText." + material + " is in locale file " + localeName + ".yml, skipping...");
            }
        }
    }


    private TextColor getTextColor(@Nullable String colorString) {
        // Try to parse it directly into a TextColor
        // fromHexString handles "#RRGGBB"
        // NamedTextColor.NAMES.value() handles names like "gold", "red", etc.
        TextColor color = TextColor.fromHexString(colorString);

        if (color == null) {
            color = NamedTextColor.NAMES.value(colorString.toLowerCase());
        }

        // Use a fallback if the config value was invalid
        if (color == null) {
            color = NamedTextColor.GOLD;
        }
        
        return color;
    }
}
