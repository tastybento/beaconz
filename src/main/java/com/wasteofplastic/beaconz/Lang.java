package com.wasteofplastic.beaconz;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;


/**
 * This class holds all the text strings. It is a precursor to enabling localization.
 * @author tastybento
 *
 */
public class Lang {

    public static String welcome = "Welcome to Beaconz!";
    public static String subTitle = "Capture, link & defend beaconz!";
    public static String welcomeColor = "gold";
    public static String subTitleColor = "gold";
    public static String welcomeToGame = "Welcome to Beaconz game [name]";
    public static String welcomeBackToGame = "Welcome back to Beaconz game [name]";
    public static String lobbyInfo = "Welcome to Beaconz!|You are in the lobby area.|Hit a sign to start a game!|"
            + "Beaconz is a team game where|you try to find, claim and link|naturally occuring beaconz in|"
            + "the world. You can mine beaconz|for goodies and defend them|with blocks and traps.";
    public static String startYourePlaying = "You're playing game [name] in [mode] mode!";
    public static String startYoureAMember = "You're a member of [name] team!";
    public static String startObjective = "Your team's objective is to capture [value] [goal]!";
    public static String startMostObjective = "Your team's objective is to capture the most [goal]!";

    public static String beaconz = "Beaconz";
    public static String beaconzNews = "Beaconz News";
    public static String success = "Success!";
    public static String failure = "Failure!";
    public static String notReady = "Sorry, that is not ready yet.";
    public static String team = "Team";
    public static String teams = "Teams";
    public static String game = "Game";
    public static String games = "Games";
    public static String members = "Members";
    public static String links = "Links";
    public static String unowned = "Unowned";
    public static String none = "None";
    public static String level = "Level";
    public static String location = "Location";


    public static String error = "Error: ";
    public static String errorYouCannotDoThat = "You cannot do that!";
    public static String errorYouMustBeOp = "You must be Op to use this command";
    public static String errorOnlyPlayers = "Only players can do that!";
    public static String errorYouHaveToBeStandingOnABeacon = "You have to be standing on a beacon";
    public static String errorNotInRegister = "Error: block isBeacon() but is not in the Register: ";
    public static String errorDistribution = " distribution <fraction> - must be less than 1";
    public static String errorYouMustBeInATeam = "You must be in a team to do that!";
    public static String errorYouMustBeInAGame = "You must be in a game to do that!";  
    public static String errorNoSuchTeam = "Could not find team!";
    public static String errorNoSuchGame = "No such game!";
    public static String errorNoGames = "Could not find any games.";
    public static String errorUnknownPlayer = "Unknown player!";
    public static String errorUnknownCommand = "Unknown command!";
    public static String errorYouCannotBuildThere = "You cannot build there!";
    public static String errorClearAroundBeacon = "Clear around and above the beacon to capture!";
    public static String errorClearAboveBeacon = "Clear blocks above before placing this block!";
    public static String errorNotEnoughExperience = "You do not have enough experience to do that!";
    public static String errorNotInGame = "You are not in the game '[game]'! Going to the lobby...";
    public static String errorNoBeaconThere = "There is no beacon there!";
    public static String errorAlreadyExists = "[name] already exists!";
    public static String errorCanOnlyPlaceBlocks = "You can only place blocks on a captured beacon!";
    public static String errorCanOnlyPlaceBlocksUpTo = "You can only place blocks up to [value] high around the beacon!";
    public static String errorYouNeedToBeLevel = "You need to be level [value] to do that!";

    public static String helpLine = "======================================================";

    public static String helpHelp = "- this help";
    public static String helpJoin = "- join an ongoing game";
    public static String helpLeave = "- leave a game";
    public static String helpLobby = "- go the lobby area";
    public static String helpLocation = "- tells you where you are";
    public static String helpScore = "- show the team scores";
    public static String helpScoreboard = "- toggles the scoreboard on and off";

    public static String cmdLocation = "You're in the Beaconz Lobby at";
    public static String cmdYourePlaying = "You're playing Beaconz game [game]";

    public static String helpAdminTitle = "Beaconz Admin Commands";
    public static String helpAdminClaim = " - force-claims a beacon in a game";
    public static String helpAdminDelete = " - deletes the game";
    public static String helpAdminDistribution = " - sets global beacon distribution temporarily";
    public static String helpAdminJoin = " - join a team in an active game";
    public static String helpAdminGames = " - list existing games";
    public static String helpAdminKick = "- kicks a player from the game";
    public static String helpAdminRestart = " - restarts the game with currently defined parameters - clears scoreboard, cleans out all beacons, restarts timer; teams aren't changed";
    public static String helpAdminReset = " - resets score, teams, and repopulates the beacons!";
    public static String helpAdminPause = " - pauses the timer and scoreboard in a game";
    public static String helpAdminResume = " - resume a paused game";
    public static String helpAdminForceEnd = " - forces a game to end immediately";
    public static String helpAdminLink = " - force-links a beacon you are standing on to one at x,z";
    public static String helpAdminList = " - lists all known beacons in the game | all games owned by team";
    public static String helpAdminListParms = " - lists game parameters";
    public static String helpAdminNewGame = " - creates a new game in an empty region; parameters are optional - do /[label] newgame help for a list of the possible parameters";
    public static String helpAdminReload = " - reloads the plugin, preserving existing games";
    public static String helpAdminSetGameParms = " - defines a game's parameters - DOES NOT restart the game (use restart for that) - do /[label] setgameparms help for a list of the possible parameters";
    public static String helpAdminSetTeamSpawn = " - sets the spawn point for team";
    public static String helpAdminSetLobbySpawn = " - sets the lobby spawn point when in the lobby area";
    public static String helpAdminSwitch = " - switches your team when in a game";
    public static String helpAdminTeams = " - shows teams and team members for a game";
    public static String helpAdminTimerToggle = " - toggles the scoreboard timer on and off";

    public static String claimingBeaconAt = "Claiming beacon at [loction]";
    public static String beaconClaimedForTeam = "Beacon claimed for team [team]";
    public static String distributionSettingTo = "Setting beacon distribution to [value]";
    public static String switchedToTeam = "Switched to [team]!";
    public static String youAreInTeam = "You are in [team]!"; 

    public static String scoreGameOver = "<< GAME OVER >>";
    public static String scoreTeamWins = "[team] TEAM WINS!!!";
    public static String scoreCongratulations = "Congratulations";
    public static String scoreNoWinners = "There were no winners!";
    public static String scoreGetTheMostGoal = "<< Get the most [goal]!! >>";
    public static String scoreGetValueGoal = "<< Get [value] [goal]!! >>";
    public static String scoreNewScore = "New score";

    public static String beaconYouCapturedABeacon = "You captured a beacon! Mine the beacon for more beacon maps.";
    public static String beaconYouCannotDestroyYourOwnBeacon = "You cannot destroy your own beacon";
    public static String beaconYouDestroyed = "You destroyed [team] team's beacon!";
    public static String beaconPlayerDestroyed = "[player] destroyed one of [team]'s beacons!";
    public static String beaconTeamDestroyed = "[team1] destroyed one of [team2]'s beacons!";
    public static String beaconYouMustCapturedBeacon = "You must capture the beacon first!";
    public static String beaconTheMapDisintegrates = "The map disintegrates!";
    public static String beaconYouHaveAMap = "You have a beacon map! Take it to another beacon to link them up!";
    public static String beaconYouCannotLinkToSelf = "You cannot link a beacon to itself!";
    public static String beaconMaxLinks = "This beacon already has [number] outbound links!";
    public static String beaconLinkAlreadyExists = "Link already exists!";
    public static String beaconLinkCannotCrossEnemy = "Link cannot cross enemy link!";
    public static String beaconLinkCreated = "Link created!";
    public static String beaconNowHasLinks = "This beacon now has [number] links.";
    public static String beaconNameCreatedALink = "[name] created a link!";
    public static String beaconLinkCouldNotBeCreated = "Link could not be created!";
    public static String beaconTriangleCreated = "Triangle created!";
    public static String beaconNameCreateATriangle = "[name] created a triangle!";
    public static String beaconNameCreateTriangles = "[name] created [number] triangles!";
    public static String beaconCannotPlaceLiquids = "You cannot place liquids above a beacon!";
    public static String beaconIsExhausted = "Beacon is exhausted. Try again in [minutes] minute(s)";
    public static String beaconOriginNotOwned = "Origin beacon is not owned by [team]!";
    public static String beaconThisBeaconIsBlocksAway = "This beacon is [number] blocks away.";
    public static String beaconYouCanLinkUpTo = "You can link up to [number] blocks away.";
    public static String beaconYouCanOnlyExtend = "You can only extend a captured beacon!";
    public static String beaconCannotBeExtended = "Cannot be extended any further in this direction!";
    public static String beaconExtended = "You extended the beacon!";

    public static String regionLimit = "That's the limit of the game region, you can't go any further that way.";

    public static String triangleLeaving = "Leaving [team]'s control area";
    public static String triangleEntering = "Now entering [team]'s control area level [level]";
    public static String triangleDroppingToLevel = "[team]'s control level dropping to [level]";
    public static String triangleCouldNotMake = "One triangle could not be created because of overlapping enemy elements!";
    public static String trianglesCouldNotMake = "[number] triangles could not be created because of overlapping enemy elements!";
    public static String triangleThisBelongsTo = "This belongs to [team]!";

    public static String adminGamesDefined = "The following games/regions are defined:";
    public static String adminGamesTheLobby = "The Lobby";
    public static String adminGamesNoOthers = "...and no others.";

    public static String adminKickAllPlayers = "All players were kicked from game [name]";
    public static String adminKickPlayer = "[player] was kicked from game [name]";

    public static String adminRestart = "Restarted game [name]";

    public static String adminDeletingGame = "Deleting game [name]... (This may take some time)";
    public static String adminDeletedGame = "Deleted [name].";
    public static String adminResettingGame = "Resetting game [name]. This may take several minutes. Please wait for the 'reset complete' message.";

    public static String adminPaused = "Paused the game [name]. To restart, use /[label] resume <game>";

    public static String adminResume = "Game [name] is back ON!!";

    public static String adminForceEnd = "Game [name] has ended.";
    public static String adminForceRestart = "To restart the game, use " +  "/[label] game restart <gamename>";

    public static String adminNewGameBuilding = "Building a new game with given parameters. Please wait...";

    public static String adminReload = "Beaconz plugin reloaded. All existing games were preserved.";

    public static String adminParmsMode = "Mode";
    public static String adminParmsTeams = "# of Teams";
    public static String adminParmsGoal = "Goal";
    public static String adminParmsGoalValue = "Goal Value";
    public static String adminParmsCountdown = "Countdown";
    public static String adminParmsScoreTypes = "Score Types";
    public static String adminParmsUnlimited = "Unlimited";
    public static String adminParmsArgumentsPairs = "Arguments must be given in pairs, separated by colons.";
    public static String adminParmsDoesNotExist = "Parameter [name] does not exist.";

    public static String adminSetSpawnNeedToBeInGame = "You need to be in the region of an active game";

    public static String adminListBeaconsInGame = "Known beacons in game [name]:";

    public static String adminResetComplete = "Reset complete. Regenerated [number] chunks.";

    public static HashMap<Material, String> defenseText = new HashMap<Material,String>();
    static {
        defenseText.put(Material.BEDROCK,"Ultimate defense! Hope you didn't make a mistake!");
        defenseText.put(Material.BED_BLOCK,"Sleep, sleep, sleep");
        defenseText.put(Material.BOOKSHELF,"Knowledge is power!");
        defenseText.put(Material.BREWING_STAND,"Potion attack!");
        defenseText.put(Material.CAKE_BLOCK,"Hunger benefits");
        defenseText.put(Material.CARPET,"Hmm, pretty!");
        defenseText.put(Material.CAULDRON,"Witch's brew!");
        defenseText.put(Material.CHEST,"I wonder what you will put in it");
        defenseText.put(Material.COAL_BLOCK,"Energy up!");
        defenseText.put(Material.DAYLIGHT_DETECTOR,"Let night be day!");
        defenseText.put(Material.DETECTOR_RAIL,"Detect what?");
        defenseText.put(Material.DIAMOND_BLOCK,"Fortune will smile upon you!");
        defenseText.put(Material.DISPENSER,"Load it up with ammo to make an auto turret!");            
        defenseText.put(Material.DRAGON_EGG,"The end is nigh!");
        defenseText.put(Material.DROPPER,"Drip, drop, drip");
        defenseText.put(Material.EMERALD_BLOCK,"Place adjacent to the beacon base to extend the beacon!");
        defenseText.put(Material.EMERALD_ORE,"Where did that come from?");
        defenseText.put(Material.ENCHANTMENT_TABLE,"Magic will occur");
        defenseText.put(Material.ENDER_CHEST,"I wonder what is inside?");
        defenseText.put(Material.ENDER_STONE,"End attack!");
        defenseText.put(Material.FLOWER_POT,"I wonder what this will do...");
        defenseText.put(Material.FURNACE,"Fire attack! If it's hot.");
        defenseText.put(Material.GLASS,"I can see clearly now");
        defenseText.put(Material.GLOWSTONE,"Glow, glow");
        defenseText.put(Material.GOLD_BLOCK,"Money, money, money");
        defenseText.put(Material.ICE,"Brrr, it's cold");
        defenseText.put(Material.IRON_BLOCK,"Protection");
        defenseText.put(Material.IRON_DOOR_BLOCK,"Knock, knock");
        defenseText.put(Material.JACK_O_LANTERN,"Boo!");
        defenseText.put(Material.LADDER,"Up we go!");
        defenseText.put(Material.LAPIS_BLOCK,"Everything is blue!");
        defenseText.put(Material.LAVA,"Hot stuff!");
        defenseText.put(Material.LEAVES,"Camoflage");
        defenseText.put(Material.LEAVES_2,"Camoflage");
        defenseText.put(Material.LEVER,"I wonder what this does!");
        defenseText.put(Material.LOG,"It's a tree!");
        defenseText.put(Material.LOG_2,"It's a tree!");
        defenseText.put(Material.MELON_BLOCK,"Hungry?");
        defenseText.put(Material.MOB_SPAWNER,"That's what I'm talking about!");
        defenseText.put(Material.MYCEL,"Smelly!");
        defenseText.put(Material.NETHERRACK,"That's not from around here!");
        defenseText.put(Material.NETHER_BRICK,"That's not from around here!");
        defenseText.put(Material.NETHER_BRICK_STAIRS,"That's not from around here!");
        defenseText.put(Material.NETHER_FENCE,"That's not from around here!");
        defenseText.put(Material.NOTE_BLOCK,"I hear things?");
        defenseText.put(Material.OBSIDIAN,"Tough protection!");
        defenseText.put(Material.PACKED_ICE,"Cold, so cold...");
        defenseText.put(Material.PISTON_BASE,"Pushy!");
        defenseText.put(Material.PISTON_STICKY_BASE,"Pushy!");
        defenseText.put(Material.POWERED_RAIL,"Power to the people!");
        defenseText.put(Material.PRISMARINE,"Aqua");
        defenseText.put(Material.PUMPKIN,"Farming?");
        defenseText.put(Material.QUARTZ_BLOCK,"Pretty");
        defenseText.put(Material.RAILS,"Where do they go?");
        defenseText.put(Material.REDSTONE_BLOCK,"Power up!");
        defenseText.put(Material.REDSTONE_COMPARATOR,"What's the question?");
        defenseText.put(Material.REDSTONE_COMPARATOR_OFF,"What's the question?");
        defenseText.put(Material.REDSTONE_COMPARATOR_ON,"What's the question?");
        defenseText.put(Material.REDSTONE_LAMP_OFF,"Light?");
        defenseText.put(Material.REDSTONE_LAMP_ON,"Light?");
        defenseText.put(Material.REDSTONE_TORCH_OFF,"Power gen");
        defenseText.put(Material.REDSTONE_TORCH_ON,"Power gen");
        defenseText.put(Material.REDSTONE_WIRE,"Does it glow?");
        defenseText.put(Material.RED_SANDSTONE,(ChatColor.RED + "It's red"));
        defenseText.put(Material.RED_SANDSTONE_STAIRS,(ChatColor.RED + "It's red"));
        defenseText.put(Material.SAND,"Weak");
        defenseText.put(Material.SANDSTONE,(ChatColor.YELLOW + "It's yellow"));
        defenseText.put(Material.SANDSTONE_STAIRS,(ChatColor.YELLOW + "It's yellow"));
        defenseText.put(Material.SEA_LANTERN,"Nice! Sea attack!");
        defenseText.put(Material.SIGN_POST,"Warning message set!");
        defenseText.put(Material.SKULL,"Death to this entity!");
        defenseText.put(Material.SLIME_BLOCK,"Boing, boing, boing!");
        defenseText.put(Material.SNOW_BLOCK,"Cold!");
        defenseText.put(Material.SOUL_SAND,"<scream>");
        defenseText.put(Material.SPONGE,"Slurp!");
        defenseText.put(Material.STAINED_GLASS,"Pretty!");
        defenseText.put(Material.STAINED_GLASS_PANE,"Pretty!");
        defenseText.put(Material.STANDING_BANNER,"Be proud!");
        defenseText.put(Material.STATIONARY_LAVA,"A moat?");
        defenseText.put(Material.STATIONARY_WATER,"A moat?");
        defenseText.put(Material.STONE_PLATE,"A trap?");
        defenseText.put(Material.THIN_GLASS,"Not much protection...");
        defenseText.put(Material.TNT,"Explosive protection!");
        defenseText.put(Material.WALL_SIGN,"Send a message!");
        defenseText.put(Material.WEB,"Slow down the enemy!");
        defenseText.put(Material.WOOD_PLATE,"Trap?");
        defenseText.put(Material.WOOL,"Keep warm!");
        defenseText.put(Material.WORKBENCH,"That's helpful!");
    }
    public static String defensePlaced = "Defense placed";
    public static String defenseRemoveTopDown = "Remove blocks top-down";
    
    public static String mapBeaconMap = "Beacon Map";
    public static String mapUnknownBeacon = "Unknown beacon";
    public static String doNotMove = "Do not move, teleporting in [number] seconds!";
    public static String youMoved = "You moved! Cancelling teleport!";
    public static String errorInventoryFull = "You inventory is full! Dropping items!";
    
}
