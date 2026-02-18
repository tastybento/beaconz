package com.wasteofplastic.beaconz.config;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;


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
    public static String actionsDistributionSettingTo;
    public static Component actionsHitSign;
    public static String actionsSwitchedToTeam;
    public static String actionsYouAreInTeam;
    public static String adminDeletedGame;
    public static String adminDeletingGame;
    public static String adminForceEnd;
    public static String adminForceRestart;
    public static Component adminGamesDefined;
    public static Component adminGamesNoOthers;
    public static Component adminGameSignPlaced;
    public static Component adminGamesTheLobby;
    public static String adminKickAllPlayers;
    public static String adminKickPlayer;
    public static String adminListBeaconsInGame;
    public static Component adminNewGameBuilding;
    public static Component adminParmsArgumentsPairs;
    public static Component adminParmsCountdown;
    public static String adminParmsDoesNotExist;
    public static Component adminParmsGoal;
    public static Component adminParmsGoalValue;
    public static Component adminParmsMode;
    public static Component adminParmsScoreTypes;
    public static Component adminParmsTeams;
    public static Component adminParmsUnlimited;
    public static String adminPaused;
    public static Component adminRegenComplete;
    public static String adminRegeneratingGame;
    public static Component adminReload;
    public static String adminRestart;
    public static String adminResume;
    public static Component adminSetSpawnNeedToBeInGame;
    public static Component adminSignKeyword;
    public static Component adminUseSurvival;
    public static Component beaconCannotBeExtended;
    public static Component beaconCannotPlaceLiquids;
    public static String beaconClaimedForTeam;
    public static String beaconClaimingBeaconAt;
    public static Component beaconDefensePlaced;
    public static Component beaconDefenseRemoveTopDown;
    public static Component beaconExtended;
    public static Component beaconAmplifierBlocksCannotBeRecovered;
    public static String beaconIsExhausted;
    public static Component beaconLinkAlreadyExists;
    public static String beaconLinkBlockBroken;
    public static String beaconLinkBlockPlaced;
    public static Component beaconLinkCannotCrossEnemy;
    public static Component beaconLinkCouldNotBeCreated;
    public static Component beaconLinkCreated;
    public static Component beaconLinkLost;
    public static Component beaconLocked;
    public static String beaconLockedAlready;
    public static String beaconLockedJustNow;
    public static String beaconLockedWithNMoreBlocks;
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
    public static Component beaconTheMapDisintegrates;
    public static Component beaconTriangleCreated;
    public static Component beaconYouCannotDestroyYourOwnBeacon;
    public static Component beaconYouCannotLinkToSelf;
    public static Component beaconYouCanOnlyExtend;
    public static Component beaconYouCapturedABeacon;
    public static String beaconYouDestroyed;
    public static Component beaconYouHaveAMap;
    public static String beaconYouHaveThisMuchExp;
    public static Component beaconYouMustCapturedBeacon;
    public static String beaconYouNeedThisMuchExp;
    public static Component beaconYouReceivedAReward;
    public static String errorAlreadyExists;
    public static Component errorCanOnlyPlaceBlocks;
    public static String errorCanOnlyPlaceBlocksUpTo;
    public static Component errorClearAboveBeacon;
    public static Component errorClearAroundBeacon;
    public static String errorDistribution;
    public static Component errorError;
    public static Component errorInventoryFull;
    public static Component errorNoBeaconThere;
    public static Component errorNoGames;
    public static Component errorNoSuchGame;
    public static Component errorNoSuchTeam;
    public static Component errorNoTeams;
    public static Component errorNotEnoughExperience;
    public static String errorNotInGame;
    public static Component errorNotInRegister;
    public static Component errorNotReady;
    public static Component errorOnlyPlayers;
    public static Component errorRegionLimit;
    public static String errorTooFar;
    public static Component errorUnknownCommand;
    public static Component errorUnknownPlayer;
    public static Component errorYouCannotBuildThere;
    public static Component errorYouCannotDoThat;
    public static Component errorYouCannotRemoveOtherPlayersBlocks;
    public static Component errorYouDoNotHavePermission;
    public static Component errorYouHaveToBeStandingOnABeacon;
    public static Component errorYouMustBeInAGame;
    public static Component errorYouMustBeInATeam;
    public static String errorYouNeedToBeLevel;
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
    public static String helpAdminNewGame;
    public static Component helpAdminPause;
    public static Component helpAdminRegenerate;
    public static Component helpAdminReload;
    public static Component helpAdminRestart;
    public static Component helpAdminResume;
    public static String helpAdminSetGameParms;
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
    public static String scoreGetTheMostGoal;
    public static String scoreGetValueGoal;
    public static Component scoreNewScore;
    public static Component scoreNoWinners;
    public static Component scoreScores;
    public static String scoreTeamWins;
    // Score GUI entries
    public static String scoreGuiTitle;
    public static String scoreGuiTeamHeader;
    public static String scoreGuiTeamPlayers;
    public static String scoreGuiScoreName;
    public static String scoreGuiScoreTeam;
    public static String scoreGuiScoreValue;
    public static String startMostObjective;
    public static String startObjective;
    public static String startYoureAMember;
    public static String startYourePlaying;
    public static String teleportDoNotMove;
    public static Component teleportYouMoved;
    public static Component titleBeaconz;
    public static Component titleBeaconzNews;
    public static Component titleCmdLocation;
    public static String titleCmdYourePlaying;
    public static String titleLobbyInfo;
    public static Component titleSubTitle;
    public static TextColor titleSubTitleColor;
    public static Component titleWelcome;
    public static String titleWelcomeBackToGame;
    public static TextColor titleWelcomeColor;
    public static String titleWelcomeToGame;
    public static Component triangleCouldNotMakeTriangle;
    public static String triangleCouldNotMakeTriangles;
    public static String triangleDroppingToLevel;
    public static String triangleEntering;
    public static String triangleLeaving;
    public static String triangleThisBelongsTo;
    public static Component triangleYourTeamLostATriangle;
    public static String triangleTeamLostATriangle;
    public static Component errorNoLobbyYet;
    public static Component errorRequestCanceled;
    public static String adminDeleteGameConfirm;
    public static String scoreStrategy;
    public static String scoreGameModeMiniGame;
    public static String scoreGoalArea;
    public static String scoreGoalBeacons;
    public static String scoreGoalTime;
    public static String scoreGoalTriangles;
    public static String scoreGoalLinks;
    public static String scoreGame;
    public static String scoreTeam;
    public static Component linkLostLink;
    public static String linkLostLinks;
    public static String linkTeamLostLink;
    public static String linkTeamLostLinks;
    public static Component beaconBreakToOwn;


    public void loadLocale(String localeName) {
        File localeDir = new File(getBeaconzPlugin().getDataFolder() + File.separator + "locale");
        if (!localeDir.exists()) {
            if (!localeDir.mkdirs()) {
                getBeaconzPlugin().getLogger().severe("Could not create locale directory!");
                return;
            }
        }
        File localeFile = new File(localeDir.getPath(), localeName + ".yml");
        if (localeFile.exists()) {
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
        //welcome = MiniMessage.miniMessage().deserialize(locale.getString("welcome", "Welcome to Beaconz!"));
        actionsDistributionSettingTo =  locale.getString("actions.DistributionSettingTo", "<green>Setting beacon distribution to <yellow><value></yellow></green>");
        actionsHitSign = MiniMessage.miniMessage().deserialize(locale.getString("actions.HitSign", "<aqua>Hit sign to start game!</aqua>"));
        actionsSwitchedToTeam = locale.getString("actions.SwitchedToTeam", "<green><player> switched to <yellow><team></yellow>!</green>");
        actionsYouAreInTeam = locale.getString("actions.youAreInTeam", "<aqua>You are in <yellow><team></yellow>!</aqua>");
        adminDeletedGame = locale.getString("admin.DeletedGame", "<green>Deleted <yellow><name></yellow>.</green>");
        adminDeletingGame = locale.getString("admin.DeletingGame", "<gold>Deleting game <yellow><name></yellow>...</gold>");
        adminDeleteGameConfirm = locale.getString("admin.DeleteGameConfirm", "<gold><bold>Enter again to confirm within 10s.</bold></gold>");
        adminForceEnd = locale.getString("admin.ForceEnd", "<green>Game <yellow><name></yellow> has ended.</green>");
        adminForceRestart = locale.getString("admin.ForceRestart", "<aqua>To restart the game, use <yellow>/<label> restart <gamename></yellow></aqua>");
        adminGamesDefined = MiniMessage.miniMessage().deserialize(locale.getString("admin.GamesDefined", "<aqua>The following games/regions are defined:</aqua>"));
        adminGamesNoOthers = MiniMessage.miniMessage().deserialize(locale.getString("admin.GamesNoOthers", "<gray>...and no others.</gray>"));
        adminGameSignPlaced = MiniMessage.miniMessage().deserialize(locale.getString("admin.GamesSignPlaced", "<green>Game sign placed successfully.</green>"));
        adminGamesTheLobby = MiniMessage.miniMessage().deserialize(locale.getString("admin.GamesTheLobby", "<gold>The Lobby</gold>"));
        adminKickAllPlayers = locale.getString("admin.KickAllPlayers", "<gold>All players were kicked from game <yellow><name></yellow></gold>");
        adminKickPlayer = locale.getString("admin.KickPlayer", "<gold><yellow><player></yellow> was kicked from game <yellow><name></yellow></gold>");
        adminListBeaconsInGame = locale.getString("admin.ListBeaconsInGame", "<aqua>Known beacons in game <yellow><name></yellow>:</aqua>");
        adminNewGameBuilding = MiniMessage.miniMessage().deserialize(locale.getString("admin.NewGameBuilding", "<gold>Building a new game with given parameters. Please wait...</gold>"));
        adminParmsArgumentsPairs = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsArgumentsPairs", "<red>Arguments must be given in pairs, separated by colons.</red>"));
        adminParmsCountdown = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsCountdown", "<aqua>Countdown</aqua>"));
        adminParmsDoesNotExist = locale.getString("admin.ParmsDoesNotExist", "<red>Parameter <yellow><name></yellow> does not exist.</red>");
        adminParmsGoal = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsGoal", "<aqua>Goal</aqua>"));
        adminParmsGoalValue = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsGoalValue", "<aqua>Goal Value</aqua>"));
        adminParmsMode = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsMode", "<aqua>Mode</aqua>"));
        adminParmsScoreTypes = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsScoreTypes", "<aqua>Score Types</aqua>"));
        adminParmsTeams = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsTeams", "<aqua># of Factions</aqua>"));
        adminParmsUnlimited = MiniMessage.miniMessage().deserialize(locale.getString("admin.ParmsUnlimited", "<gray>Unlimited</gray>"));
        adminPaused = locale.getString("admin.Paused", "<gold>Paused the game <yellow><name></yellow>. To restart, use <yellow>/<label> resume <game></yellow></gold>");
        adminRegenComplete = MiniMessage.miniMessage().deserialize(locale.getString("admin.RegenComplete", "<green>Regeneration complete.</green>"));
        adminRegeneratingGame = locale.getString("admin.RegeneratingGame", "<gold>Regenerating game <yellow><name></yellow>...</gold>");
        adminReload = MiniMessage.miniMessage().deserialize(locale.getString("admin.Reload", "<green>Beaconz plugin reloaded. All existing games were preserved.</green>"));
        adminRestart = locale.getString("admin.Restart", "<green>Restarted game <yellow><name></yellow></green>");
        adminResume = locale.getString("admin.Resume", "<green>Game <yellow><name></yellow> is back ON!!</green>");
        adminSetSpawnNeedToBeInGame = MiniMessage.miniMessage().deserialize(locale.getString("admin.SetSpawnNeedToBeInGame", "<red>You need to be in the region of an active game</red>"));
        adminSignKeyword = MiniMessage.miniMessage().deserialize(locale.getString("admin.SignKeyword", "[beaconz]"));
        adminUseSurvival = MiniMessage.miniMessage().deserialize(locale.getString("admin.UseSurvival", "<gold>Use Survival mode to break signs in lobby.</gold>"));
        beaconAmplifierBlocksCannotBeRecovered = MiniMessage.miniMessage().deserialize(locale.getString("beacon.AmplifierBlocksCannotBeRecovered", "<gold>Link amplifier blocks cannot be recovered!</gold>"));
        beaconBreakToOwn = MiniMessage.miniMessage().deserialize(locale.getString("beacon.BreakToOwn", "<aqua>Break the obsidian to own the beacon!</aqua>"));
        beaconCannotBeExtended = MiniMessage.miniMessage().deserialize(locale.getString("beacon.CannotBeExtended", "<red>Cannot be extended any further in this direction!</red>"));
        beaconCannotPlaceLiquids = MiniMessage.miniMessage().deserialize(locale.getString("beacon.CannotPlaceLiquids", "<red>You cannot place liquids above a beacon!</red>"));
        beaconClaimedForTeam = locale.getString("beacon.ClaimedForTeam", "<green>Beacon claimed for <yellow><team></yellow> faction!</green>");
        beaconDefensePlaced = MiniMessage.miniMessage().deserialize(locale.getString("beacon.DefensePlaced", "<green>Defense placed</green>"));
        beaconDefenseRemoveTopDown = MiniMessage.miniMessage().deserialize(locale.getString("beacon.DefenseRemoveTopDown", "<gold>Remove blocks top-down</gold>"));
        beaconExtended = MiniMessage.miniMessage().deserialize(locale.getString("beacon.Extended", "<green>You extended the beacon!</green>"));
        beaconIsExhausted = locale.getString("beacon.IsExhausted", "<gold>Beacon is exhausted! Try again in <yellow><minutes></yellow> minute(s)</gold>");
        beaconLinkAlreadyExists = MiniMessage.miniMessage().deserialize(locale.getString("beacon.LinkAlreadyExists", "<gold>Link already exists!</gold>"));
        beaconLinkBlockBroken = locale.getString("beacon.LinkBlockBroken", "<red>Link amplifier broken! Link range decreased by <yellow><range></yellow>!</red>");
        beaconLinkBlockPlaced = locale.getString("beacon.LinkBlockPlaced", "<green>Link amplifier placed! Link range increased by <yellow><range></yellow>!</green>");
        beaconLinkCannotCrossEnemy = MiniMessage.miniMessage().deserialize(locale.getString("beacon.LinkCannotCrossEnemy", "<red>Link cannot cross enemy link!</red>"));
        beaconLinkCouldNotBeCreated = MiniMessage.miniMessage().deserialize(locale.getString("beacon.LinkCouldNotBeCreated", "<red>Link could not be created!</red>"));
        beaconLinkCreated = MiniMessage.miniMessage().deserialize(locale.getString("beacon.LinkCreated", "<green>Link created!</green>"));
        beaconLinkLost = MiniMessage.miniMessage().deserialize(locale.getString("beacon.LinkLost", "<red>The longest link was lost!</red>"));
        beaconLocked = MiniMessage.miniMessage().deserialize(locale.getString("beacon.Locked", "<gold>This beacon is locked!</gold>"));
        beaconLockedAlready = locale.getString("beacon.LockedAlready", "<gold>This beacon is already locked. Don't waste <yellow><lockingblock></yellow>'s!</gold>");
        beaconLockedJustNow = locale.getString("beacon.LockedJustNow", "<green>This beacon is now locked. Break an <yellow><lockingblock></yellow> to unlock it!</green>");
        beaconLockedWithNMoreBlocks = locale.getString("beacon.LockedWithNMoreBlocks", "<aqua><yellow><number></yellow> additional locking block(s) on this level will lock the beacon.</aqua>");
        beaconMapBeaconMap = locale.getString("beacon.MapBeaconMap", "Beacon Map"); // Not colored because it's used as the map name in the item lore
        beaconMapUnknownBeacon = locale.getString("beacon.MapUnknownBeacon", "<gray>Unknown beacon</gray>");
        beaconMaxLinks = locale.getString("beacon.MaxLinks", "<gold>This beacon already has <yellow><number></yellow> outbound links!</gold>");
        beaconNameCreateATriangle = locale.getString("beacon.NameCreateATriangle", "<green><yellow><name></yellow> created a triangle!</green>");
        beaconNameCreatedALink = locale.getString("beacon.NameCreatedALink", "<green><yellow><name></yellow> created a link!</green>");
        beaconNameCreateTriangles = locale.getString("beacon.NameCreateTriangles", "<green><yellow><name></yellow> created <yellow><number></yellow> triangles!</green>");
        beaconNowHasLinks = locale.getString("beacon.NowHasLinks", "<aqua>This beacon now has <yellow><number></yellow> links.</aqua>");
        beaconOriginNotOwned = locale.getString("beacon.OriginNotOwned", "<red>Origin beacon is not owned by <yellow><team></yellow>!</red>");
        beaconPlayerDestroyed = locale.getString("beacon.PlayerDestroyed", "<red><yellow><player></yellow> destroyed one of <yellow><team></yellow>'s beacons!</red>");
        beaconTeamDestroyed = locale.getString("beacon.TeamDestroyed", "<red><yellow><team1></yellow> destroyed one of <yellow><team2></yellow>'s beacons!</red>");
        beaconTheMapDisintegrates = MiniMessage.miniMessage().deserialize(locale.getString("beacon.TheMapDisintegrates", "<red><italic>The map disintegrates!</italic></red>"));
        beaconTriangleCreated = MiniMessage.miniMessage().deserialize(locale.getString("beacon.TriangleCreated", "<green>Triangle created!</green>"));
        beaconYouCannotDestroyYourOwnBeacon = MiniMessage.miniMessage().deserialize(locale.getString("beacon.YouCannotDestroyYourOwnBeacon", "<red>You cannot destroy your own beacon</red>"));
        beaconYouCannotLinkToSelf = MiniMessage.miniMessage().deserialize(locale.getString("beacon.YouCannotLinkToSelf", "<red>You cannot link a beacon to itself!</red>"));
        beaconYouCanOnlyExtend = MiniMessage.miniMessage().deserialize(locale.getString("beacon.YouCanOnlyExtend", "<red>You can only extend a captured beacon!</red>"));
        beaconYouCapturedABeacon = MiniMessage.miniMessage().deserialize(locale.getString("beacon.YouCapturedABeacon", "<green>You captured a beacon! Mine the beacon for more beacon maps.</green>"));
        beaconYouDestroyed = locale.getString("beacon.YouDestroyed", "<green>You destroyed <yellow><team></yellow> faction's beacon!</green>");
        beaconYouHaveAMap = MiniMessage.miniMessage().deserialize(locale.getString("beacon.YouHaveAMap", "<aqua>You have a beacon map! Take it to another beacon to link them up!</aqua>"));
        beaconYouHaveThisMuchExp = locale.getString("beacon.YouHaveThisMuchExp", "<aqua>You have <yellow><number></yellow> exp points.</aqua>");
        beaconYouMustCapturedBeacon = MiniMessage.miniMessage().deserialize(locale.getString("beacon.YouMustCapturedBeacon", "<red>You must capture the beacon first!</red>"));
        beaconYouNeedThisMuchExp = locale.getString("beacon.YouNeedThisMuchExp", "<gold>You need <yellow><number></yellow> exp points to link these beacons.</gold>");
        beaconYouReceivedAReward = MiniMessage.miniMessage().deserialize(locale.getString("beacon.YouReceivedAReward", "<green><bold>You received a reward!</bold></green>"));
        beaconClaimingBeaconAt = locale.getString("beacon.ClaimingBeaconAt", "<aqua>Claiming beacon at <yellow><location></yellow></aqua>");
        errorAlreadyExists = locale.getString("error.AlreadyExists", "<red><yellow><name></yellow> already exists!</red>");
        errorCanOnlyPlaceBlocks = MiniMessage.miniMessage().deserialize(locale.getString("error.CanOnlyPlaceBlocks", "<red>You can only place blocks on a captured beacon!</red>"));
        errorCanOnlyPlaceBlocksUpTo = locale.getString("error.CanOnlyPlaceBlocksUpTo", "<red>You can only place blocks up to <yellow><value></yellow> high around the beacon!</red>");
        errorClearAboveBeacon = MiniMessage.miniMessage().deserialize(locale.getString("error.ClearAboveBeacon", "<red>Clear blocks above before placing this block!</red>"));
        errorClearAroundBeacon = MiniMessage.miniMessage().deserialize(locale.getString("error.ClearAroundBeacon", "<red>Clear around and above the beacon to capture!</red>"));
        errorDistribution = locale.getString("error.Distribution", "<red>distribution <yellow><fraction></yellow> - must be less than 1</red>");
        errorError = MiniMessage.miniMessage().deserialize(locale.getString("error.error", "<red><bold>Error:</bold> </red>"));
        errorInventoryFull = MiniMessage.miniMessage().deserialize(locale.getString("error.InventoryFull", "<gold>Your inventory is full! Dropping items!</gold>"));
        errorNoBeaconThere = MiniMessage.miniMessage().deserialize(locale.getString("error.NoBeaconThere", "<red>There is no beacon there!</red>"));
        errorNoGames = MiniMessage.miniMessage().deserialize(locale.getString("error.NoGames", "<red>Could not find any games.</red>"));
        errorNoLobbyYet = MiniMessage.miniMessage().deserialize(locale.getString("error.NoLobbyYet", "<gold>Hmm, there is no lobby yet...</gold>"));
        errorNoSuchGame = MiniMessage.miniMessage().deserialize(locale.getString("error.NoSuchGame", "<red>No such game!</red>"));
        errorNoSuchTeam = MiniMessage.miniMessage().deserialize(locale.getString("error.NoSuchTeam", "<red>Could not find faction!</red>"));
        errorNotEnoughExperience = MiniMessage.miniMessage().deserialize(locale.getString("error.NotEnoughExperience", "<red>You do not have enough experience to do that!</red>"));
        errorNotInGame = locale.getString("error.NotInGame", "<red>You are not in the game '<yellow><game></yellow>'!</red> <aqua>Going to the lobby...</aqua>");
        errorNotInRegister = MiniMessage.miniMessage().deserialize(locale.getString("error.NotInRegister", "<red>Error: block isBeacon() but is not in the Register</red>"));
        errorNotReady = MiniMessage.miniMessage().deserialize(locale.getString("error.notReady", "<gold>Sorry, that is not ready yet.</gold>"));
        errorOnlyPlayers = MiniMessage.miniMessage().deserialize(locale.getString("error.OnlyPlayers", "<red>Only players can do that!</red>"));
        errorRegionLimit = MiniMessage.miniMessage().deserialize(locale.getString("error.RegionLimit", "<red>That's the limit of the game region, you can't go any further that way.</red>"));
        errorRequestCanceled = MiniMessage.miniMessage().deserialize(locale.getString("error.RequestCanceled", "<gold>Request canceled.</gold>"));
        errorTooFar = locale.getString("error.TooFar", "<red>That beacon is too far away.</red> <aqua>To link over <yellow><max></yellow> blocks, use gold or diamond range extender blocks.</aqua>");
        errorUnknownCommand = MiniMessage.miniMessage().deserialize(locale.getString("error.UnknownCommand", "<red>Unknown command!</red>"));
        errorUnknownPlayer = MiniMessage.miniMessage().deserialize(locale.getString("error.UnknownPlayer", "<red>Unknown or offline player!</red>"));
        errorYouCannotBuildThere = MiniMessage.miniMessage().deserialize(locale.getString("error.YouCannotBuildThere", "<red>You cannot build there!</red>"));
        errorYouCannotDoThat = MiniMessage.miniMessage().deserialize(locale.getString("error.YouCannotDoThat", "<red>You cannot do that!</red>"));
        errorYouCannotRemoveOtherPlayersBlocks = MiniMessage.miniMessage().deserialize(locale.getString("error.YouCannotRemoveOtherPlayersBlocks", "<red>You cannot remove other player's blocks!</red>"));
        errorYouDoNotHavePermission = MiniMessage.miniMessage().deserialize(locale.getString("error.YouDoNotHavePermission", "<red>You do not have permission to use this command!</red>"));
        errorYouHaveToBeStandingOnABeacon = MiniMessage.miniMessage().deserialize(locale.getString("error.YouHaveToBeStandingOnABeacon", "<red>You have to be standing on a beacon</red>"));
        errorYouMustBeInAGame = MiniMessage.miniMessage().deserialize(locale.getString("error.YouMustBeInAGame", "<red>You must be in a game to do that!</red>"));
        errorYouMustBeInATeam = MiniMessage.miniMessage().deserialize(locale.getString("error.YouMustBeInATeam", "<red>You must be in a faction to do that!</red>"));
        errorYouNeedToBeLevel = locale.getString("error.YouNeedToBeLevel", "<red>You need to be level <yellow><value></yellow> to do that!</red>");
        generalFailure = MiniMessage.miniMessage().deserialize(locale.getString("general.Failure", "<red>Failure!</red>"));
        generalGame = MiniMessage.miniMessage().deserialize(locale.getString("general.Game", "<aqua>Game</aqua>"));
        generalGames = MiniMessage.miniMessage().deserialize(locale.getString("general.Games", "<aqua>Games</aqua>"));
        generalLevel = MiniMessage.miniMessage().deserialize(locale.getString("general.Level", "<aqua>Level</aqua>"));
        generalLinks = MiniMessage.miniMessage().deserialize(locale.getString("general.Links", "<aqua>Links</aqua>"));
        generalLocation = locale.getString("general.Location", "<aqua>Location</aqua>");
        generalMembers = MiniMessage.miniMessage().deserialize(locale.getString("general.Members", "<aqua>Members</aqua>"));
        generalNone = MiniMessage.miniMessage().deserialize(locale.getString("general.None", "<gray>None</gray>"));
        generalSuccess = MiniMessage.miniMessage().deserialize(locale.getString("general.Success", "<green>Success!</green>"));
        generalTeam = MiniMessage.miniMessage().deserialize(locale.getString("general.Team", "<aqua>Faction</aqua>"));
        generalTeams = MiniMessage.miniMessage().deserialize(locale.getString("general.Teams", "<aqua>Factions</aqua>"));
        generalUnowned = MiniMessage.miniMessage().deserialize(locale.getString("general.Unowned", "<gray>Unowned</gray>"));
        helpAdminClaim = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminClaim", "<gray> - force-claims a beacon in a game</gray>"));
        helpAdminDelete = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminDelete", "<gray> - deletes the game and regenerates chunks</gray>"));
        helpAdminDistribution = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminDistribution", "<gray> - sets global beacon distribution temporarily</gray>"));
        helpAdminForceEnd = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminForceEnd", "<gray> - forces a game to end immediately</gray>"));
        helpAdminGames = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminGames", "<gray> - list existing games</gray>"));
        helpAdminJoin = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminJoin", "<gray> - join a faction in an active game</gray>"));
        helpAdminKick = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminKick", "<gray> - kicks a player from the game</gray>"));
        helpAdminLink = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminLink", "<gray> - force-links a beacon you are standing on to one at x,z</gray>"));
        helpAdminList = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminList", "<gray> - lists all known beacons in the game | all games owned by faction</gray>"));
        helpAdminListParms = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminListParms", "<gray> - lists game parameters</gray>"));
        helpAdminNewGame = locale.getString("help.AdminNewGame", "<gray> - creates a new game in an empty region; parameters are optional - do <yellow>/<label> newgame help</yellow> for a list of the possible parameters</gray>");
        helpAdminPause = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminPause", "<gray> - pauses the timer and scoreboard in a game</gray>"));
        helpAdminRegenerate = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminRegenerate", "<gray> - regenerates game area chunks and resets game</gray>"));
        helpAdminReload = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminReload", "<gray> - reloads the plugin, preserving existing games</gray>"));
        helpAdminRestart = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminRestart", "<gray> - restarts the game with currently defined parameters - clears scoreboard, cleans out all beacons, restarts timer; factions aren't changed</gray>"));
        helpAdminResume = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminResume", "<gray> - resume a paused game</gray>"));
        helpAdminSetGameParms = locale.getString("help.AdminSetGameParms", "<gray> - defines a game's parameters - DOES NOT restart the game (use restart for that) - do <yellow>/<label> setgameparms help</yellow> for a list of the possible parameters</gray>");
        helpAdminSetLobbySpawn = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminSetLobbySpawn", "<gray> - sets the lobby spawn point when in the lobby area</gray>"));
        helpAdminSetTeamSpawn = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminSetTeamSpawn", "<gray> - sets the spawn point for faction</gray>"));
        helpAdminSwitch = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminSwitch", "<gray> - switches faction when in a game</gray>"));
        helpAdminTeams = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminTeams", "<gray> - shows factions and faction members for a game</gray>"));
        helpAdminTimerToggle = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminTimerToggle", "<gray> - toggles the scoreboard timer on and off</gray>"));
        helpAdminTitle = MiniMessage.miniMessage().deserialize(locale.getString("help.AdminTitle", "<gold><bold>Beaconz Admin Commands</bold></gold>"));
        helpHelp = MiniMessage.miniMessage().deserialize(locale.getString("help.Help", "<gray> - this help</gray>"));
        helpJoin = MiniMessage.miniMessage().deserialize(locale.getString("help.Join", "<gray> - join an ongoing game</gray>"));
        helpLeave = MiniMessage.miniMessage().deserialize(locale.getString("help.Leave", "<gray> - leave a game</gray>"));
        helpLine = MiniMessage.miniMessage().deserialize(locale.getString("help.Line", "<gold>==================================================</gold>"));
        helpLobby = MiniMessage.miniMessage().deserialize(locale.getString("help.Lobby", "<gray> - go the lobby area</gray>"));
        helpLocation = MiniMessage.miniMessage().deserialize(locale.getString("help.Location", "<gray> - tells you where you are</gray>"));
        helpScore = MiniMessage.miniMessage().deserialize(locale.getString("help.Score", "<gray> - show the faction scores</gray>"));
        helpScoreboard = MiniMessage.miniMessage().deserialize(locale.getString("help.Scoreboard", "<gray> - toggles the scoreboard on and off</gray>"));
        scoreCongratulations = MiniMessage.miniMessage().deserialize(locale.getString("score.Congratulations", "<green><bold>Congratulations!</bold></green>"));
        scoreGameOver = locale.getString("score.GameOver", "<gold><bold><< GAME OVER >></bold></gold>");
        scoreGetTheMostGoal = locale.getString("score.GetTheMostGoal", "<green><bold><< Get the most <yellow><goal></yellow>!! >></bold></green>");
        scoreGetValueGoal = locale.getString("score.GetValueGoal", "<green><bold><< Get <yellow><value></yellow> <yellow><goal></yellow>!! >></bold></green>");
        scoreGame = locale.getString("score.game", "<aqua><score> <unit></aqua>");
        scoreGameModeMiniGame = locale.getString("score.gamemode.minigame", "Minigame");
        scoreGoalArea = locale.getString("score.goals.area", "Area");
        scoreGoalBeacons = locale.getString("score.goals.beacons", "Beacons");
        scoreGoalTime = locale.getString("score.goals.time", "Time");
        scoreGoalTriangles = locale.getString("score.goals.triangles", "Triangles");
        scoreGoalLinks = locale.getString("score.goals.links", "Links");
        scoreNewScore = MiniMessage.miniMessage().deserialize(locale.getString("score.NewScore", "<green>New score</green>"));
        scoreNoWinners = MiniMessage.miniMessage().deserialize(locale.getString("score.NoWinners", "<gray>There were no winners!</gray>"));
        scoreScores = MiniMessage.miniMessage().deserialize(locale.getString("score.Scores", "<aqua><bold>Scores:</bold></aqua>"));
        scoreTeam = locale.getString("score.team", "<team>");
        scoreStrategy = locale.getString("score.gamemode.strategy", "Strategy");
        scoreTeamWins = locale.getString("score.TeamWins", "<green><bold><team> FACTION WINS!!!</bold></green>");
        // Score GUI strings
        scoreGuiTitle = locale.getString("score.gui.title", "<aqua><bold>Scores - <game></bold></aqua>");
        scoreGuiTeamHeader = locale.getString("score.gui.teamHeader", "<team>");
        scoreGuiTeamPlayers = locale.getString("score.gui.teamPlayers", "<gray>Players: <yellow><count></yellow></gray>");
        scoreGuiScoreName = locale.getString("score.gui.scoreName", "<yellow><type></yellow>: <green><bold><score></bold></green>");
        scoreGuiScoreTeam = locale.getString("score.gui.scoreTeam", "<gray>Team: <team></gray>");
        scoreGuiScoreValue = locale.getString("score.gui.scoreValue", "Score: ");
        startMostObjective = locale.getString("start.MostObjective", "<aqua>Your faction's objective is to capture the most <yellow><goal></yellow>!</aqua>");
        startObjective = locale.getString("start.Objective", "<aqua>Your faction's objective is to capture <yellow><value></yellow> <yellow><goal></yellow>!</aqua>");
        startYoureAMember = locale.getString("start.YoureAMember", "<aqua>You're a member of <yellow><name></yellow> faction!</aqua>");
        startYourePlaying = locale.getString("start.YourePlaying", "<aqua>You're playing game <yellow><name></yellow> in <yellow><mode></yellow> mode!</aqua>");
        teleportDoNotMove = locale.getString("teleport.DoNotMove", "<gold>Do not move, teleporting in <yellow><number></yellow> seconds!</gold>");
        teleportYouMoved = MiniMessage.miniMessage().deserialize(locale.getString("teleport.YouMoved", "<red>You moved! Cancelling teleport!</red>"));
        titleBeaconz = MiniMessage.miniMessage().deserialize(locale.getString("title.Beaconz", "<gold><bold>Beaconz</bold></gold>"));
        titleBeaconzNews = MiniMessage.miniMessage().deserialize(locale.getString("title.BeaconzNews", "<gold><bold>Beaconz News</bold></gold>"));
        titleCmdLocation = MiniMessage.miniMessage().deserialize(locale.getString("title.CmdLocation", "<aqua>You're in the Beaconz Lobby at</aqua>"));
        titleCmdYourePlaying = locale.getString("title.CmdYourePlaying", "<aqua>You're playing Beaconz game <yellow><game></yellow></aqua>");
        titleLobbyInfo = locale.getString("title.LobbyInfo", "<gold>Welcome to Beaconz!</gold>|<aqua>You are in the lobby area.</aqua>|<green>Hit a sign to start a game!</green>|<gray>Beaconz is a faction game where</gray>|<gray>you try to find, claim and link</gray>|<gray>naturally occuring beaconz in</gray>|<gray>the world. You can mine beaconz</gray>|<gray>for goodies and defend them</gray>|<gray>with blocks and traps.</gray>");
        titleSubTitle = MiniMessage.miniMessage().deserialize(locale.getString("title.SubTitle", "<italic>Capture, link & defend beaconz!</italic>"));
        titleSubTitleColor = getTextColor(locale.getString("title.SubTitleColor", "gold"));
        titleWelcome = MiniMessage.miniMessage().deserialize(locale.getString("title.Welcome",  "<gold><bold>Welcome to Beaconz!</bold></gold>"));
        titleWelcomeBackToGame = locale.getString("title.WelcomeBackToGame", "<green>Welcome back to Beaconz game <yellow><name></yellow>!</green>");
        titleWelcomeColor = getTextColor(locale.getString("title.WelcomeColor", "gold"));
        titleWelcomeToGame = locale.getString("title.WelcomeToGame", "<green>Welcome to Beaconz game <yellow><name></yellow>!</green>");
        triangleCouldNotMakeTriangle = MiniMessage.miniMessage().deserialize(locale.getString("triangle.CouldNotMakeTriangle", "<gold>One triangle could not be created because of overlapping enemy elements!</gold>"));
        triangleCouldNotMakeTriangles = locale.getString("triangle.CouldNotMakeTriangles", "<gold><yellow><number></yellow> triangles could not be created because of overlapping enemy elements!</gold>");
        triangleDroppingToLevel = locale.getString("triangle.DroppingToLevel ",  "<gold><yellow><team></yellow>'s triangle area level dropping to <yellow><level></yellow></gold>");
        triangleEntering = locale.getString("triangle.Entering ", "<aqua>Now entering <yellow><team></yellow>'s triangle area level <yellow><level></yellow></aqua>");
        triangleLeaving = locale.getString("triangle.Leaving", "<aqua>Leaving <yellow><team></yellow>'s triangle area</aqua>");
        triangleThisBelongsTo = locale.getString("triangle.ThisBelongsTo", "<red>This belongs to <yellow><team></yellow>!</red>");
        triangleYourTeamLostATriangle = MiniMessage.miniMessage().deserialize(locale.getString("triangle.YourTeamLostATriangle", "<red>Your faction lost a triangle!</red>"));
        triangleTeamLostATriangle = locale.getString("triangle.TeamLostATriangle", "<red><yellow><team></yellow> lost a triangle!</red>");
        linkLostLink = MiniMessage.miniMessage().deserialize(locale.getString("link.LostLink", "<red>Your team lost a link!</red>"));
        linkLostLinks = locale.getString("link.LostLinks", "<red>Your team lost <yellow><number></yellow> links!</red>");
        linkTeamLostLink = locale.getString("link.TeamLostLink", "<red><yellow><team></yellow> lost a link!</red>");
        linkTeamLostLinks = locale.getString("link.TeamLostLinks", "<red><yellow><team></yellow> lost <yellow><number></yellow> links!</red>");

        // Defense text
        defenseText = new HashMap<>();
        if (locale.isConfigurationSection("defenseText")) {
            for (String material : locale.getConfigurationSection("defenseText").getKeys(false)) {
                try {
                    Material mat = Material.valueOf(material.toUpperCase());
                    defenseText.put(mat,MiniMessage.miniMessage().deserialize(locale.getString("defenseText." + material,"")));
                } catch (Exception e) {
                    getLogger().severe("No not know what defenseText." + material + " is in locale file " + localeName + ".yml, skipping...");
                }
            }
        }
    }


    private TextColor getTextColor(@Nullable String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            return NamedTextColor.GOLD;
        }
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
