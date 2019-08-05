package com.rhetorical.cod.game;

import com.rhetorical.cod.ComVersion;
import com.rhetorical.cod.Main;
import com.rhetorical.cod.assignments.AssignmentManager;
import com.rhetorical.cod.inventories.InventoryManager;
import com.rhetorical.cod.inventories.ShopManager;
import com.rhetorical.cod.lang.Lang;
import com.rhetorical.cod.lang.LevelNames;
import com.rhetorical.cod.loadouts.Loadout;
import com.rhetorical.cod.loadouts.LoadoutManager;
import com.rhetorical.cod.perks.Perk;
import com.rhetorical.cod.perks.PerkListener;
import com.rhetorical.cod.progression.CreditManager;
import com.rhetorical.cod.progression.ProgressionManager;
import com.rhetorical.cod.progression.RankPerks;
import com.rhetorical.cod.progression.StatHandler;
import com.rhetorical.cod.streaks.KillStreak;
import com.rhetorical.cod.streaks.KillStreakManager;
import com.rhetorical.cod.weapons.CodGun;
import com.rhetorical.cod.weapons.CodWeapon;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameInstance implements Listener {

	private long id;

	private ArrayList<Player> players;
	private CodMap currentMap;
	private int gameTime;
	private int lobbyTime;

	private GameState state;
	private BukkitRunnable gameRunnable;

	private ArrayList<Player> blueTeam = new ArrayList<>();
	private ArrayList<Player> redTeam = new ArrayList<>();

	private int blueTeamScore;
	private int redTeamScore;

	private int hardpointController;

	private boolean forceStarted = false;

	private final int maxScore_TDM,
			maxScore_RSB,
			maxScore_FFA,
			maxScore_DOM,
			maxScore_CTF,
			maxScore_KC,
			maxScore_GUN,
			maxScore_OITC,
			maxScore_DESTROY,
			maxScore_RESCUE,
			maxScore_HARDPOINT;

	private CtfFlag redFlag, blueFlag;

	private DomFlag aFlag, bFlag, cFlag, hardpointFlag;

	// Score management and game information system for FFA (Free for all)
	private HashMap<Player, Integer> ffaPlayerScores = new HashMap<>();
	private HashMap<Player, Object> freeForAllBar = new HashMap<>();

	private Object scoreBar;

	private ScoreboardManager scoreboardManager;

	public HealthManager health;
	private HungerManager hungerManager;

	private HashMap<Player, CodScore> playerScores = new HashMap<>();

	private CodMap[] nextMaps = new CodMap[2];
	private Gamemode[] nextModes = new Gamemode[2];
	private ArrayList[] mapVotes = new ArrayList[2];

	private boolean blueUavActive;
	private boolean redUavActive;

	private boolean blueCounterUavActive;
	private boolean redCounterUavActive;
	private boolean pinkCounterUavActive;

	private boolean blueNukeActive;
	private boolean redNukeActive;
	private Player pinkNukeActive;

	private boolean pastClassChange = true;


	GameInstance(ArrayList<Player> pls, CodMap map) {

		try {
			scoreBar = Bukkit.createBossBar(ChatColor.GRAY + "«" + ChatColor.WHITE + getFancyTime(Main.getPlugin().getConfig().getInt("lobbyTime")) + ChatColor.RESET + "" + ChatColor.GRAY + "»", org.bukkit.boss.BarColor.GREEN, org.bukkit.boss.BarStyle.SEGMENTED_10);
		} catch(NoClassDefFoundError e) {
			System.out.println();
		} catch(Exception ignored) {}

		id = System.currentTimeMillis();

		players = pls;
		currentMap = map;
		Main.getPlugin().reloadConfig();

		updateTimeLeft();

		lobbyTime = Main.getPlugin().getConfig().getInt("lobbyTime");

		if (ComVersion.getPurchased()) {
			maxScore_TDM = Main.getPlugin().getConfig().getInt("maxScore.TDM");
			maxScore_CTF = Main.getPlugin().getConfig().getInt("maxScore.CTF");
			maxScore_DOM = Main.getPlugin().getConfig().getInt("maxScore.DOM");
			maxScore_FFA = Main.getPlugin().getConfig().getInt("maxScore.FFA");
			maxScore_RSB = Main.getPlugin().getConfig().getInt("maxScore.RSB");
			maxScore_KC = Main.getPlugin().getConfig().getInt("maxScore.KC");
			maxScore_GUN = GameManager.gunGameGuns.size();
			maxScore_OITC = Main.getPlugin().getConfig().getInt("maxScore.OITC");
			maxScore_DESTROY = Main.getPlugin().getConfig().getInt("maxScore.DESTROY");
			maxScore_RESCUE = Main.getPlugin().getConfig().getInt("maxScore.RESCUE");
			maxScore_HARDPOINT = Main.getPlugin().getConfig().getInt("maxScore.HARDPOINT");
		} else {
			maxScore_TDM = 75;
			maxScore_RSB = 75;
			maxScore_FFA = 30;
			maxScore_KC = 50;
			maxScore_DOM = 200;
			maxScore_CTF = 3;
			maxScore_OITC = 3;
			maxScore_DESTROY = 4;
			maxScore_RESCUE = 4;
			maxScore_GUN = GameManager.gunGameGuns.size();
			maxScore_HARDPOINT = 75;
		}

		setState(GameState.WAITING);

		health = new HealthManager(pls, Main.getDefaultHealth());
		hungerManager = new HungerManager();

		Bukkit.getServer().getPluginManager().registerEvents(this, Main.getPlugin());

		for (Player p : pls) {
			health.update(p);
		}

		scoreboardManager = new ScoreboardManager(this);

		System.gc();

		Main.getConsole().sendMessage(Main.getPrefix() + ChatColor.GRAY + "Game lobby with id " + getId() + " created with map " + getMap().getName() + " with gamemode " + getGamemode() + ".");
	}

	private void setupNextMaps() {
		clearNextMaps();
		CodMap m1 = GameManager.pickRandomMap();
		CodMap m2 = GameManager.pickRandomMap();

		nextMaps[0] = m1 == null ? currentMap : m1;
		nextMaps[1] = m2 == null ? currentMap : m2;
		mapVotes[0] = new ArrayList<>();
		mapVotes[1] = new ArrayList<>();

		if (nextMaps[0] != null)
			nextModes[0] = nextMaps[0].getRandomGameMode();
		if (nextMaps[1] != null)
			nextModes[1] = nextMaps[1].getRandomGameMode();
	}

	private void clearNextMaps() {
		if (nextMaps[0] != null && nextMaps[0] != currentMap) {
			GameManager.usedMaps.remove(nextMaps[0]);
		}

		if (nextMaps[1] != null && nextMaps[1] != currentMap) {
			GameManager.usedMaps.remove(nextMaps[1]);
		}
	}

	public void addVote(int map, Player p) throws Exception {
		if (map != 1 && map != 0)
			throw new Exception(Main.getPrefix() + "Improper map selected!");

		mapVotes[0].remove(p);
		mapVotes[1].remove(p);

		if (!mapVotes[map].contains(p))
			mapVotes[map].add(p);
	}

	private void reset() {

		redTeamScore = 0;
		blueTeamScore = 0;
		ffaPlayerScores.clear();
		blueTeam.clear();
		redTeam.clear();

		setState(GameState.WAITING);

		changeMap(GameManager.pickRandomMap());

		health = new HealthManager(players, Main.getDefaultHealth());

		for (Player p : players) {
			p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
			health.update(p);
			p.getInventory().clear();
			p.teleport(Main.getLobbyLocation());

			setTeamArmor(p);

			InventoryManager inv = InventoryManager.getInstance();
			p.getInventory().setItem(0, inv.codItem);
			p.getInventory().setItem(8, inv.leaveItem);

			try {
				scoreBar.getClass().getMethod("setTitle", String.class).invoke(scoreBar, ChatColor.GOLD + getMap().getName() + " " + ChatColor.GRAY + "«" + ChatColor.WHITE + getFancyTime(lobbyTime) + ChatColor.RESET + "" + ChatColor.GRAY + "» " + ChatColor.GOLD + getMap().getGamemode().toString());
			}catch(NoClassDefFoundError e) {
				System.out.println();
			} catch(Exception ignored) {}

			getScoreboardManager().clearScoreboards(p);
		}

		playerScores.clear();

		if (players.size() >= Main.getMinPlayers()) {
			startLobbyTimer(lobbyTime);
		}
	}

	public long getId() {
		return id;
	}

	void changeMap(CodMap map) {

		if (map != null) {
			GameManager.usedMaps.remove(getMap());

			currentMap = map;
		}

		map = currentMap;

		map.changeGamemode();

		updateTimeLeft();
	}

	void changeMap(CodMap map, Gamemode mode) {
		if (map == null)
			return;

		GameManager.usedMaps.remove(getMap());

		currentMap = map;
		map.setGamemode(mode);
		updateTimeLeft();
	}

	public void changeGamemode(Gamemode gm) {
		if (getState() != GameState.WAITING && getState() != GameState.STARTING)
			return;

		if (!getMap().getAvailableGamemodes().contains(gm))
			return;

		currentMap.changeGamemode(gm);

		updateTimeLeft();
	}

	boolean addPlayer(Player p) {

		if (p == null)
			return false;

		if (players.size() >= Main.getMaxPlayers())
			return false;

		if (players.contains(p))
			return false;

		if (getState() == GameState.STOPPING)
			return false;

		players.add(p);

		new PlayerSnapshot(p);
		int level = ProgressionManager.getInstance().getLevel(p);
		String prestige = ProgressionManager.getInstance().getPrestigeLevel(p) > 0 ? ChatColor.WHITE + "[" + ChatColor.GREEN + ProgressionManager.getInstance().getPrestigeLevel(p) + ChatColor.WHITE + "]-" : "";
		String levelName = LevelNames.getInstance().getLevelName(level);
		levelName = !levelName.equals("") ? "[" + levelName + "] " : "";
		p.setPlayerListName(ChatColor.WHITE + levelName + prestige + "[" +
				level + "] " + ChatColor.YELLOW + p.getDisplayName());

		health.addPlayer(p);
		hungerManager.addPlayer(p);

		ProgressionManager.getInstance().update(p);

		p.getInventory().clear();

		KillStreakManager.getInstance().loadStreaks(p);

		p.setGameMode(GameMode.SURVIVAL);
		p.setHealth(20D);
		p.setFoodLevel(20);
		ProgressionManager.getInstance().update(p);

		List<PotionEffect> peTypes = new ArrayList<>(p.getActivePotionEffects());
		for (PotionEffect pe : peTypes) {
			p.removePotionEffect(pe.getType());
		}

		p.teleport(Main.getLobbyLocation());

		playerScores.put(p, new CodScore(p));

		try {
//			scoreBar.addPlayer(p);
			scoreBar.getClass().getMethod("addPlayer", Player.class).invoke(scoreBar, p);

		} catch(NoClassDefFoundError e) {
			System.out.println();
		}catch(Exception ignored) {}

		if (getState() == GameState.IN_GAME) {

			getScoreboardManager().setupGameBoard(p, getFancyTime(gameTime));

			assignTeams();

			if (getGamemode() == Gamemode.OITC) {
				ffaPlayerScores.put(p, maxScore_OITC);
			}

			if (getGamemode() != Gamemode.DESTROY && getGamemode() != Gamemode.RESCUE) {

				Location spawn;
				if (isOnRedTeam(p)) {
					spawn = currentMap.getRedSpawn();
				} else if (isOnBlueTeam(p)) {
					spawn = currentMap.getBlueSpawn();
				} else {
					spawn = currentMap.getPinkSpawn();
				}

				spawnCodPlayer(p, spawn);
			} else {
				p.setGameMode(GameMode.SPECTATOR);
				isAlive.put(p, false);
				if (isOnRedTeam(p)) {
					if(redTeam.size() > 1) {
						p.setSpectatorTarget(redTeam.get(0));
					}
				} else if (isOnBlueTeam(p)) {
					if(blueTeam.size() > 1) {
						p.setSpectatorTarget(redTeam.get(0));
					}
				}
			}
		} else {
			setTeamArmor(p);
			p.getInventory().setItem(0, InventoryManager.getInstance().codItem);
			p.getInventory().setItem(8, InventoryManager.getInstance().leaveItem);

			getScoreboardManager().setupLobbyBoard(p, getFancyTime(lobbyTime));
		}

		if ((players.size() >= Main.getMinPlayers()) && getState() == GameState.WAITING) {
			startLobbyTimer(lobbyTime);
			setState(GameState.STARTING);
		}

		for (Player pp : players) {
			Main.sendMessage(pp, Main.getPrefix() + Lang.PLAYER_JOINED_LOBBY.getMessage().replace("{player}", p.getDisplayName()), Main.getLang());
		}

		return true;
	}

	private void addBluePoint() {
		blueTeamScore++;
	}

	private void addRedPoint() {
		redTeamScore++;
	}

	private void addPointForPlayer(Player p) {
		if (!ffaPlayerScores.containsKey(p)) {
			ffaPlayerScores.put(p, 0);
		}

		ffaPlayerScores.put(p, ffaPlayerScores.get(p) + 1);
	}

	private void removePointForPlayer(Player p) {
		if (!ffaPlayerScores.containsKey(p)) {
			ffaPlayerScores.put(p, 0);
			return;
		}

		if (ffaPlayerScores.get(p) <= 0)
			return;


		ffaPlayerScores.put(p, ffaPlayerScores.get(p) - 1);
	}

	public void removePlayer(Player p) {
		if (!players.contains(p))
			return;

		ShopManager.getInstance().checkForNewGuns(p);

//		if (isLegacy)
		p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());


		try {
			List pls = (List) scoreBar.getClass().getMethod("getPlayers").invoke(scoreBar);
			if (pls.contains(p)) {
				scoreBar.getClass().getMethod("removePlayer", Player.class).invoke(scoreBar, p);
			}
		} catch(NoClassDefFoundError ignored) {
		}catch(Exception ignored) {
		}

		if (freeForAllBar.containsKey(p)) {
			if (freeForAllBar.get(p) == null) {
				freeForAllBar.remove(p);
			}

			try {
				Object bar = freeForAllBar.get(p);

				bar.getClass().getMethod("removeAll").invoke(bar);

				freeForAllBar.remove(p);
			} catch(NoClassDefFoundError e) {
				System.out.println();
			}catch(Exception ignored) {}
		}

		health.removePlayer(p);

		playerScores.remove(p);

		players.remove(p);
		hungerManager.removePlayer(p);
		ffaPlayerScores.remove(p);

		ProgressionManager.getInstance().saveData(p);

		AssignmentManager.getInstance().save(p);

		if (players.size() == 0) {
			despawnCtfFlags();
			despawnDomFlags();
			despawnHardpointFlag();
			GameManager.removeInstance(this);
		} else if ((redTeam.size() > 0 && blueTeam.size() == 0)
				|| (blueTeam.size() > 0 && redTeam.size() == 0)
				|| getPlayers().size() == 1) {
			if (getState() == GameState.IN_GAME) {
				if (!Main.isDisabling())
					stopGame();
			}
		}

		if (PlayerSnapshot.hasSnapshot(p)) {
			PlayerSnapshot.apply(p);
			System.gc();
		} else {
			p.setPlayerListName(p.getDisplayName());
			p.setFoodLevel(20);
			p.setLevel(0);
			p.setExp(0f);
			p.setHealth(20d);
		}

		getScoreboardManager().clearScoreboards(p);

		try {
			p.getClass().getMethod("setPlayerListHeader", String.class).invoke(p, "");
			p.getClass().getMethod("setPlayerListFooter", String.class).invoke(p, "");
		} catch(NoSuchMethodException ignored) {} catch(Exception ignored) {}

		System.gc();
	}

	private void startGame() {

		forceStarted = false;

		blueTeam.clear();
		redTeam.clear();
		ffaPlayerScores.clear();

		assignTeams();
		playerScores.clear();

		for (Player p : players) {

			LoadoutManager.getInstance().getActiveLoadouts().remove(p);

			KillStreakManager.getInstance().reset(p);

			playerScores.put(p, new CodScore(p));

			if (getGamemode() != Gamemode.FFA && getGamemode() != Gamemode.OITC && getGamemode() != Gamemode.GUN) {
				if (blueTeam.contains(p)) {
					spawnCodPlayer(p, currentMap.getBlueSpawn());
				} else if (redTeam.contains(p)) {
					spawnCodPlayer(p, currentMap.getRedSpawn());
				} else {
					assignTeams();
				}
			} else {
				if (getGamemode() != Gamemode.OITC) {
					ffaPlayerScores.put(p, 0);
				} else {
					ffaPlayerScores.put(p, maxScore_OITC);
				}
				spawnCodPlayer(p, currentMap.getPinkSpawn());
			}
		}

		if (getGamemode() == Gamemode.DESTROY || getGamemode() == Gamemode.RESCUE) {
			for (Player p : players) {
				isAlive.put(p, true);
			}
		}

		if (getGamemode() == Gamemode.CTF)
			spawnCtfFlags();

		startGameTimer(gameTime, false);
		setState(GameState.IN_GAME);
	}

	private void dropFlag(Item flag, Location location) {
		location.getWorld().dropItem(location, flag.getItemStack());
	}

	private void spawnCtfFlags() {
		if (getGamemode() != Gamemode.CTF)
			return;

		despawnCtfFlags();

		redFlag = new CtfFlag(this, Team.RED, Lang.FLAG_RED, getMap().getRedFlagSpawn());
		blueFlag = new CtfFlag(this, Team.BLUE, Lang.FLAG_BLUE, getMap().getBlueFlagSpawn());

		redFlag.setOtherFlag(blueFlag);
		blueFlag.setOtherFlag(redFlag);

		redFlag.spawn();
		blueFlag.spawn();
	}

	private void spawnCodPlayer(Player p, Location L) {
		p.teleport(L);
		p.getInventory().clear();
		p.setGameMode(GameMode.ADVENTURE);
		p.setHealth(20d);
		p.setFoodLevel(20);
		Loadout loadout = LoadoutManager.getInstance().getActiveLoadout(p);

		setTeamArmor(p);

		if (getGamemode() == Gamemode.RSB) {

			CodGun primary = LoadoutManager.getInstance().getRandomPrimary();
			CodGun secondary = LoadoutManager.getInstance().getRandomSecondary();
			CodWeapon lethal = LoadoutManager.getInstance().getRandomLethal();
			CodWeapon tactical = LoadoutManager.getInstance().getRandomTactical();

			ItemStack primaryAmmo = primary.getAmmo();
			primaryAmmo.setAmount(primary.getAmmoCount());

			ItemStack secondaryAmmo = secondary.getAmmo();
			secondaryAmmo.setAmount(secondary.getAmmoCount());


			p.getInventory().setItem(0, LoadoutManager.getInstance().knife);
			if (!primary.equals(LoadoutManager.getInstance().blankPrimary)) {
				p.getInventory().setItem(1, primary.getGunItem());
				p.getInventory().setItem(28, primaryAmmo);
			}

			if (!secondary.equals(LoadoutManager.getInstance().blankSecondary)) {
				p.getInventory().setItem(2, secondary.getGunItem());
				p.getInventory().setItem(29, secondaryAmmo);
			}

			if (Math.random() > 0.5 && !lethal.equals(LoadoutManager.getInstance().blankLethal)) {
				p.getInventory().setItem(3, lethal.getWeaponItem());
			}

			if (Math.random() > 0.5 && !tactical.equals(LoadoutManager.getInstance().blankTactical)) {
				p.getInventory().setItem(4, tactical.getWeaponItem());
			}

		} else if (getGamemode() == Gamemode.DOM
				|| getGamemode() == Gamemode.CTF
				|| getGamemode() == Gamemode.KC
				|| getGamemode() == Gamemode.TDM
				|| getGamemode() == Gamemode.FFA
				|| getGamemode() == Gamemode.INFECT
				|| getGamemode() == Gamemode.DESTROY
				|| getGamemode() == Gamemode.RESCUE
				|| getGamemode() == Gamemode.HARDPOINT) {

			p.getInventory().setItem(0, LoadoutManager.getInstance().knife);

			if (getGamemode() != Gamemode.INFECT || (getGamemode() == Gamemode.INFECT && blueTeam.contains(p))) {
				LoadoutManager.getInstance().giveLoadout(p, loadout);
			}

			if (getGamemode() == Gamemode.INFECT && redTeam.contains(p)) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * gameTime, 1));
			}

		} else if (getGamemode() == Gamemode.OITC) {
			p.getInventory().setItem(0, LoadoutManager.getInstance().knife);
			p.getInventory().setItem(1, GameManager.oitcGun.getGunItem());
			ItemStack ammo = GameManager.oitcGun.getAmmo();
			ammo.setAmount(1);
			p.getInventory().setItem(8, ammo);
		} else if (getGamemode() == Gamemode.GUN) {
			if(!ffaPlayerScores.containsKey(p)) {
				ffaPlayerScores.put(p, 0);
			}
			p.getInventory().setItem(0, LoadoutManager.getInstance().knife);
			CodGun gun = GameManager.gunGameGuns.get(ffaPlayerScores.get(p));
			ItemStack gunItem = gun.getGunItem();
			ItemStack ammo = gun.getAmmo();
			ammo.setAmount(gun.getAmmoCount());

			p.getInventory().setItem(1, gunItem);
			p.getInventory().setItem(28, ammo);
		}

		p.getInventory().setItem(32, InventoryManager.getInstance().selectClass);
		p.getInventory().setItem(35, InventoryManager.getInstance().leaveItem);

		KillStreakManager.getInstance().streaksAfterDeath(p);
	}

	private void dropDogTag(Player p) {
		if (!GameManager.isInMatch(p))
			return;

		if (!players.contains(p))
			return;

		if (!isOnRedTeam(p) && !isOnBlueTeam(p)) {
			assignTeams();
			return;
		}

		if (getGamemode() == Gamemode.RESCUE || getGamemode() == Gamemode.KC) {

			ItemStack dogtag = new ItemStack(Material.NAME_TAG);

			ItemMeta meta = dogtag.getItemMeta();

			ChatColor teamColor;

			if (blueTeam.contains(p))
				teamColor = ChatColor.BLUE;
			else
				teamColor = ChatColor.RED;


			meta.setDisplayName(Lang.PLAYER_DOG_TAG_NAME.getMessage().replace("{team-color}", teamColor + "").replace("{player}", p.getName()));

			List<String> lore = new ArrayList<>();

			if (blueTeam.contains(p))
				lore.add(p.getUniqueId().toString());
			else
				lore.add(p.getUniqueId().toString());

			meta.setLore(lore);
			dogtag.setItemMeta(meta);



			p.getWorld().dropItem(p.getLocation(), dogtag).setCustomNameVisible(true);
		}
	}

	private void assignTeams() {

		if (getGamemode() != Gamemode.FFA && getGamemode() != Gamemode.OITC && getGamemode() != Gamemode.GUN) {
			for (Player p : players) {
				if (blueTeam.contains(p) || redTeam.contains(p))
					continue;

				ChatColor tColor;
				String team;

				if (redTeam.size() >= blueTeam.size()) {
					blueTeam.add(p);
					tColor = ChatColor.BLUE;
					team = "blue";
				} else {
					redTeam.add(p);
					tColor = ChatColor.RED;
					team = "red";
				}

				Main.sendMessage(p, Main.getPrefix() + Lang.ASSIGNED_TO_TEAM.getMessage().replace("{team-color}", tColor + "").replace("{team}", team), Main.getLang());
			}
		} else if (getGamemode() == Gamemode.INFECT) {
			Collections.shuffle(players);
			for (Player p : players) {
				if (redTeam.size() > 0) {
					blueTeam.add(p);
					continue;
				}

				redTeam.add(p);
			}
		} else {
			for (Player p : players) {
				if (ffaPlayerScores.containsKey(p))
					continue;

				Main.sendMessage(p, Main.getPrefix() + Lang.ASSIGNED_TO_TEAM.getMessage().replace("{team-color}", ChatColor.LIGHT_PURPLE + "").replace("{team}", "pink"), Main.getLang());
			}
		}

	}

	private void stopGame() {

		setState(GameState.STOPPING);

		CodScore highestScore = null;
		CodScore highestKD = null;

		for (Player p : getPlayers()) {
			if (p.getGameMode().equals(GameMode.SPECTATOR)) {
				Location spawnPoint = isOnPinkTeam(p) ? currentMap.getPinkSpawn() : isOnBlueTeam(p) ? currentMap.getBlueSpawn() : currentMap.getRedSpawn();
				spawnCodPlayer(p, spawnPoint);
			}
			p.setGameMode(GameMode.ADVENTURE);
		}

		for (CodScore score : playerScores.values()) {
			if (highestScore == null || score.getScore() > highestScore.getScore()) {
				highestScore = score;
			}

			if (highestKD == null || score.getRatio() > highestKD.getRatio()) {
				highestKD = score;
			}
		}

		if (!Main.getRewardHighestScore().equalsIgnoreCase("none") && highestScore != null) {
			String cmd = Main.getRewardHighestScore().replace("{PLAYER}", highestScore.getOwner().getName());
			Bukkit.getServer().dispatchCommand(Main.getConsole(), cmd);
		}

		if (!Main.getRewardHighestKD().equalsIgnoreCase("none") && highestKD != null) {
			String cmd = Main.getRewardHighestKD().replace("{PLAYER}", highestScore.getOwner().getName());
			Bukkit.getServer().dispatchCommand(Main.getConsole(), cmd);
		}

		for (Player p : getPlayers()) {

			boolean won = false;

			p.removePotionEffect(PotionEffectType.SPEED);

			if (getWinningTeam().equalsIgnoreCase("red") && redTeam.contains(p)) {
				won = true;
			} else if (getWinningTeam().equalsIgnoreCase("blue") && blueTeam.contains(p)) {
				won = true;
			} else if (getWinningTeam().equals(p.getDisplayName())) {
				won = true;
			}

			AssignmentManager.getInstance().updateAssignments(p, 0, getGamemode(), won);

			AssignmentManager.getInstance().save(p);

			ShopManager.getInstance().checkForNewGuns(p);

			if (freeForAllBar.containsKey(p)) {
				try {
					freeForAllBar.get(p).getClass().getMethod("removeAll").invoke(freeForAllBar.get(p));
				}catch(NoClassDefFoundError e) {
					System.out.println();
				} catch(Exception ignored) {}
			}

			try {
				List players = (List) scoreBar.getClass().getMethod("getPlayers").invoke(scoreBar);
				if (!players.contains(p)) {
					scoreBar.getClass().getMethod("addPlayer", Player.class).invoke(scoreBar, p);
				}
			} catch(NoClassDefFoundError e) {
				System.out.println();
			}catch (Exception ignored) {}

			p.getInventory().clear();

			ProgressionManager.getInstance().saveData(p);

			StatHandler.saveStatData();
		}

		if (getGamemode() == Gamemode.DOM)
			despawnDomFlags();

		if (getGamemode() == Gamemode.HARDPOINT)
			despawnHardpointFlag();

		if (getGamemode() == Gamemode.CTF)
			despawnCtfFlags();

		GameInstance game = this;

		BukkitRunnable br = new BukkitRunnable() {

			int t = 10;

			public void run() {
				for (Player p : game.players) {
					for (int i = 0; i < 100; i++) {
						Main.sendMessage(p, "", Main.getLang());
					}

					String teamFormat = "";

					if (currentMap.getGamemode() != Gamemode.FFA && currentMap.getGamemode() != Gamemode.GUN && currentMap.getGamemode() != Gamemode.OITC) {
						if (getWinningTeam().equalsIgnoreCase("red")) {
							teamFormat = ChatColor.RED + "RED";
						} else if (getWinningTeam().equalsIgnoreCase("blue")) {
							teamFormat = ChatColor.BLUE + "BLUE";
						} else if (getWinningTeam().equalsIgnoreCase("nobody") || getWinningTeam().equalsIgnoreCase("tie")) {
							Main.sendMessage(p, Main.getPrefix() + ChatColor.GRAY + Lang.NOBODY_WON_GAME.getMessage(), Main.getLang());
							Main.sendMessage(p, Main.getPrefix() + Lang.RETURNING_TO_LOBBY.getMessage().replace("{time}", t + ""), Main.getLang());
							playerScores.computeIfAbsent(p, k -> new CodScore(p));
							CodScore score = playerScores.get(p);

							float kd = ((float) score.getKills() / (float) score.getDeaths());

							if (Float.isNaN(kd) || Float.isInfinite(kd)) {
								kd = score.getKills();
							}

							String msg = Lang.END_GAME_KILLS_DEATHS.getMessage();
							msg = msg.replace("{kills}", score.getKills() + "");
							msg = msg.replace("{deaths}", score.getDeaths() + "");
							msg = msg.replace("{kd}", kd + "");
							Main.sendMessage(p, msg, Main.getLang());
							continue;
						}

						Main.sendMessage(p, Main.getPrefix() + Lang.SOMEBODY_WON_GAME.getMessage().replace("{team}", teamFormat), Main.getLang());
						Main.sendMessage(p, Main.getPrefix() + Lang.RETURNING_TO_LOBBY.getMessage().replace("{time}", t + ""), Main.getLang());
						CodScore score = playerScores.get(p);

						float kd = ((float) score.getKills() / (float) score.getDeaths());

						if (Float.isNaN(kd) || Float.isInfinite(kd)) {
							kd = score.getKills();
						}

						String msg = Lang.END_GAME_KILLS_DEATHS.getMessage();
						msg = msg.replace("{kills}", score.getKills() + "");
						msg = msg.replace("{deaths}", score.getDeaths() + "");
						msg = msg.replace("{kd}", kd + "");
						Main.sendMessage(p, msg, Main.getLang());
					} else {
						Main.sendMessage(p, Main.getPrefix() + Lang.SOMEONE_WON_GAME.getMessage().replace("{player}", getWinningTeam()), Main.getLang());
						Main.sendMessage(p, Main.getPrefix() + Lang.RETURNING_TO_LOBBY.getMessage().replace("{time}", t + ""), Main.getLang());
						CodScore score = playerScores.get(p);
						float kd = ((float) score.getKills() / (float) score.getDeaths());

						if (Float.isNaN(kd) || Float.isInfinite(kd)) {
							kd = score.getKills();
						}

						String msg = Lang.END_GAME_KILLS_DEATHS.getMessage();
						msg = msg.replace("{kills}", score.getKills() + "");
						msg = msg.replace("{deaths}", score.getDeaths() + "");
						msg = msg.replace("{kd}", kd + "");
						Main.sendMessage(p, msg, Main.getLang());
					}
				}

				t--;

				if (t <= 0) {
					game.reset();
					cancel();
				}

			}
		};

		br.runTaskTimer(Main.getPlugin(), 0L, 20L);
	}

	private void startLobbyTimer(int time) {

		forceStarted = false;

		setState(GameState.STARTING);

		try {
			scoreBar.getClass().getMethod("removeAll").invoke(scoreBar);
		} catch(NoClassDefFoundError e) {
			System.out.println();
		}catch(Exception ignored) {}
		for (Player p : players) {
			try {
				scoreBar.getClass().getMethod("addPlayer", Player.class).invoke(scoreBar, p);
			} catch(Exception ignored) {}
			p.getInventory().setItem(0, InventoryManager.getInstance().codItem);
			p.getInventory().setItem(8, InventoryManager.getInstance().leaveItem);

			getScoreboardManager().clearScoreboards(p);
			getScoreboardManager().setupLobbyBoard(p, getFancyTime(lobbyTime));
		}

		GameInstance game = this;

		setupNextMaps();

		for (Player p : players) {
			setTeamArmor(p);
		}

		changeMap(nextMaps[0], nextModes[0]);


		BukkitRunnable br = new BukkitRunnable() {

			int t = time;

			int lobbyTime = time;

			@Override
			public void run() {

				String counter = getFancyTime(t);

				try {
					scoreBar.getClass().getMethod("setTitle", String.class).invoke(scoreBar, ChatColor.GOLD + getMap().getName() + " " + ChatColor.GRAY + "«" + ChatColor.WHITE + counter + ChatColor.RESET + "" + ChatColor.GRAY + "» " + ChatColor.GOLD + getMap().getGamemode().toString());
				} catch(Exception|NoClassDefFoundError ignored) {}

				double progress = (((double) t) / ((double) lobbyTime));

				try {
					scoreBar.getClass().getMethod("setProgress", Double.class).invoke(scoreBar, progress);
				} catch(Exception|NoClassDefFoundError ignored) {}

				if (t == 20) {
					CodMap[] maps = nextMaps;
					if (mapVotes[0].size() > mapVotes[1].size()) {
						changeMap(maps[0], nextModes[0]);
					} else if (mapVotes[1].size() > mapVotes[0].size()) {
						changeMap(maps[1], nextModes[1]);
					} else {
						int index = (new Random()).nextInt(2);
						changeMap(maps[index], nextModes[index]);
					}
					clearNextMaps();

					for (Player p : game.players) {
						Main.sendMessage(p, Main.getPrefix() + Lang.MAP_VOTING_NEXT_MAP.getMessage().replace("{map}", game.currentMap.getName()), Main.getLang());
					}
				}

				for (Player p : game.getPlayers()) {
					getScoreboardManager().updateLobbyBoard(p, getFancyTime(t));
				}

				if (t % 30 == 0 || (t % 10 == 0 && t < 30) || (t % 5 == 0 && t < 15)) {
					for (Player p : game.players) {
						Main.sendMessage(p, Main.getPrefix() + Lang.GAME_STARTING_MESSAGE.getMessage().replace("{time}", getFancyTime(t)), Main.getLang());
						Main.sendMessage(p, Main.getPrefix() + Lang.GAME_STARTING_MAP_MESSAGE.getMessage().replace("{map}", getMap().getName()).replace("{mode}", getMap().getGamemode().toString()) ,Main.getLang());
						if (t > 20) {
							Main.sendMessage(p, Main.getPrefix() + Lang.MAP_VOTING_HEADER.getMessage(), Main.getLang());
							Main.sendMessage(p, Main.getPrefix() + ChatColor.GRAY + "===============", Main.getLang());
							Main.sendMessage(p, Main.getPrefix() + Lang.MAP_VOTING_NAMES.getMessage().replace("{1}", nextMaps[0].getName() + " - " + nextModes[0].toString()).replace("{2}", nextMaps[1].getName() + " - " + nextModes	[1].toString()), Main.getLang());
							Main.sendMessage(p, Main.getPrefix() + Lang.MAP_VOTING_VOTES.getMessage().replace("{1}", mapVotes[0].size() + "").replace("{2}", mapVotes[1].size() + ""), Main.getLang());
						}
					}
				}

				for (Player p : game.players) {
					int level = ProgressionManager.getInstance().getLevel(p);
					String prestige = ProgressionManager.getInstance().getPrestigeLevel(p) > 0 ? ChatColor.WHITE + "[" + ChatColor.GREEN + ProgressionManager.getInstance().getPrestigeLevel(p) + ChatColor.WHITE + "]-" : "";
					String levelName = LevelNames.getInstance().getLevelName(level);
					levelName = !levelName.equals("") ? "[" + levelName + "] " : "";
					p.setPlayerListName(ChatColor.WHITE + levelName + prestige + "[" +
							level + "] " + ChatColor.YELLOW + p.getDisplayName());
					try {
						p.getClass().getMethod("setPlayerListHeader", String.class).invoke(p, Lang.LOBBY_HEADER.getMessage());
						p.getClass().getMethod("setPlayerListFooter", String.class).invoke(p, Lang.LOBBY_FOOTER.getMessage().replace("{time}", getFancyTime(t)));
					} catch(Exception ignored) {}

					if (t > 20) {
						if (p.getInventory().getItem(3) == null || !p.getInventory().getItem(3).getType().equals(InventoryManager.getInstance().voteItemA.getType())) {
							ItemStack voteItem = InventoryManager.getInstance().voteItemA;
							ItemMeta voteMeta = voteItem.getItemMeta();
							List<String> lore = new ArrayList<>();
							lore.add(Lang.VOTE_MAP_NAME.getMessage().replace("{map}", nextMaps[0].getName()));
							lore.add(Lang.VOTE_MAP_MODE.getMessage().replace("{mode}", nextModes[0].toString()));
							voteMeta.setLore(lore);
							voteItem.setItemMeta(voteMeta);
							p.getInventory().setItem(3, voteItem);
						}

						if (p.getInventory().getItem(4) == null || !p.getInventory().getItem(4).getType().equals(InventoryManager.getInstance().voteItemB.getType())) {
							ItemStack voteItem = InventoryManager.getInstance().voteItemB;
							ItemMeta voteMeta = voteItem.getItemMeta();
							List<String> lore = new ArrayList<>();
							lore.add(Lang.VOTE_MAP_NAME.getMessage().replace("{map}", nextMaps[1].getName()));
							lore.add(Lang.VOTE_MAP_MODE.getMessage().replace("{mode}", nextModes[1].toString()));
							voteMeta.setLore(lore);
							voteItem.setItemMeta(voteMeta);
							p.getInventory().setItem(4, voteItem);
						}
					} else {
						p.getInventory().setItem(3, new ItemStack(Material.AIR));
						p.getInventory().setItem(4, new ItemStack(Material.AIR));
					}
				}

				if (t == 0 || forceStarted) {

					for (Player p : getPlayers()) {
						if (t == 0) {
							Main.sendMessage(p, Main.getPrefix() + Lang.GAME_STARTING.getMessage(), Main.getLang());
						}
					}

					clearNextMaps();

					startGame();

					cancel();
				}

				t--;
			}
		};

		br.runTaskTimer(Main.getPlugin(), 0L, 20L);
	}

	private void startGameTimer(int time, boolean newRound) {

		pastClassChange = false;

		if (!newRound) {
			setState(GameState.IN_GAME);

			try {
//			scoreBar.removeAll();
				scoreBar.getClass().getMethod("removeAll").invoke(scoreBar);
			} catch(NoClassDefFoundError e) {
				System.out.println();
			}catch(Exception ignored) {}

			for (Player p : players) {
				if (!currentMap.getGamemode().equals(Gamemode.FFA) && !currentMap.getGamemode().equals(Gamemode.OITC) && !currentMap.getGamemode().equals(Gamemode.GUN)) {
					try {
						scoreBar.getClass().getMethod("addPlayer", Player.class).invoke(scoreBar, p);
					}catch(NoClassDefFoundError e) {
						System.out.println();
					} catch(Exception ignored) {}
				} else {

					try {
						Object bar = Bukkit.createBossBar(ChatColor.GRAY + "«" + ChatColor.WHITE + getFancyTime(gameTime) + ChatColor.RESET + ChatColor.WHITE + "»", org.bukkit.boss.BarColor.GREEN, org.bukkit.boss.BarStyle.SEGMENTED_10);
						freeForAllBar.put(p, bar);
						freeForAllBar.get(p).getClass().getMethod("addPlayer", Player.class).invoke(freeForAllBar.get(p), p);

					} catch(NoClassDefFoundError e) {
						System.out.println();
					}catch(Exception ignored) {}

					if (getGamemode() == Gamemode.OITC) {
						ffaPlayerScores.put(p, maxScore_OITC);
					} else {
						ffaPlayerScores.put(p, 0);
					}
				}

				getScoreboardManager().setupGameBoard(p, getFancyTime(gameTime));
			}

			if (getGamemode().equals(Gamemode.DOM)) {
				spawnDomFlags();
			}
		} else {
			for (Player p : players) {
				if (isOnBlueTeam(p)) {
					spawnCodPlayer(p, getMap().getBlueSpawn());
				} else if (isOnRedTeam(p)) {
					spawnCodPlayer(p, getMap().getRedSpawn());
				} else {
					assignTeams();
				}

			}
		}

		GameInstance game = this;

		gameRunnable = new BukkitRunnable() {

			int t = time;
			@Override
			public void run() {

				if (t == 0) {

					stopGame();

					cancel();
					return;
				}

				if (t % 60 == 0 && getGamemode() == Gamemode.HARDPOINT) {
					updateHardpointFlagLocation();
				}

				if (getState() != GameState.IN_GAME) {
					this.cancel();
					return;
				}

				if (time - t == 10) {
					pastClassChange = true;
				}

				t--;

				String counter = getFancyTime(t);

				if (getGamemode() == Gamemode.DOM) {
					game.checkDomFlags();
				}

				if (getGamemode() == Gamemode.HARDPOINT) {
					game.checkHardpointFlag();
				}

				if (getGamemode() == Gamemode.CTF) {
					if (redFlag != null)
						redFlag.checkNearbyPlayers();
					if (blueFlag != null)
						blueFlag.checkNearbyPlayers();
				}

				if (currentMap.getGamemode() == Gamemode.INFECT) {
					blueTeamScore = blueTeam.size();
					redTeamScore = redTeam.size();

					if (blueTeam.size() == 0) {
						endGameByScore(this);
						return;
					}
				}

				if (currentMap.getGamemode() != Gamemode.FFA && currentMap.getGamemode() != Gamemode.OITC && currentMap.getGamemode() != Gamemode.GUN) {
					try {
						//scoreBar.setTitle(ChatColor.RED + "RED: " + redTeamScore + ChatColor.GRAY + " «" + ChatColor.WHITE + counter + ChatColor.RESET + ChatColor.GRAY + "»" + ChatColor.BLUE + " BLU: " + blueTeamScore);
						scoreBar.getClass().getMethod("setTitle", String.class).invoke(scoreBar, ChatColor.RED + "RED: " + redTeamScore + ChatColor.GRAY + " «" + ChatColor.WHITE + counter + ChatColor.RESET + ChatColor.GRAY + "»" + ChatColor.BLUE + " BLU: " + blueTeamScore);
					} catch(NoClassDefFoundError e) {
						System.out.println();
					} catch(Exception ignored) {}
				} else {

					Player highestScorer = Bukkit.getPlayer(getWinningTeam());

					for (Player p : players) {
						if (highestScorer == null) {
							highestScorer = p;
						}

						if (!ffaPlayerScores.containsKey(p)) {
							if (getGamemode() != Gamemode.OITC) {
								ffaPlayerScores.put(p, 0);
							} else {
								ffaPlayerScores.put(p, maxScore_OITC);
							}
						}

						if (!ffaPlayerScores.containsKey(highestScorer)) {
							ffaPlayerScores.put(highestScorer, 0);
						}

						if (highestScorer == p) {
							if (getPlayers().size() > 1) {
								TreeMap<Integer, Player> scores = new TreeMap<>();
								for (Player pl : ffaPlayerScores.keySet()) {
									if (pl == highestScorer)
										continue;
									scores.put(ffaPlayerScores.get(pl), pl);
								}

								highestScorer = scores.lastEntry().getValue();
							}
						}


						double progress = (((double) t) / ((double) gameTime));
						try {
							freeForAllBar.get(p).getClass().getMethod("setTitle", String.class).invoke(freeForAllBar.get(p), ChatColor.GREEN + p.getDisplayName() + ": " + ffaPlayerScores.get(p) + ChatColor.GRAY + " «" + ChatColor.WHITE + counter + ChatColor.RESET + ChatColor.GRAY + "»" + " " + ChatColor.GOLD + highestScorer.getDisplayName() + ": " + ffaPlayerScores.get(highestScorer));
							freeForAllBar.get(p).getClass().getMethod("setProgress", Double.class).invoke(freeForAllBar.get(p), progress);
						} catch(NoClassDefFoundError e) {
							System.out.println();
						}catch(Exception ignored) {}
					}
				}

				double progress = (((double) t) / ((double) gameTime));

				try {
					scoreBar.getClass().getMethod("setProgress", Double.class).invoke(scoreBar, progress);
				} catch(Exception ignored) {}
				game.updateTabList();

				for (Player p : getPlayers()) {
					getScoreboardManager().updateGameScoreBoard(p, getFancyTime(t));
				}

				if (currentMap.getGamemode() == Gamemode.TDM || currentMap.getGamemode() == Gamemode.RSB || currentMap.getGamemode() == Gamemode.DOM || currentMap.getGamemode() == Gamemode.CTF || currentMap.getGamemode() == Gamemode.KC || currentMap.getGamemode() == Gamemode.HARDPOINT) {
					if ((blueTeamScore >= maxScore_TDM || redTeamScore >= maxScore_TDM) && getGamemode().equals(Gamemode.TDM)) {
						endGameByScore(this);
						return;
					} else if ((blueTeamScore >= maxScore_RSB || redTeamScore >= maxScore_RSB) && getGamemode().equals(Gamemode.RSB)) {
						endGameByScore(this);
						return;
					} else if ((blueTeamScore >= maxScore_DOM || redTeamScore >= maxScore_DOM) && getGamemode().equals(Gamemode.DOM)) {
						endGameByScore(this);
						return;
					} else if ((blueTeamScore >= maxScore_CTF || redTeamScore >= maxScore_CTF) && getGamemode().equals(Gamemode.CTF)) {
						endGameByScore(this);
						return;
					} else if ((blueTeamScore >= maxScore_KC || redTeamScore >= maxScore_KC) && getGamemode().equals(Gamemode.KC)) {
						endGameByScore(this);
						return;
					} else if ((blueTeamScore >= maxScore_HARDPOINT || redTeamScore >= maxScore_HARDPOINT) && getGamemode().equals(Gamemode.HARDPOINT)) {
						endGameByScore(this);
						return;
					}
				}

				if (getGamemode() == Gamemode.DESTROY || getGamemode() == Gamemode.RESCUE) {
					if (getAlivePlayers(redTeam) == 0) {
						addBluePoint();

						if (!(blueTeamScore >= maxScore_DESTROY) && !(blueTeamScore >= maxScore_RESCUE)) {
							startNewRound(7, blueTeam);
						}

						for (Player pp : players) {
							isAlive.put(pp, true);
						}
						cancel();
					} else if (getAlivePlayers(blueTeam) == 0) {
						addRedPoint();

						if (!(redTeamScore >= maxScore_DESTROY) && !(redTeamScore >= maxScore_RESCUE)) {
							startNewRound(7, redTeam);
						}

						for (Player pp : players) {
							isAlive.put(pp, true);
						}
						cancel();
					}

					if (blueTeamScore >= maxScore_DESTROY || redTeamScore >= maxScore_DESTROY && getGamemode().equals(Gamemode.DESTROY)) {
						endGameByScore(this);
						cancel();
						return;
					}else if (blueTeamScore >= maxScore_RESCUE || redTeamScore >= maxScore_RESCUE && getGamemode().equals(Gamemode.RESCUE)) {
						endGameByScore(this);
						cancel();
						return;
					}
				}

				if (currentMap.getGamemode().equals(Gamemode.FFA)) {
					for (Player p : players) {
						if (ffaPlayerScores.get(p) >= maxScore_FFA) {
							endGameByScore(this);
							return;
						}
					}
				}

				if(currentMap.getGamemode().equals(Gamemode.OITC)) {
//					for (Player p : players) {
//						if (ffaPlayerScores.get(p) >= maxScore_OITC) {
//							endGameByScore(this);
//							return;
//						}
//					}
					for(Player p : getPlayers()) {
						boolean lastManStanding = true;
						for(Player other : getPlayers()) {
							if (other.equals(p))
								continue;

							if (ffaPlayerScores.get(other) > 0)
								lastManStanding = false;
						}

						if(lastManStanding) {
							endGameByScore(this);
							return;
						}
					}
				}

				if (currentMap.getGamemode().equals(Gamemode.GUN)) {
					for (Player p : players) {
						if (ffaPlayerScores.get(p) >= maxScore_GUN) {
							endGameByScore(this);
							return;
						}
					}
				}

			}

		};
		gameRunnable.runTaskTimer(Main.getPlugin(), 0L, 20L);
	}

	private void startNewRound(int delay, List<Player> prevRWT) {
		for(Player p : players) {
			if (prevRWT != null && !prevRWT.isEmpty()) {
				ChatColor tColor;
				String team;

				if (prevRWT.equals(blueTeam)) {
					tColor = ChatColor.BLUE;
					team = "blue";
                } else {
					tColor = ChatColor.RED;
					team = "red";
                }

				Main.sendTitle(p, Lang.TEAM_WON_ROUND.getMessage().replace("{team-color}", tColor + "").replace("{team}", team), Lang.NEXT_ROUND_STARTING.getMessage().replace("{time}", delay + ""), tColor);
			}
		}

		BukkitRunnable br = new BukkitRunnable() {
			@Override
			public void run() {
				startGameTimer(gameTime, true);
			}
		};

		br.runTaskLater(Main.getPlugin(), 20L * (long) delay);
	}

	private void endGameByScore(BukkitRunnable runnable) {
		stopGame();
		runnable.cancel();
	}

	public void resetScoreBoard() {
		if (getGamemode() != Gamemode.FFA && getGamemode() != Gamemode.GUN && getGamemode() != Gamemode.OITC) {
			try {
				scoreBar = Bukkit.createBossBar(Color.RED + "RED: 0" + "     " + "«" + getFancyTime(gameTime) + "»" + "     " + Color.BLUE + "BLUE: 0", org.bukkit.boss.BarColor.GREEN, org.bukkit.boss.BarStyle.SEGMENTED_10);
			} catch(NoClassDefFoundError e) {
				System.out.println();
			}catch(Exception ignored) {}
		} else {
			try {
				scoreBar = Bukkit.createBossBar(Color.RED + "YOU: 0" + "     " + "«" + getFancyTime(gameTime) + "»" + "     " + Color.BLUE + "1ST: 0", org.bukkit.boss.BarColor.GREEN, org.bukkit.boss.BarStyle.SEGMENTED_10);
			} catch(NoClassDefFoundError e) {
				System.out.println();
			}catch(Exception ignored) {}
		}
	}

	private String getWinningTeam() {

		if (getGamemode().equals(Gamemode.FFA) || getGamemode().equals(Gamemode.OITC) || getGamemode().equals(Gamemode.GUN)) {

			if (pinkNukeActive != null)
				return pinkNukeActive.getDisplayName();

			int highestScore = 0;
			Player highestScoringPlayer = null;
			for (Player p : ffaPlayerScores.keySet()) {
				if (ffaPlayerScores.get(p) > highestScore) {
					highestScore = ffaPlayerScores.get(p);
					highestScoringPlayer = p;
				}
			}

			if (highestScoringPlayer == null) {
				return "nobody";
			}

			return highestScoringPlayer.getDisplayName();
		}

		if (getGamemode() == Gamemode.INFECT && blueTeamScore > 0) {
			return "blue";
		}

		if (redNukeActive)
			return "red";
		else if (blueNukeActive)
			return "blue";

		if (redTeamScore > blueTeamScore) {
			return "red";
		} else if (blueTeamScore > redTeamScore) {
			return "blue";
		}

		return "tie";
	}

	private String getFancyTime(int time) {

		String seconds = Integer.toString(time % 60);

		if (seconds.length() == 1) {
			seconds = "0" + seconds;
		}

		String minutes = Integer.toString(time / 60);

		if (minutes.length() == 1) {
			minutes = "0" + minutes;
		}

		return (minutes + ":" + seconds);
	}

	public ArrayList<Player> getPlayers() {
		return players;
	}

	private boolean areEnemies(Player a, Player b) {

		if (a == null || b == null) {
			return true;
		}

		if (redTeam.contains(a) && redTeam.contains(b)) {
			return false;
		} else if (blueTeam.contains(a) && blueTeam.contains(b)) {
			return false;
		}

		return true;
	}

	private HashMap<Player, Boolean> isAlive = new HashMap<>();
	private int getAlivePlayers(ArrayList<Player> team) {
		int count = 0;

		if (getGamemode() != Gamemode.DESTROY && getGamemode() != Gamemode.RESCUE)
			return 1;

		for (Player p : team) {
			if (isAlive.get(p)) {
				count++;
			}
		}

		return count;
	}


	public void kill(Player p, Player killer) {

		AssignmentManager.getInstance().updateAssignments(p, 1, getGamemode());

		if (getGamemode() == Gamemode.DESTROY || getGamemode() == Gamemode.RESCUE) {
			p.setGameMode(GameMode.SPECTATOR);
			p.getInventory().clear();
			isAlive.put(p, false);

			if (getGamemode() == Gamemode.RESCUE) {
				dropDogTag(p);
				if (isOnBlueTeam(p) && getAlivePlayers(blueTeam) > 0) {
					Main.sendTitle(p, Main.getPrefix() + Lang.RESPAWN_IF_DOG_TAG_PICKED_UP.getMessage(), "");
				} else if (isOnRedTeam(p) && getAlivePlayers(redTeam) > 0) {
					Main.sendTitle(p, Main.getPrefix() + Lang.RESPAWN_IF_DOG_TAG_PICKED_UP.getMessage(), "");
				}
			} else {
				Main.sendTitle(p, Main.getPrefix() + Lang.RESPAWN_NEXT_ROUND.getMessage(), "");
			}

			return;
		}


		if (getGamemode() == Gamemode.KC) {
			dropDogTag(p);
		}

		if (getGamemode() == Gamemode.INFECT && redTeam.contains(killer)) {
			blueTeam.remove(p);

			redTeam.add(p);

			if (getGamemode().equals(Gamemode.INFECT)) {
				blueTeamScore = blueTeam.size();
				redTeamScore = redTeam.size();
			}
		}

		if (getGamemode() == Gamemode.OITC) {
			if (ffaPlayerScores.get(p) == 0) {
				p.sendMessage(Main.getPrefix() + Lang.OITC_RAN_OUT_OF_LIVES.getMessage());
				p.setGameMode(GameMode.SPECTATOR);
				p.getInventory().clear();
				return;
			}
		}

		BukkitRunnable br = new BukkitRunnable() {
			int t = 3;

			public void run() {

				p.getInventory().clear();
				p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 120, 1));
				p.removePotionEffect(PotionEffectType.SPEED);

				if (t > 0) {

					p.getInventory().clear();
					p.setGameMode(GameMode.SPECTATOR);
					p.setSpectatorTarget(killer);

					if (t == 3)
						Main.sendTitle(p, Main.getPrefix() + Lang.YOU_WILL_RESPAWN.getMessage().replace("{time}", t + ""), "");
				} else {
					if (getState() == GameState.IN_GAME) {
						if (getGamemode() != Gamemode.FFA && getGamemode() != Gamemode.OITC && getGamemode() != Gamemode.GUN) {
							if (blueTeam.contains(p)) {
								spawnCodPlayer(p, getMap().getBlueSpawn());
							} else if (redTeam.contains(p)) {
								spawnCodPlayer(p, getMap().getRedSpawn());
							} else {
								assignTeams();
							}

							cancel();
						} else {
							spawnCodPlayer(p, getMap().getPinkSpawn());
							cancel();
						}
					} else {
						p.setGameMode(GameMode.ADVENTURE);
						p.teleport(Main.getLobbyLocation());
						p.setHealth(20D);
						p.setFoodLevel(20);
						cancel();
					}
				}

				t--;
			}
		};

		br.runTaskTimer(Main.getPlugin(), 0L, 20L);
	}

	private void updateTabList() {

		String teamColor;

		for (Player p : players) {

			if (isOnRedTeam(p)) {
				teamColor = ChatColor.RED + "";
			} else if (isOnBlueTeam(p)) {
				teamColor = ChatColor.BLUE + "";
			} else {
				teamColor = ChatColor.LIGHT_PURPLE + "";
			}

			CodScore score = playerScores.get(p);

			try {
				p.getClass().getMethod("setPlayerListHeader", String.class).invoke(p, Main.getHeader());
				p.getClass().getMethod("setPlayerListFooter", String.class).invoke(p, ChatColor.WHITE + "Playing " + ChatColor.GOLD + getMap().getGamemode().toString() + ChatColor.WHITE + " on " + ChatColor.GOLD + getMap().getName() + ChatColor.WHITE + "!");
			} catch(NoSuchMethodException ig) {} catch(Exception ignored) {}

			int level = ProgressionManager.getInstance().getLevel(p);
			String prestige = ProgressionManager.getInstance().getPrestigeLevel(p) > 0 ? ChatColor.WHITE + "[" + ChatColor.GREEN + ProgressionManager.getInstance().getPrestigeLevel(p) + ChatColor.WHITE + "]-" : "";
			String levelName = LevelNames.getInstance().getLevelName(level);
			levelName = !levelName.equals("") ? "[" + levelName + "] " : "";
			p.setPlayerListName(ChatColor.WHITE + levelName + prestige + "[" +
					level + "] " + teamColor + p.getDisplayName() + ChatColor.WHITE + " [K] " +
					ChatColor.GREEN + score.getKills() + ChatColor.WHITE + " [D] " + ChatColor.GREEN + score.getDeaths() +
					ChatColor.WHITE + " [S] " + ChatColor.GREEN + score.getKillstreak());
		}
	}

	public boolean isOnRedTeam(Player p) {
		return redTeam.contains(p);
	}

	public boolean isOnBlueTeam(Player p) {
		return blueTeam.contains(p);
	}

	public boolean isOnPinkTeam(Player p) {
		return ffaPlayerScores.containsKey(p);

	}

	public CodMap getMap() {

		if (currentMap == null) {
			changeMap(GameManager.pickRandomMap());
		}

		return currentMap;
	}

	public boolean forceStart(boolean forceStarted) {
		this.forceStarted = forceStarted;
		return forceStarted;
	}

	public CodScore getScore(Player p) {
		if (!playerScores.containsKey(p)) {
			playerScores.put(p, new CodScore(p));
		}

		return playerScores.get(p);
	}

	public GameState getState() {
		return state;
	}

	private void setState(GameState state) {
		this.state = state;
	}

	public Gamemode getGamemode() {
		return getMap().getGamemode();
	}

	public boolean isPastClassChange() {
		return pastClassChange;
	}

	public void changeClass(Player p) {
		if (!isPastClassChange()) {
			if (isOnBlueTeam(p))
				spawnCodPlayer(p, getMap().getBlueSpawn());
			else if (isOnRedTeam(p))
				spawnCodPlayer(p, getMap().getRedSpawn());
			else
				spawnCodPlayer(p, getMap().getPinkSpawn());
		}
	}

	private void handleDeath(Player killer, Player victim) {

		RankPerks rank = Main.getRank(killer);

		if (getGamemode().equals(Gamemode.TDM) || getGamemode().equals(Gamemode.KC) || getGamemode().equals(Gamemode.RSB) || getGamemode().equals(Gamemode.DOM) || getGamemode().equals(Gamemode.RESCUE) || getGamemode().equals(Gamemode.DESTROY) || getGamemode().equals(Gamemode.HARDPOINT)) {
			if (isOnRedTeam(killer)) {

				double xp = rank.getKillExperience();

				if (getGamemode().equals(Gamemode.KC)) {
					xp /= 2d;
				}

				Main.sendMessage(killer, "" + ChatColor.RED + ChatColor.BOLD + "YOU " + ChatColor.RESET + "" + ChatColor.WHITE + "[" + Lang.KILLED_TEXT.getMessage() +"] " + ChatColor.RESET + ChatColor.BLUE + ChatColor.BOLD + victim.getDisplayName(), Main.getLang());
				Main.sendActionBar(killer, ChatColor.YELLOW + "+" + xp + "xp");

				ProgressionManager.getInstance().addExperience(killer, xp);
				CreditManager.setCredits(killer, CreditManager.getCredits(killer) + rank.getKillCredits());
				kill(victim, killer);
				if (getGamemode() != Gamemode.RESCUE && getGamemode() != Gamemode.DESTROY && getGamemode() != Gamemode.KC) {
					if (getGamemode() != Gamemode.HARDPOINT) {
						addRedPoint();
					} else if (hardpointController == 1) {
						addRedPoint();
					}
				}
				updateScores(victim, killer, rank);
			} else if (isOnBlueTeam(killer)) {

				double xp = rank.getKillExperience();

				if (getGamemode().equals(Gamemode.KC)) {
					xp /= 2d;
				}

				Main.sendMessage(killer,  "" + ChatColor.BLUE + ChatColor.BOLD + "YOU " + ChatColor.RESET + ChatColor.WHITE + "[" + Lang.KILLED_TEXT.getMessage() + "] " + ChatColor.RESET + ChatColor.RED + ChatColor.BOLD + victim.getDisplayName(), Main.getLang());
				Main.sendActionBar(killer, ChatColor.YELLOW + "+" + xp + "xp");
				ProgressionManager.getInstance().addExperience(killer, xp);
				CreditManager.setCredits(killer, CreditManager.getCredits(killer) + rank.getKillCredits());
				kill(victim, killer);
				if (getGamemode() != Gamemode.RESCUE && getGamemode() != Gamemode.DESTROY && getGamemode() != Gamemode.KC) {
					if (getGamemode() != Gamemode.HARDPOINT) {
						addBluePoint();
					} else if (hardpointController == 0) {
						addBluePoint();
					}
				}
				updateScores(victim, killer, rank);
			}

		} else if (getGamemode().equals(Gamemode.CTF) || getGamemode().equals(Gamemode.INFECT)) {
			if (redTeam.contains(killer)) {
				Main.sendMessage(killer, "" + ChatColor.RED + ChatColor.BOLD + "YOU " + ChatColor.RESET + "" + ChatColor.WHITE + "[" + Lang.KILLED_TEXT.getMessage() + "] " + ChatColor.RESET + ChatColor.BLUE + ChatColor.BOLD + victim.getDisplayName(), Main.getLang());
				Main.sendActionBar(killer, ChatColor.YELLOW + "+" + rank.getKillExperience() + "xp");

				ProgressionManager.getInstance().addExperience(killer, rank.getKillExperience());
				CreditManager.setCredits(killer, CreditManager.getCredits(killer) + rank.getKillCredits());
				kill(victim, killer);
				updateScores(victim, killer, rank);
			} else if (blueTeam.contains(killer)) {
				Main.sendMessage(killer,  "" + ChatColor.BLUE + ChatColor.BOLD + "YOU " + ChatColor.RESET + ChatColor.WHITE + "[" + Lang.KILLED_TEXT.getMessage() + "] " + ChatColor.RESET + ChatColor.RED + ChatColor.BOLD + victim.getDisplayName(), Main.getLang());
				Main.sendActionBar(killer,  ChatColor.YELLOW + "+" + rank.getKillExperience() + "xp");
				CreditManager.setCredits(killer, CreditManager.getCredits(killer) + rank.getKillCredits());
				ProgressionManager.getInstance().addExperience(killer, rank.getKillExperience());
				kill(victim, killer);
				updateScores(victim, killer, rank);
			}

			if (getGamemode() == Gamemode.CTF) {
				if (victim.equals(redFlag.getFlagHolder())) {
					redFlag.drop(victim);
				} else if (victim.equals(blueFlag.getFlagHolder())) {
					blueFlag.drop(victim);
				}
			}

		} else if (getGamemode().equals(Gamemode.FFA) || getGamemode().equals(Gamemode.GUN) || getGamemode().equals(Gamemode.OITC)) {
			Main.sendMessage(killer, "" + ChatColor.GREEN + ChatColor.BOLD + "YOU " + ChatColor.RESET + ChatColor.WHITE + "[" + Lang.KILLED_TEXT.getMessage() + "] " + ChatColor.RESET	 + ChatColor.GOLD + ChatColor.BOLD + victim.getDisplayName(), Main.getLang());
			Main.sendActionBar(killer, ChatColor.YELLOW + "+" + rank.getKillExperience() + "xp");
			ProgressionManager.getInstance().addExperience(killer, rank.getKillExperience());
			CreditManager.setCredits(killer, CreditManager.getCredits(killer) + rank.getKillCredits());
			kill(victim, killer);
			if (getGamemode() == Gamemode.OITC) {
				removePointForPlayer(victim);
				ItemStack ammo = GameManager.oitcGun.getAmmo();
				ammo.setAmount(1);
				if (killer.getInventory().getItem(8) != null && killer.getInventory().getItem(8).getType() == ammo.getType()) {
					killer.getInventory().addItem(ammo);
				} else {
					killer.getInventory().setItem(8, ammo);
				}
			} else {
				addPointForPlayer(killer);
			}



			if (getGamemode() == Gamemode.GUN) {

				ItemStack held;

				try {
					held = (ItemStack) killer.getInventory().getClass().getMethod("getItemInMainHand").invoke(killer.getInventory());
				} catch(NoSuchMethodException e) {
					held = killer.getInventory().getItemInHand();
				} catch(Exception e) {
					held = killer.getInventory().getItemInHand();
				}

				if (held.equals(LoadoutManager.getInstance().knife)) {
					removePointForPlayer(victim);
				}

				killer.getInventory().clear();
				setTeamArmor(killer);
				killer.getInventory().setItem(32, InventoryManager.getInstance().selectClass);
				killer.getInventory().setItem(35, InventoryManager.getInstance().leaveItem);

				KillStreakManager.getInstance().streaksAfterDeath(killer);
				killer.getInventory().setItem(0, LoadoutManager.getInstance().knife);
				CodGun gun;
				try {
					gun = GameManager.gunGameGuns.get(ffaPlayerScores.get(killer));
					ItemStack gunItem = gun.getGunItem();
					ItemStack ammo = gun.getAmmo();
					ammo.setAmount(gun.getAmmoCount());

					killer.getInventory().setItem(1, gunItem);
					killer.getInventory().setItem(19, ammo);
				} catch(Exception ignored) {
					killer.getInventory().clear();
				}
			}

			updateScores(victim, killer, rank);
		}
	}

	private void updateScores(Player victim, Player killer, RankPerks rank) {

		playerScores.computeIfAbsent(killer, k -> new CodScore(killer));

		CodScore killerScore = playerScores.get(killer);

		killerScore.addScore(rank.getKillExperience());

		killerScore.addKillstreak();

		KillStreakManager.getInstance().checkStreaks(killer);

		killerScore.addKill();

		playerScores.put(killer, killerScore);

		if (playerScores.get(victim) == null) {
			playerScores.put(killer, new CodScore(victim));
		}

		CodScore victimScore = playerScores.get(victim);

		victimScore.setDeaths(victimScore.getDeaths() + 1);
		StatHandler.addDeath(victim);

		victimScore.resetKillstreak();

		playerScores.put(victim, victimScore);
	}

	/* Gamemode Listeners */

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerHit(EntityDamageByEntityEvent e) {

		if (!(e.getEntity() instanceof Player && e.getDamager() instanceof Player))
			return;

		Player victim = (Player) e.getEntity();
		Player attacker = (Player) e.getDamager();

		if (GameManager.isInMatch(victim) || GameManager.isInMatch(attacker)) {
			e.setCancelled(true);
		} else {
			return;
		}

		if (!players.contains(victim) && !players.contains(attacker))
			return;

		if (getState() != GameState.IN_GAME) {
			return;
		}

		if (!areEnemies(attacker, victim)) {
			e.setDamage(0);
			return;
		}

		double damage;

		ItemStack heldWeapon;

		try {
			heldWeapon = (ItemStack) attacker.getInventory().getClass().getMethod("getItemInMainHand").invoke(attacker.getInventory());
		} catch(NoSuchMethodException e2) {
			heldWeapon = attacker.getInventory().getItemInHand();
		} catch(Exception e1) {
			heldWeapon = attacker.getInventory().getItemInHand();
		}

		Material gSwordMat;
		Material wSwordMat;

		try {
			gSwordMat = Material.valueOf("GOLDEN_SWORD");
		} catch(Exception silent) {
			gSwordMat = Material.valueOf("GOLD_SWORD");
		}

		try {
			wSwordMat = Material.valueOf("WOODEN_SWORD");
		} catch(Exception silent) {
			wSwordMat = Material.valueOf("WOOD_SWORD");
		}

		if (heldWeapon.getType() == Material.DIAMOND_SWORD || heldWeapon.getType() == gSwordMat || heldWeapon.getType() == Material.IRON_SWORD || heldWeapon.getType() == Material.STONE_SWORD || heldWeapon.getType() == wSwordMat) {
			damage = Main.getDefaultHealth();
		} else {
			damage = Math.round(Main.getDefaultHealth() / 4);
		}

		health.damage(victim, damage);

		if (health.isDead(victim)) {
			if (!LoadoutManager.getInstance().getCurrentLoadout(victim).hasPerk(Perk.LAST_STAND)) {
				handleDeath(attacker, victim);
			} else {
				if (!PerkListener.getInstance().getIsInLastStand().contains(victim)) {
					PerkListener.getInstance().getIsInLastStand().add(victim);
					PerkListener.getInstance().lastStand(victim, this);
					double xp = Main.getRank(attacker).getKillExperience() / 2f;
					ChatColor t1 = redTeam.contains(attacker) ? ChatColor.RED : blueTeam.contains(attacker) ? ChatColor.BLUE : ChatColor.LIGHT_PURPLE;
					ChatColor t2 = t1 == ChatColor.RED ? ChatColor.BLUE : t1 == ChatColor.BLUE ? ChatColor.RED : ChatColor.LIGHT_PURPLE;
					Main.sendMessage(attacker,  "" + t1 + ChatColor.BOLD + "YOU " + ChatColor.RESET + ChatColor.WHITE + "[" + Lang.DOWNED_TEXT.getMessage() + "] " + ChatColor.RESET + t2 + ChatColor.BOLD + victim.getDisplayName(), Main.getLang());
					Main.sendActionBar(attacker, ChatColor.YELLOW + "+" + xp + "xp");
					ProgressionManager.getInstance().addExperience(attacker, xp);
					CreditManager.setCredits(attacker, CreditManager.getCredits(attacker) + Main.getRank(attacker).getKillCredits());
				} else {
					PerkListener.getInstance().getIsInLastStand().remove(victim);
					victim.setSneaking(false);
					victim.setWalkSpeed(0.2f);
					handleDeath(attacker, victim);
				}
			}
		}

	}

	@EventHandler
	public void preventInventoryMovement(InventoryClickEvent e) {

		Player p = (Player) e.getWhoClicked();

		if (getPlayers().contains(p)) {
			if (e.getCurrentItem() != null && e.getCurrentItem().equals(InventoryManager.getInstance().selectClass)) {
				InventoryManager.getInstance().openSelectClassInventory(p);
			} else if (e.getCurrentItem() != null && e.getCurrentItem().equals(InventoryManager.getInstance().leaveItem)) {
				GameManager.leaveMatch(p);
			}
			e.setCancelled(true);
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerHitWolf(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Player || e.getDamager() instanceof Projectile))
			return;

		if (!(e.getEntity() instanceof Wolf))
			return;

		Player damager;

		if (e.getDamager() instanceof Player) {
			damager = (Player) e.getDamager();
		} else {
			if (((Projectile) e.getDamager()).getShooter() instanceof Player) {
				damager = (Player) ((Projectile)e.getDamager()).getShooter();
			} else {
				return;
			}
		}

		double scalar = (20d / Main.getDefaultHealth()) * 0.4d;
		double damage = e.getDamage() * scalar;

		for (Player p : dogsScoreStreak.keySet()) {
			if (p.equals(damager)) {
				e.setCancelled(true);
				continue;
			}
			for (Wolf w : dogsScoreStreak.get(p)) {
				if (w.equals(e.getEntity())) {
					if (w.getHealth() - damage <= 0d) {
						e.getEntity().remove();
						e.setCancelled(true);
					} else {
						w.setHealth(w.getHealth() - damage);
						e.setCancelled(true);
					}
				}
			}

		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerHitByWeapon(EntityDamageByEntityEvent e) {

		Projectile bullet;

		if (e.getDamager() instanceof Projectile) {
			bullet = (Projectile) e.getDamager();
			if (!(bullet.getShooter() instanceof Player)) {
				return;
			}
		} else {
			return;
		}

		if (!(e.getEntity() instanceof Player))
			return;

		Player victim = (Player) e.getEntity();
		Player shooter = (Player) bullet.getShooter();

		double damage = e.getDamage();

		if (!players.contains(victim) && !players.contains(shooter))
			return;

		if (getState() != GameState.IN_GAME) {
			return;
		}

		if (players.contains(victim) && players.contains(shooter)) {
			e.setCancelled(true);
		} else {
			return;
		}

		if (!areEnemies(shooter, victim)) {
			return;
		}
		if (!health.isDead(victim)) {

			if (getGamemode() == Gamemode.OITC) {
				damage = health.defaultHealth * 2;
			}

			health.damage(victim, damage);

			if (health.isDead(victim)) {
				if (!LoadoutManager.getInstance().getCurrentLoadout(victim).hasPerk(Perk.LAST_STAND)) {
					handleDeath(shooter, victim);
				} else {
					PerkListener.getInstance().lastStand(victim, this);
				}
			}
		}
	}

	public void damagePlayer(Player p, double damage, Player... damagers) {
		health.damage(p, damage);
		if (health.isDead(p)) {
			if (!LoadoutManager.getInstance().getCurrentLoadout(p).hasPerk(Perk.LAST_STAND)) {
				if (damagers.length < 1) {
					Main.sendMessage(p, "" + ChatColor.GREEN + ChatColor.BOLD + "YOU " + ChatColor.RESET + "" + ChatColor.WHITE + "[" + Lang.KILLED_TEXT.getMessage() + "] " + ChatColor.RESET + ChatColor.GREEN + ChatColor.BOLD + "YOURSELF", Main.getLang());
				}
				kill(p, p);
			} else {
				PerkListener.getInstance().lastStand(p, this);
			}
		}
	}

	@EventHandler
	public void onPlayerPickupDogtag(PlayerPickupItemEvent e) {
		Player p = e.getPlayer();

		if (!GameManager.isInMatch(p))
			return;

		if (!players.contains(p))
			return;

		ItemStack stack = e.getItem().getItemStack();

		e.setCancelled(true);
		e.getItem().remove();

		if (stack.getItemMeta() == null || stack.getItemMeta().getLore() == null || stack.getItemMeta().getLore().size() == 0) {
			return;
		}

		Player tagOwner;

		try {
			tagOwner = Bukkit.getPlayer(UUID.fromString(stack.getItemMeta().getLore().get(0)));
		} catch(Exception ex) {
			return;
		}
		if (!areEnemies(p, tagOwner)) {
			if (getGamemode() == Gamemode.RESCUE) {
				if (isOnBlueTeam(tagOwner)) {
					spawnCodPlayer(tagOwner, getMap().getBlueSpawn());
				} else if (isOnRedTeam(tagOwner)) {
					spawnCodPlayer(tagOwner, getMap().getRedSpawn());
				}
			} else if (getGamemode() == Gamemode.KC) {
				p.sendMessage(Lang.KILL_DENIED.getMessage());
				Main.sendActionBar(p, ChatColor.YELLOW + "+" + (Main.getRank(p).getKillExperience() / 2) + "xp!");
				ProgressionManager.getInstance().addExperience(p, Main.getRank(p).getKillExperience() / 2);
			}
		} else {
			if (getGamemode() == Gamemode.RESCUE) {
				p.sendMessage(Main.getPrefix() + Lang.SPAWN_DENIED.getMessage().replace("{player}", tagOwner.getName()));
			} else if (getGamemode() == Gamemode.KC) {
				p.sendMessage(Lang.KILL_CONFIRMED.getMessage());
				Main.sendActionBar(p,ChatColor.YELLOW + "+" + Main.getRank(p).getKillExperience() + "xp!");
				ProgressionManager.getInstance().addExperience(p, Main.getRank(p).getKillExperience());
				if (isOnRedTeam(p)) {
					addRedPoint();
				} else if (isOnBlueTeam(p)) {
					addBluePoint();
				} else {
					assignTeams();
				}
			}
			e.setCancelled(true);
			e.getItem().remove();
		}

	}

	private void spawnDomFlags() {
		if(!getGamemode().equals(Gamemode.DOM))
			return;

		Location aLoc = getMap().getAFlagSpawn();
		Location bLoc = getMap().getBFlagSpawn();
		Location cLoc = getMap().getCFlagSpawn();

		if(aLoc == null || bLoc == null || cLoc == null) {
			Main.sendMessage(Main.getConsole(), Main.getPrefix() + ChatColor.RED + "The Alpha, Beta, or Charlie flag spawns have not been set for the current map in arena id " + getId() + ". The game will likely not work properly.", Main.getLang());
			return;
		}

		aFlag = new DomFlag(Lang.FLAG_A, aLoc);
		bFlag = new DomFlag(Lang.FLAG_B, bLoc);
		cFlag = new DomFlag(Lang.FLAG_C, cLoc);

		aFlag.spawn();
		bFlag.spawn();
		cFlag.spawn();
	}

	private void despawnDomFlags() {
		if (aFlag != null)
			aFlag.remove();

		if (bFlag != null)
			bFlag.remove();

		if (cFlag != null)
			cFlag.remove();
	}

	private void despawnHardpointFlag() {
		if (hardpointFlag != null)
			hardpointFlag.remove();
	}

	private void despawnCtfFlags() {
		if (blueFlag != null)
			blueFlag.despawn();

		if (redFlag != null)
			redFlag.despawn();
	}

	private void checkDomFlags() {
		if (!getGamemode().equals(Gamemode.DOM))
			return;


		int[] flags = {aFlag.checkFlag(this), bFlag.checkFlag(this), cFlag.checkFlag(this)};

		int blueFlags = 0,
				redFlags = 0;

		for (int flag : flags) {
			switch (flag) {
				case 0:
					blueFlags++;
					break;
				case 1:
					redFlags++;
					break;
			}
		}

		if (blueFlags > redFlags) {
			blueTeamScore++;
		} else if (redFlags > blueFlags) {
			redTeamScore++;
		}
	}

	private void updateHardpointFlagLocation() {

		Location lastLoc = null;
		if (hardpointFlag != null) {
			lastLoc = hardpointFlag.getLocation();
			despawnHardpointFlag();
		}

		final List<Location> locs = new ArrayList<>(getMap().getHardpointFlags());

		Location spawnLocation = null;

		if (locs.size() == 0) {
			Main.sendMessage(Main.getConsole(), Main.getPrefix() + ChatColor.RED + "No hardpoint locations set up, could not move hardpoint location!", Main.getLang());
			for (Player p : getPlayers()) {
				removePlayer(p);
			}
			return;
		} else if (locs.size() == 1) {
			spawnLocation = locs.get(0);
		} else {
			if (lastLoc != null) {
				for (Location possibleLoc : locs) {
					if (possibleLoc.equals(lastLoc))
						continue;
					spawnLocation = possibleLoc;
					break;
				}
			} else {
				spawnLocation = locs.get(0);
			}
		}

		for (Player p : getPlayers()) {
			p.sendMessage(Lang.HARDPOINT_FLAG_SPAWNED.getMessage());
		}

		hardpointController = -1;

		hardpointFlag = new DomFlag(Lang.FLAG_HARDPOINT, spawnLocation);

		hardpointFlag.spawn();
	}

	private void checkHardpointFlag() {
		hardpointController = hardpointFlag.checkFlag(this);
	}

	void setTeamArmor(Player p) {

		Color color;

		if (isOnBlueTeam(p)) {
			color = Color.BLUE;
		} else if (isOnRedTeam(p)) {
			if (getGamemode() == Gamemode.INFECT)
				color = Color.GREEN;
			else
				color = Color.RED;
		} else {
			color = Color.PURPLE;
		}

		ItemStack helmet = new ItemStack(Material.LEATHER_HELMET, 1);
		ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
		ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS, 1);
		ItemStack boots = new ItemStack(Material.LEATHER_BOOTS, 1);

		LeatherArmorMeta hMeta = (LeatherArmorMeta) helmet.getItemMeta();
		hMeta.setColor(color);
		helmet.setItemMeta(hMeta);

		LeatherArmorMeta cMeta = (LeatherArmorMeta) chest.getItemMeta();
		cMeta.setColor(color);
		chest.setItemMeta(cMeta);

		LeatherArmorMeta lMeta = (LeatherArmorMeta) legs.getItemMeta();
		lMeta.setColor(color);
		legs.setItemMeta(lMeta);

		LeatherArmorMeta bMeta = (LeatherArmorMeta) boots.getItemMeta();
		bMeta.setColor(color);
		boots.setItemMeta(bMeta);

		p.getInventory().setHelmet(helmet);
		p.getInventory().setChestplate(chest);
		p.getInventory().setLeggings(legs);
		p.getInventory().setBoots(boots);
		p.updateInventory();
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {

		if (!players.contains(e.getPlayer()))
			return;

		Player p = e.getPlayer();

		if (p.getItemInHand().equals(KillStreak.UAV.getKillStreakItem())) {
			if (isOnBlueTeam(p)) {
				if (!blueUavActive) {
					startUav(p);
				} else {
					Main.sendMessage(p, Lang.KILLSTREAK_AIRSPACE_OCCUPIED.getMessage(), Main.getLang());
				}
			} else if (isOnRedTeam(p)) {
				if (!redUavActive) {
					startUav(p);
				} else {
					Main.sendMessage(p, Lang.KILLSTREAK_AIRSPACE_OCCUPIED.getMessage(), Main.getLang());
				}

			} else {
				startUav(p);
			}
		} else if (p.getItemInHand().equals(KillStreak.COUNTER_UAV.getKillStreakItem())) {
			startCounterUav(p);
			if (!isOnBlueTeam(p) && !isOnRedTeam(p)) {
				if (!pinkCounterUavActive) {
					startCounterUav(p);
				} else {
					Main.sendMessage(p, Lang.KILLSTREAK_AIRSPACE_OCCUPIED.getMessage(), Main.getLang());
				}
			}
		} else if (p.getItemInHand().equals(KillStreak.DOGS.getKillStreakItem())) {
			startDogs(p);
		} else if (p.getItemInHand().equals(KillStreak.NUKE.getKillStreakItem())) {
			startNuke(p);
		}

	}

	private void startUav(Player owner) {

		if (!players.contains(owner))
			return;

		if (isOnRedTeam(owner))
			redUavActive = true;
		else if (isOnBlueTeam(owner))
			blueUavActive = true;

		owner.getInventory().remove(KillStreak.UAV.getKillStreakItem());
		KillStreakManager.getInstance().useStreak(owner, KillStreak.UAV);

		BukkitRunnable br = new BukkitRunnable() {

			int t = 10;

			@Override
			public void run() {
				t--;

				if (t < 0) {
					if (isOnRedTeam(owner))
						redUavActive = false;
					else if (isOnBlueTeam(owner))
						blueUavActive = false;
					this.cancel();
				}

				if(isOnBlueTeam(owner)) {
					if (redCounterUavActive)
						return;

					//blue launched
					for (Player p : redTeam) {
						if (health.isDead(p))
							continue;
						Firework fw = p.getLocation().getWorld().spawn(p.getLocation(), Firework.class);
						FireworkMeta fwm = fw.getFireworkMeta();
						fwm.addEffect(FireworkEffect.builder()
								.flicker(false)
								.trail(true)
								.with(FireworkEffect.Type.BALL)
								.withColor(Color.RED)
								.build());

						fwm.setPower(3);

						fw.setFireworkMeta(fwm);
					}
				} else if(isOnRedTeam(owner)) {
					//red launched
					if (blueCounterUavActive)
						return;

					for (Player p : blueTeam) {
						if (health.isDead(p))
							continue;
						Firework fw = p.getLocation().getWorld().spawn(p.getLocation(), Firework.class);
						FireworkMeta fwm = fw.getFireworkMeta();
						fwm.addEffect(FireworkEffect.builder()
								.flicker(false)
								.trail(true)
								.with(FireworkEffect.Type.BALL)
								.withColor(Color.BLUE)
								.build());

						fwm.setPower(3);

						fw.setFireworkMeta(fwm);
					}
				} else {
					//pink
					if (pinkCounterUavActive)
						return;

					for (Player p : players) {
						if (p == owner)
							continue;

						if (health.isDead(p))
							continue;

						Firework fw = p.getLocation().getWorld().spawn(p.getLocation(), Firework.class);
						FireworkMeta fwm = fw.getFireworkMeta();
						fwm.addEffect(FireworkEffect.builder()
								.flicker(false)
								.trail(true)
								.with(FireworkEffect.Type.BALL)
								.withColor(Color.PURPLE)
								.build());

						fwm.setPower(3);

						fw.setFireworkMeta(fwm);
					}
				}
			}
		};

		br.runTaskTimer(Main.getPlugin(), 3L, 60L);
	}

	private void startCounterUav(Player owner) {

		if (!players.contains(owner))
			return;

		KillStreakManager.getInstance().useStreak(owner, KillStreak.COUNTER_UAV);

		owner.getInventory().remove(KillStreak.COUNTER_UAV.getKillStreakItem());

		if (isOnBlueTeam(owner)) {
			blueCounterUavActive = true;
		} else if (isOnRedTeam(owner)) {
			redCounterUavActive = true;
		} else {
			pinkCounterUavActive = true;
		}

		BukkitRunnable br = new BukkitRunnable() {
			@Override
			public void run() {
				if (isOnBlueTeam(owner)) {
					blueCounterUavActive = false;
				} else if (isOnRedTeam(owner)) {
					redCounterUavActive = false;
				} else {
					pinkCounterUavActive = false;
				}
			}
		};

		br.runTaskLater(Main.getPlugin(), 20L * 20L);

	}

	public HashMap<Player, Wolf[]> dogsScoreStreak = new HashMap<>();

	private void startDogs(Player owner) {
		if (!players.contains(owner))
			return;

		KillStreakManager.getInstance().useStreak(owner, KillStreak.DOGS);

		owner.getInventory().remove(KillStreak.DOGS.getKillStreakItem());

		Wolf[] wolves = new Wolf[8];

		for (int i = 0; i < 8; i++) {
			Wolf wolf = owner.getLocation().getWorld().spawn(owner.getLocation(), Wolf.class);
			wolf.setOwner(owner);
			wolf.setAngry(true);
			DyeColor collarColor;

			if (isOnBlueTeam(owner))
				collarColor = DyeColor.BLUE;
			else if (isOnRedTeam(owner))
				collarColor = DyeColor.RED;
			else
				collarColor = DyeColor.PINK;

			wolf.setCollarColor(collarColor);
			wolf.setCanPickupItems(false);
			wolf.setCustomName(owner.getDisplayName() + "'s Dog");
			wolf.setCustomNameVisible(true);
			wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 2));

			wolves[i] = wolf;
		}

		for (int i = 0; i < wolves.length; i++) {
			Wolf wolf = wolves[i];

			if (isOnBlueTeam(owner)) {
				Player target;
				int index = (new Random()).nextInt(redTeam.size());
				target = redTeam.get(index);

				wolf.setTarget(target);
			} else if (isOnRedTeam(owner)) {
				Player target;
				int index = (new Random()).nextInt(blueTeam.size());
				target = blueTeam.get(index);

				wolf.setTarget(target);
			} else {
				Player target;
				do {
					int index = (new Random()).nextInt(players.size());
					target = players.get(index);
					if (players.size() == 1) {
						target = null;
						break;
					}
				} while (!target.equals(owner));

				if (target != null)
					wolf.setTarget(target);
			}
		}

		if (dogsScoreStreak.containsKey(owner)) {
			for (Wolf w : dogsScoreStreak.get(owner)) {
				if (w != null)
					w.remove();
			}
		}

		dogsScoreStreak.put(owner, wolves);

		BukkitRunnable br = new BukkitRunnable() {
			int t = 45;

			@Override
			public void run() {
				t--;

				if (t < 0) {

					Wolf[] currentWolves = dogsScoreStreak.get(owner);
					if (Arrays.equals(currentWolves, wolves))
						dogsScoreStreak.remove(owner);

					for (Wolf w : wolves) {
						Objects.requireNonNull(w).remove();
					}
					this.cancel();
					return;
				}
				for (int i = 0; i < wolves.length; i++) {
					Wolf w = wolves[i];
					if (w == null) {
						Wolf wolf = owner.getLocation().getWorld().spawn(owner.getLocation(), Wolf.class);
						wolf.setOwner(owner);
						wolf.setAngry(true);
						DyeColor collarColor;

						if (isOnBlueTeam(owner))
							collarColor = DyeColor.BLUE;
						else if (isOnRedTeam(owner))
							collarColor = DyeColor.RED;
						else
							collarColor = DyeColor.PINK;

						wolf.setCollarColor(collarColor);
						wolf.setCanPickupItems(false);
						wolf.setCustomName(owner.getDisplayName() + "'s Dog");
						wolf.setCustomNameVisible(true);
						wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 2));
						wolves[i] = wolf;
						w = wolves[i];
					}

					w.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 2));

					if (w.getTarget() == null
							|| !(w.getTarget() instanceof Player)
							|| (w.getTarget() instanceof Player && health.isDead(((Player) w.getTarget())))) {
						if (isOnBlueTeam(owner)) {
							Player target;
							int index = (new Random()).nextInt(redTeam.size());
							target = redTeam.get(index);

							w.setTarget(target);

						} else if (isOnRedTeam(owner)) {
							Player target;
							int index = (new Random()).nextInt(blueTeam.size());
							target = blueTeam.get(index);

							w.setTarget(target);
						} else {
							Player target;
							do {
								int index = (new Random()).nextInt(players.size());
								target = players.get(index);
								if (players.size() == 1) {
									target = null;
									break;
								}
							} while (!target.equals(owner));

							if (target != null)
								w.setTarget(target);
						}
					}
				}
			}
		};

		br.runTaskTimer(Main.getPlugin(), 0L, 20L);
	}

	private void startNuke(Player owner) {
		if (!players.contains(owner))
			return;

		KillStreakManager.getInstance().useStreak(owner, KillStreak.NUKE);
		owner.getInventory().remove(KillStreak.NUKE.getKillStreakItem());

		if (!redNukeActive && !blueNukeActive && pinkNukeActive == null) {

			ChatColor tColor;
			String launcher = owner.getDisplayName();

			if (isOnRedTeam(owner)) {
				redNukeActive = true;
				tColor = ChatColor.RED;
			} else if (isOnBlueTeam(owner)) {
				blueNukeActive = true;
				tColor = ChatColor.BLUE;
			} else {
				tColor = ChatColor.LIGHT_PURPLE;
				pinkNukeActive = owner;
			}

			BukkitRunnable br = new BukkitRunnable() {

				int t = 10;

				@Override
				public void run() {
					t--;

					if (t < 1 || getState() != GameState.IN_GAME) {
						if (getState() == GameState.IN_GAME) {
							stopGame();
						}
						blueNukeActive = false;
						redNukeActive = false;
						pinkNukeActive = null;
						this.cancel();
					}

					for (Player p : players) {
						String title = Lang.NUKE_LAUNCHED_TITLE.getMessage().replace("{team-color}", tColor + "").replace("{team}", launcher),
								subtitle = Lang.NUKE_LAUNCHED_SUBTITLE.getMessage().replace("{time}", Integer.toString(t));

						Main.sendTitle(p, title, subtitle, tColor, 1, 20, 1);
					}
				}
			};

			br.runTaskTimer(Main.getPlugin(), 0L, 20L);
		}
	}

	private void updateTimeLeft() {
		if (getGamemode() != Gamemode.INFECT) {
			gameTime = Main.getPlugin().getConfig().getInt("gameTime." + getGamemode().toString());
		} else {
			if (ComVersion.getPurchased())
				gameTime = Main.getPlugin().getConfig().getInt("maxScore.INFECT");
			else
				gameTime = 120;
		}
	}

	private ScoreboardManager getScoreboardManager() {
		return scoreboardManager;
	}

	void incrementScore(Player p) {
		if (isOnRedTeam(p))
			redTeamScore++;
		else if (isOnBlueTeam(p))
			blueTeamScore++;
		else
			try {
				throw new Exception("Unexpected game logic when incrementing score!");
			} catch(Exception e) {
				e.printStackTrace();
			}
	}
}
