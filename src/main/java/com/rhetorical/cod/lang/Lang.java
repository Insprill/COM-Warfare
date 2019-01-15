package com.rhetorical.cod.lang;

import com.rhetorical.cod.Main;
import com.rhetorical.cod.files.LangFile;
import org.bukkit.ChatColor;

public enum Lang {

	UNKNOWN_COMMAND("&cUnknown Command! Try using '/cod help' for a list of commands."),
	NO_PERMISSION("&cYou don't have permission to do that!"),
	INCORRECT_USAGE("&cIncorrect usage! Correct usage: '{command}'"),
	MUST_BE_PLAYER("&cYou must be a player to execute this command."),
	NOT_PROPER_PAGE("&cYou didn't specify a proper page."),
	MAP_LIST_HEADER("&f=====&6&flMap List&r&f====="),
	MAP_LIST_ENTRY( "{map-id} - &6&lName: &r&a {map-name} &r&6&lGamemode: &r&c {game-mode} &r&6&lStatus: {map-status}"),
	CREATE_MAP_ALREADY_EXISTS("&cThere already exists a map with that name!"),
	CREATE_MAP_SUCCESS("&aSuccessfully created map {map-name}!"),
	REMOVE_MAP_SUCCESS("&aSuccessfully removed map!"),
	MAP_NOT_EXISTS_WITH_NAME("&cNo map exists with that name!"),
	GAME_MODE_NOT_EXISTS_WITH_NAME("&cNo game mode exists with that name!"),
	GAME_MODE_NOT_SET_UP_ON_MAP("&cThat game mode is not set up on that map!"),
	SET_LOBBY_SUCCESS("&aSuccessfully set lobby to your location! (You might want to restart the server for it to take effect)"),
	SET_SPAWN_SUCCESS("&aSuccessfully created {team} spawn for map {map-name}!"),
	TEAM_NOT_EXISTS_WITH_NAME("&cNo team exists with that name!"),
	SET_FLAG_CTF_SUCCESS("&aSuccessfully set {team} flag spawn!"),
	SET_FLAG_DOM_SUCCESS("&aSuccessfully set {flag} DOM flag spawn!"),
	LOBBY_NOT_EXISTS("&cThere is no lobby to send you to!"),
	BALANCE_COMMAND("&aYou have {credits} credits!"),
	GIVE_BALANCE_COMMAND("&aSuccessfully gave {player} {amount} credits! They now have {total} credits!"),
	SET_BALANCE_COMMAND("&aSuccessfully set {player}'s balance to {amount} credits!"),
	FORCE_START_FAIL("&cCould not force start arena!"),
	COULD_NOT_FIND_GAME_PLAYER_IN("&cCould not find the game that the player is in!"),
	MUST_NOT_BE_IN_GAME("&cYou must not be in game to use that command!"),
	MUST_BE_IN_GAME("&cYou must be in game to use that command!"),
	PLAYERS_BOOTED_SUCCESS("&aAll players removed from all games!"),
	PLAYER_BOOTED_FAILURE("&cCouldn't boot all players successfully!"),
	WEAPON_NOT_FOUND_WITH_NAME("&cCould not find weapon with the name: {gun-name}!"),
	OITC_GUN_SET_SUCCESS("&aSuccessfully set OITC gun to {gun-name}!"),
	GUN_PROGRESSION_ADDED_SUCCESS("&aSuccessfully added gun to the Gun Game progression!"),
	MAP_CHANGE_SUCCESS("&aSuccessfully changed map to &6&l{map-name}&r&a!"),
	GAME_MODE_CHANGE_SUCCESS("&aSuccessfully changed game mode to &6&l{game-mode}&r&a!"),
	BLACKLIST_SUCCESS("&aSuccessfully blacklisted mode {mode} from {map-name}!"),
	WEAPON_TYPE_NOT_EXISTS("&cThat weapon type does not exist! Valid types include 'tactical' and 'lethal'!"),
	GUN_TYPE_NOT_EXISTS("&cThat gun type does not exist! Valid types include 'primary' and 'secondary'!"),
	UNLOCK_TYPE_NOT_EXISTS("&cThat unlock type does not exist! Valid types include 'level', 'credits', and 'both'!"),
	MATERIAL_NOT_EXISTS("&cNo material exists with the name &7{name}&c!"),
	WEAPON_CREATED_SUCCESS("&aSuccessfully created weapon {weapon-name} as a {weapon-type} grenade!"),
	GUN_CREATED_SUCCESS("&aSuccessfully created gun {gun-name} as a {gun-type}!"),
	WEAPON_UNLOCKED("&aYou just unlocked the &6{gun-name}! \n &aEquip it after the match!"),
	WEAPON_PURCHASE_UNLOCKED("&aThe &6{gun-name} is now available for purchase!"),
	RANK_UP_MESSAGE("&7Congratulations! You just ranked up to level &e{level}&r&7!"),
	RANK_UP_PRESTIGE_MESSAGE("&7Congratulations! You just ranked up to prestige level &e{level}&r&7!"),
	RANK_UP_READY_TO_PRESTIGE("&7Congratulations! You have just reached the highest rank! Visit the prestige menu to get your reward!"),
	RANK_RESET_MESSAGE("&7Your rank has been reset!"),
	ERROR_SETTING_PLAYER_EXPERIENCE_LEVEL("&cThere was an error setting the player's experience level."),
	ERROR_READING_PERK_DATA("&cThere was an error reading the perk data from the config. The file may be corrupted!"),
	ERROR_READING_PLAYER_LOADOUT("&cError loading player's loadout from the config."),
	NO_PRIMARY("&cNo Primary"),
	NO_SECONDARY("&cNo Secondary"),
	NO_LETHAL("&cNo Lethal"),
	NO_TACTICAL("&cNo Tactical"),
	KNIFE_LORE("&6A knife that can kill players in one hit."),
	CLASS_PREFIX("Class"),
	CHAT_FORMAT("{team-color}{player}&r&f» &7{message}"),
	NO_LOBBY_SET("&cNo lobby location set!"),
	COULD_NOT_CREATE_MATCH_BECAUSE_NO_LOBBY("&7Could not create a match because there is no lobby location!"),
	ALREADY_IN_GAME("&7You are already in a game!"),
	SEARCHING_FOR_MATCH("&7Searching for match . . ."),
	COULD_NOT_FIND_MATCH("&7Could not find a match . . ."),
	CREATING_MATCH("&7Creating match . . ."),
	COULD_NOT_CREATE_MATCH_BECAUSE_NO_MAPS("&cCould not create match because there are not enough maps!"),
	CREATED_LOBBY("&7Created game lobby!"),
	FOUND_MATCH("&7Found match!"),
	PLAYER_JOINED_LOBBY("&7{player} has joined your lobby!"),
	PLAYER_LEAVE_GAME("&7You have left the lobby!"),
	PLAYER_NOT_IN_GAME("&7You are not in a match!"),
	ERROR_PLAYER_NOT_EXISTS("&cNo player exists with that name!"),
	RAN_OUT_OF_MAPS("&cCOM-Warfare ran out of maps to use!"),
	CURRENT_GAME_REMOVED("&cThe current game has been removed!"),
	INSUFFICIENT_FUNDS("&cInsufficient funds!"),
	PURCHASE_SUCCESSFUL("&aPurchase successful!"),
	PERK_ONE_MAN_ARMY_FAILED("&cYou moved and couldn't finish switching your class!"),
	PERK_FINAL_STAND_NOTIFICATION("&fYou are in final stand! Wait 20 seconds to get back up!"),
	PERK_FINAL_STAND_FINISHED("&fYou are out of final stand!"),
	ERROR_COULD_NOT_LOAD_KILL_STREAKS("&cCould not load kill streak information for {player}!"),
	KILL_STREAK_REQUIRED_KILLS("&f&lRequired Kills: &a{kills}"),
	UAV_NAME("&6&lUAV"),
	COUNTER_UAV_NAME("&6&lCounter UAV"),
	NUKE_NAME("&6&lNuke"),
	DOGS_NAME("&6&lDogs"),
	SCOREBOARD_PART1("Press 'TAB' to check"),
	SCOREBOARD_PART2("your scores!"),
	PLAYER_DOG_TAG_NAME("{team-color}{player}'s dog tag."),
	ASSIGNED_TO_TEAM("{team-color}You are on the {team} team!"),
	NOBODY_WON_GAME("&7Nobody won the match! It was a tie!"),
	RETURNING_TO_LOBBY("&fReturning to the lobby in {time} seconds!"),
	SOMEBODY_WON_GAME("&fThe {team} team won the match!"),
	SOMEONE_WON_GAME("&f{player} won the match!"),
	END_GAME_KILLS_DEATHS("&a&lKills: {kills} &c&lDeaths: {deaths} &f&lKDR: {kd}"),
	GAME_STARTING_MESSAGE("&7Game starting in {time}!"),
	GAME_STARTING_MAP_MESSAGE("&7Map: &a{map} &r&7Gamemode: &c{mode}"),
	MAP_VOTING_HEADER("&6Map Voting"),
	MAP_VOTING_NAMES("&71: &6{1} &7| 2: &b{2}"),
	MAP_VOTING_VOTES("&7Votes: &6{1} &7| Votes: &b{2}"),
	LOBBY_HEADER("&7Waiting in lobby."),
	LOBBY_FOOTER("&7Game Starts in {time}!"),
	GAME_STARTING("&7The game is starting now!"),
	MAP_VOTING_NEXT_MAP("&7The next map is set to: {map}"),
	TEAM_WON_ROUND("{team-color}The {team} won the round!"),
	NEXT_ROUND_STARTING("The next round will start in {time} seconds"),
	RESPAWN_IF_DOG_TAG_PICKED_UP("&cYou will respawn if your teammate pick up your dog tag!"),
	RESPAWN_NEXT_ROUND("&cYou will respawn next round!"),
	OITC_RAN_OUT_OF_LIVES("&cYou've ran out of lives!"),
	YOU_WILL_RESPAWN("&cYou will respawn in {time} seconds!"),
	KILLED_TEXT("killed"),
	DOWNED_TEXT("downed"),
	FLAG_DROPPED("&aThe {team-color}{team} &aflag has been dropped!"),
	TEAM_SCORED("{team-color}The {team} team scored!"),
	PLAYER_PICKED_UP_FLAG("&7{player} has picked up the {team-color}{team} &7flag!"),
	KILL_DENIED("&7Kill Denied!"),
	SPAWN_DENIED("&cYou just denied {player} a respawn!"),
	KILL_CONFIRMED("&7Kill Confirmed!"),
	FLAG_A("Flag A"),
	FLAG_B("Flag B"),
	FLAG_C("Flag C"),
	FLAG_CAPTURED("&eThe {team-color}{team} team has captured {flag}!"),
	FLAG_NEUTRALIZED("&e{flag} has been neutralized!"),
	INVENTORY_MAIN_MENU_TITLE("COM-Warfare - Menu"),
	INVENTORY_SHOP_MENU_TITLE("COM-Warfare - SHOP"),
	INVENTORY_LEADERBOARD_MENU_TITLE("Leader board"),
	INVENTORY_CLOSE_BUTTON_NAME("&c&lClose"),
	INVENTORY_BACK_BUTTON_NAME("&c&lBack"),
	INVENTORY_LEAVE_GAME_NAME("&c&lLeave Game"),
	INVENTORY_LEAVE_GAME_LORE("&6Right or left click this item to leave the lobby."),
	INVENTORY_OPEN_MENU_NAME("&6&lOpen Menu"),
	INVENTORY_OPEN_MENU_LORE("&fRight or left click this item to open the cod menu."),
	INVENTORY_VOTE_MAP_ONE_NAME("&6&lVote for Map 1"),
	INVENTORY_VOTE_MAP_ONE_LORE("&fRight or left click this item to vote for map 1."),
	INVENTORY_VOTE_MAP_TWO_NAME("&6&lVote for Map 2"),
	INVENTORY_VOTE_MAP_TWO_LORE("&fRight or left click this item to vote for map 2."),
	INVENTORY_JOIN_GAME_NAME("&a&lFind Match"),
	INVENTORY_JOIN_GAME_LORE("&6&lUse the matchmaker to find a match!"),
	INVENTORY_CREATE_A_CLASS_NAME("&4&lCreate-a-Class"),
	INVENTORY_CREATE_A_CLASS_LORE("&6Create custom loadouts to use in game!"),
	INVENTORY_SCORESTREAKS_NAME("&c&lKill streaks"),
	INVENTORY_SCORESTREAKS_LORE("&6Choose which kill streaks you want to use!"),
	INVENTORY_PRESTIGE_NAME("&6&lPrestige Your Account!"),
	INVENTORY_PRESTIGE_LORE("&6Prestige your account and unlock special awards!"),
	INVENTORY_ASSIGNMENTS_NAME("&3&lAssignments"),
	INVENTORY_ASSIGNMENTS_LORE("&6Complete assignments to earn special rewards!"),
	INVENTORY_RECORD_NAME("&9&lCombat Record"),
	INVENTORY_RECORD_LORE("&6Shows your amount of kills & deaths, as well as other statistics."),
	INVENTORY_BOARDS_NAME("&2&lLeader board"),
	INVENTORY_BOARDS_LORE("&6Compare your stats to everyone else!"),
	INVENTORY_SHOP_NAME("&a&lShop"),
	INVENTORY_SHOP_LORE("&6Buy items here at the shop!"),
	INVENTORY_GUN_SHOP_NAME("&9&lGun Shop"),
	INVENTORY_GUN_SHOP_LORE("&6Buy guys you have unlocked here!"),
	INVENTORY_GRENADE_SHOP_NAME("&6Grenade Shop"),
	INVENTORY_GRENADE_SHOP_LORE("&6Buy grenades you have unlocked here!"),
	INVENTORY_PERK_SHOP_NAME("&aPerk Shop"),
	INVENTORY_PERK_SHOP_LORE("&6Buy perks you have unlocked here!"),
	INVENTORY_LOADOUT_HEADER_LORE("&6Edit the class to the right."),
	INVENTORY_PRIMARY_WEAPON_NAME("&6Primary Weapon&f: &r&f{gun}"),
	INVENTORY_PRIMARY_WEAPON_LORE("&6This should be your go-to gun during matches."),
	INVENTORY_SECONDARY_WEAPON_NAME("&6Secondary Weapon&f: &r&f{gun}"),
	INVENTORY_SECONDARY_WEAPON_LORE("&6This should be used as a backup weapon."),
	INVENTORY_LETHAL_WEAPON_NAME("&6Lethal Grenade&f: &r&f{gun}"),
	INVENTORY_LETHAL_WEAPON_LORE("&6Use this during games to throw at and kill players!"),
	INVENTORY_TACTICAL_WEAPON_NAME("&6Tactical Weapon&f: &r&f{gun}"),
	INVENTORY_TACTICAL_WEAPON_LORE("&6Use this grenade to gain an advantage during matches!"),
	INVENTORY_PERK_NAME("&6Perk {i}&f: &r&f{perk}"),
	SHOP_COST("&6Cost"),
	PERK_SLOT("&6Slot"),
	INVENTORY_SELECT_CLASS_TITLE("&6Select Class"),
	INVENTORY_SELECT_CLASS_PRIMARY("&6Primary"),
	INVENTORY_SELECT_CLASS_SECONDARY("&6Secondary"),
	INVENTORY_SELECT_CLASS_LETHAL("&6Lethal"),
	INVENTORY_SELECT_CLASS_TACTICAL("&6Tactical"),
	INVENTORY_SELECT_CLASS_PERK("&6Perk"),
	ERROR_CAN_NOT_CHANGE_CLASS("&cYou can not select a class while not in a game!"),
	LEADERBOARD_PLAYER_ENTRY("&f&lPlayer: {name}"),
	LEADERBOARD_POSITION("&f&lPosition: {score}"),
	LEADERBOARD_SCORE("&6&lScore: {score}"),
	LEADERBOARD_KILLS("&a&lKills: {score}"),
	LEADERBOARD_DEATHS("&c&lDeaths: {score}"),
	LEADERBOARD_KD("&5&lKDR: {score}"),
	ERROR_DEFAULT_WEAPONS_GUNS_NOT_SET("&cMake sure that you have at least one primary, one secondary, one lethal, and one tactical weapon created!"),
	PUT_IN_QUEUE("&7Put in matchmaker queue . . ."),
	ERROR_CAN_NOT_SAVE_KILL_STREAKS("&cError saving kill streaks!"),
	ERROR_SELECTING_CLASS("&cCould not select a class!"),
	CHANGED_CLASS_ONE_MAN_ARMY("&fYour class will change in 10 seconds if you do not move!"),
	CHANGED_CLASS_MESSAGE("&fYour class will change when you next respawn."),
	VOTE_REGISTERED("&aSuccessfully cast vote!"),
	KILL_STREAK_NAME("&cChange Kill Streak {number}"),
	KILL_STREAK_LORE("&eClick here to select your streak for this spot."),
	SELECT_STREAK_INVENTORY_NAME("&c&lSelect Kill Streak"),
	CHANGE_STREAK_SUCCESS("&aSuccessfully changed kill streak!"),
	VOTE_MAP_NAME("&6Map: &f{map}"),
	VOTE_MAP_MODE("&6Mode: &f{mode}"),
	SET_LEVEL_SUCCESS("&aSuccessfully changed &f{player}&a's level to &f{level}&a!"),
	ERROR_NOT_HIGH_ENOUGH_LEVEL("&cYou can't prestige because you aren't a high enough level!"),
	ALREADY_HIGHEST_PRESTIGE("&cYou can't prestige because you are already the max prestige level!");


	private String message;

	Lang(String msg) {
		message = msg;
	}

	public static void load() {
		for (Lang m : Lang.values()) {
			if (LangFile.getData().contains(m.toString())) {
//				Main.cs.sendMessage("Loading value \"" + m.toString() + "\" from config");
				m.message = LangFile.getData().getString(m.toString());
			} else {
				LangFile.getData().set(m.toString(), m.message);
			}
		}

		LangFile.saveData();
	}

	public String getMessage() {
		// \u00A7 is section symbol.
		return message.replace("&", "\u00A7");
	}

}