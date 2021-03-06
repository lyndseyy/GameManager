package cc.lynzie.minigame.arena;

import cc.lynzie.minigame.GameManager;
import cc.lynzie.minigame.arena.state.GameState;
import cc.lynzie.minigame.arena.state.StateManager;
import cc.lynzie.minigame.data.ArenaConfig;
import cc.lynzie.minigame.player.GamePlayer;
import cc.lynzie.minigame.player.GameScoreboardLines;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class GameArena {

  // Basic configuration options.
  private final ArenaConfig config;
  private String arenaName;
  private String mapName;
  private String joinMessage;
  private String leaveMessage;
  private Location preGameLocation;
  private int minPlayers;
  private int maxPlayers;

  // Information for the current game
  private GameManager gameManager;
  private ArenaScoreboardManager scoreboardManager;
  private GameScoreboardLines scoreboard;
  private final StateManager stateManager;
  private boolean allowNewPlayers = true;
  private GameState currentGameState;
  private List<GameState> gameStates = new ArrayList<>();
  private Logger logger;
  private World arenaWorld;
  private List<Location> playerSpawns = new ArrayList<>();
  private List<GamePlayer> players = new ArrayList<>();
  private List<GamePlayer> activePlayers = new ArrayList<>();

  public GameArena(GameManager gameManager, String arenaName) {
    this.gameManager = gameManager;
    this.config = gameManager.getArenaConfig();
    this.arenaName = arenaName;
    this.logger = LogManager.getLogger(String.format("Arena [%s]", arenaName));
    this.stateManager = new StateManager(this);
    this.scoreboardManager = new ArenaScoreboardManager(gameManager);

    initArena();
  }

  public void initArena() {
    this.mapName = config.getConfig().getString(String.format("arenas.%s.map", arenaName));
    this.arenaWorld = Bukkit.getWorld(mapName);

    this.minPlayers = config.getConfig().getInt(String.format("arenas.%s.players.min", arenaName));
    this.maxPlayers = config.getConfig().getInt(String.format("arenas.%s.players.max", arenaName));
    this.joinMessage = config.getConfig()
        .getString(String.format("arenas.%s.players.join-msg", arenaName));
    this.leaveMessage = config.getConfig()
        .getString(String.format("arenas.%s.players.leave-msg", arenaName));
    String[] preGameSpawnCoords = config.getConfig().getString(
        String.format("arenas.%s.pre-game-spawn", arenaName)).split(",");
    this.preGameLocation = new Location(this.arenaWorld, Double.parseDouble(preGameSpawnCoords[0]),
        Double.parseDouble(preGameSpawnCoords[1]), Double.parseDouble(preGameSpawnCoords[2]));

    for (String spawn : config.getConfig()
        .getStringList(String.format("arenas.%s.spawns", arenaName))) {
      String[] coords = spawn.split(",");
      this.playerSpawns.add(
          new Location(arenaWorld, Double.parseDouble(coords[0]), Double.parseDouble(coords[1]),
              Double.parseDouble(coords[2])));
    }

    // Set the StateManager to go off every second.
    this.gameManager.getJavaPlugin().getServer().getScheduler()
        .runTaskTimer(this.gameManager.getJavaPlugin(),
            this.stateManager::performUpdate, 0L, 20L);
  }

  public void addPlayer(GamePlayer gamePlayer) {
    // Make sure that we're able to add the player to the arena, if we can't
    // decline them with a message.
    if (!allowNewPlayers && !(players.size() >= maxPlayers)) {
      gamePlayer.sendMessage(
          Component.text("Sorry, this game isn't accepting new players right now!")
              .color(TextColor.color(255, 100, 100)));
      return;
    }

    // Add the player to our list and send a message welcoming them.
    players.add(gamePlayer);
    activePlayers.add(gamePlayer);
    scoreboardManager.createBoardForPlayer(gamePlayer, scoreboard);

    this.gameManager.addPlayer(gamePlayer);
    sendMessage(joinMessage.replace("{player}", gamePlayer.getDisplayName())
        .replace("{cur}", "" + players.size())
        .replace("{max}", "" + maxPlayers));

    // Teleport the player to the pre-game arena.
    gamePlayer.teleportPlayer(preGameLocation);
    gamePlayer.onJoin();

    gamePlayer.setArena(this);
    this.allowNewPlayers = maxPlayers > players.size();
  }

  public void removePlayer(GamePlayer gamePlayer) {
    players.remove(gamePlayer);
    activePlayers.remove(gamePlayer);

    sendMessage(leaveMessage.replace("{player}", gamePlayer.getDisplayName())
        .replace("{cur}", "" + players.size())
        .replace("{max}", "" + maxPlayers));
  }

  public void removeActivePlayer(GamePlayer player) {
    activePlayers.remove(player);
  }

  /**
   * Adds a new GameState to the arena, and sets it as the current one if there are no other ones.
   *
   * @param gameState The GameState to add.
   */
  public void addGameState(GameState gameState) {
    this.gameStates.add(gameState);
    if (this.currentGameState == null) {
      currentGameState = gameState;
    }
  }

  /**
   * Loops through all the GameStates setup for this arena, if there's
   * one that hasn't ended, yet it will be returned.
   *
   * @return The next GameState to happen.
   */
  public GameState getNextState() {
    for (GameState gameState : gameStates) {
      if (!gameState.isEnded()) {
        return gameState;
      }
    }

    return null;
  }

  public List<GamePlayer> getActivePlayers() {
    return activePlayers;
  }

  public void sendMessage(String msg) {
    for (GamePlayer player : players) {
      player.sendMessage(msg);
    }
  }

  public void sendMessage(Component msg) {
    for (GamePlayer player : players) {
      player.sendMessage(msg);
    }
  }

  public ArenaScoreboardManager getScoreboardManager() {
    return scoreboardManager;
  }

  public GameManager getGameManager() {
    return gameManager;
  }

  public GameScoreboardLines getScoreboard() {
    return scoreboard;
  }

  public void setScoreboard(GameScoreboardLines scoreboard) {
    this.scoreboard = scoreboard;
  }

  public StateManager getStateManager() {
    return stateManager;
  }

  public GameState getCurrentGameState() {
    return currentGameState;
  }

  public void setCurrentGameState(GameState currentGameState) {
    this.currentGameState = currentGameState;
  }

  public ArenaConfig getConfig() {
    return config;
  }

  public String getArenaName() {
    return arenaName;
  }

  public String getMapName() {
    return mapName;
  }

  public String getJoinMessage() {
    return joinMessage;
  }

  public String getLeaveMessage() {
    return leaveMessage;
  }

  public Location getPreGameLocation() {
    return preGameLocation;
  }

  public int getMinPlayers() {
    return minPlayers;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public boolean isAllowingNewPlayers() {
    return allowNewPlayers;
  }

  public void setAllowNewPlayers(boolean allowNewPlayers) {
    this.allowNewPlayers = allowNewPlayers;
  }

  public List<GameState> getGameStates() {
    return gameStates;
  }

  public Logger getLogger() {
    return logger;
  }

  public World getArenaWorld() {
    return arenaWorld;
  }

  public List<Location> getPlayerSpawns() {
    return playerSpawns;
  }

  public List<GamePlayer> getPlayers() {
    return players;
  }
}