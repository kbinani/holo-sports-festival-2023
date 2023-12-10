package com.github.kbinani.holosportsfestival2023;

import com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener;
import com.github.kbinani.holosportsfestival2023.holoup.HoloUpEventListener;
import com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener;
import com.github.kbinani.holosportsfestival2023.relay.RelayEventListener;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.world.SpawnChangeEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.util.*;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@SuppressWarnings("unused")
public class Main extends JavaPlugin implements Listener, KibasenEventListener.Delegate, RelayEventListener.Delegate {
  enum TrackAndFieldOwner {
    KIBASEN,
    RELAY,
  }

  public static final @Nonnull String sScoreboardTeamPrefix = "hololive_sports_festival_2023_";

  private World world;
  private final List<MiniGame> miniGames = new ArrayList<>();
  private boolean isLevelsReady = false;
  private @Nullable TrackAndField taf;
  private @Nullable TrackAndFieldOwner tafOwner;
  private int spawnProtection = 16;

  @Override
  public void onEnable() {
    var server = getServer();
    PluginManager pluginManager = server.getPluginManager();

    Optional<World> overworld = server.getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
    if (overworld.isEmpty()) {
      getLogger().log(java.util.logging.Level.SEVERE, "server should have at least one overworld dimension");
      pluginManager.disablePlugin(this);
      return;
    }
    world = overworld.get();
    if (miniGames.isEmpty()) {
      miniGames.add(new HimeraceEventListener(world, this, new int[]{0, 1, 2}));
      miniGames.add(new HoloUpEventListener(world, this));
      miniGames.add(new KibasenEventListener(world, this, this));
      miniGames.add(new RelayEventListener(world, this, this));
    }

    List<String> reasons = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Boolean mobGriefing = world.getGameRuleValue(GameRule.MOB_GRIEFING);
    Boolean keepInventory = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
    Boolean showDeathMessages = world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
    Boolean announceAdvancements = world.getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS);
    Boolean doWeatherCycle = world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE);
    if (mobGriefing != null && mobGriefing) {
      reasons.add("mobGriefing gamerule is set to true");
    }
    if (keepInventory != null && !keepInventory) {
      reasons.add("keepInventory gamerule is set to false");
    }
    if (showDeathMessages != null && showDeathMessages) {
      warnings.add("showDeathMessages gamerule is set to true");
    }
    if (announceAdvancements != null && announceAdvancements) {
      warnings.add("announceAdvancements gamerule is set to true");
    }
    if (world.getDifficulty() == Difficulty.PEACEFUL) {
      reasons.add("the \"Himerace\" mini-game is not playable as the difficulty is set to peaceful");
    }
    if (!world.getPVP()) {
      reasons.add("pvp is set to false");
    }
    if (doWeatherCycle != null && doWeatherCycle) {
      reasons.add("the \"Holoup\" mini-game is not playable as the doWeatherCycle gamerule is set to true");
    }

    var props = new Properties();
    try {
      props.load(new FileInputStream("server.properties"));
    } catch (Throwable e) {
      reasons.add("cannot read server.properties");
    }

    var spawnAnimals = props.getProperty("spawn-animals");
    if (!Boolean.parseBoolean(spawnAnimals)) {
      reasons.add("the \"Himerace\" mini-game is not playable as the spawn-animals is set to false");
    }

    try {
      var spawnProtectionValue = props.getProperty("spawn-protection");
      spawnProtection = Integer.parseInt(spawnProtectionValue);
    } catch (Throwable e) {
    }
    if (spawnProtection > 0) {
      var spawn = world.getSpawnLocation();
      var bx = spawn.getBlockX();
      var bz = spawn.getBlockZ();
      var protectedBounds = new BoundingBox(bx - spawnProtection, -100, bz - spawnProtection, bx + spawnProtection, 500, bz + spawnProtection);
      for (var game : miniGames) {
        if (game.miniGameGetBoundingBox().overlaps(protectedBounds)) {
          reasons.add("mini-game overlaps world spawn protection: " + game.getClass().getSimpleName());
        }
      }
    }
    if (disablePluginBecause(reasons)) {
      return;
    }
    if (!warnings.isEmpty()) {
      for (String warning : warnings) {
        getLogger().warning(warning);
      }
    }

    pluginManager.registerEvents(this, this);

    for (var miniGame : miniGames) {
      pluginManager.registerEvents(miniGame, this);
    }
    cleanupScoreboardTeams();
  }

  private boolean disablePluginBecause(List<String> reasons) {
    if (reasons.isEmpty()) {
      return false;
    }
    getLogger().log(java.util.logging.Level.SEVERE, "Disabling the plugin because:");
    for (String reason : reasons) {
      getLogger().log(java.util.logging.Level.SEVERE, "  " + reason);
    }
    getServer().getPluginManager().disablePlugin(this);
    return true;
  }

  private void cleanupScoreboardTeams() {
    var scoreboard = getServer().getScoreboardManager().getMainScoreboard();
    for (var team : new HashSet<>(scoreboard.getTeams())) {
      if (team.getName().startsWith(sScoreboardTeamPrefix)) {
        team.unregister();
      }
    }
  }

  @Override
  public void onDisable() {
    for (var game : miniGames) {
      game.miniGameReset();
    }
    getServer().getOnlinePlayers().forEach(player -> {
      for (var game : miniGames) {
        game.miniGameClearItem(player);
      }
    });
    cleanupScoreboardTeams();
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerJoin(PlayerJoinEvent e) {
    if (!isLevelsReady) {
      isLevelsReady = true;
      for (var miniGame : miniGames) {
        miniGame.miniGameReset();
      }
    }
    var player = e.getPlayer();
    for (var miniGame : miniGames) {
      miniGame.miniGameClearItem(player);
    }
    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent e) {
    var player = e.getPlayer();
    switch (e.getStatus()) {
      case DECLINED -> {
        player.kick(text("Resource pack discarded. You must apply the server resource pack to join", RED));
      }
      case FAILED_DOWNLOAD -> {
        getLogger().warning("リソースパックが FAILED_DOWNLOAD となっているプレイヤーがいます: " + player.getName());
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onCreatureSpawn(CreatureSpawnEvent e) {
    if (e.isCancelled()) {
      return;
    }
    if (e.getEntity().getWorld() != world) {
      return;
    }
    switch (e.getSpawnReason()) {
      case NATURAL:
      case VILLAGE_INVASION:
      case BUILD_WITHER:
      case BUILD_IRONGOLEM:
      case BUILD_SNOWMAN:
      case SPAWNER_EGG:
      case SPAWNER:
        e.setCancelled(true);
        break;
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onBlockForm(BlockFormEvent e) {
    if (e.isCancelled()) {
      return;
    }
    var from = e.getBlock();
    if (from.getType() == Material.WHITE_CONCRETE_POWDER) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onBlockFade(BlockFadeEvent e) {
    var from = e.getBlock().getType();
    var to = e.getNewState().getType();
    if (from == Material.FIRE_CORAL_BLOCK && to == Material.DEAD_FIRE_CORAL_BLOCK) {
      e.setCancelled(true);
    } else if (from == Material.HORN_CORAL_BLOCK && to == Material.DEAD_HORN_CORAL_BLOCK) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerQuit(PlayerQuitEvent e) {
    var player = e.getPlayer();
    Cloakroom.shared.restore(player);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityExhaustion(EntityExhaustionEvent e) {
    if (!(e.getEntity() instanceof Player player)) {
      return;
    }
    e.setExhaustion(0);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onSpawnChange(SpawnChangeEvent e) {
    var world = e.getWorld();
    if (world != this.world) {
      return;
    }
    var reasons = new ArrayList<String>();
    if (spawnProtection <= 0) {
      return;
    }
    var spawn = world.getSpawnLocation();
    var cx = spawn.getBlockX() / 16;
    var cz = spawn.getBlockZ() / 16;
    var protectedBounds = new BoundingBox((cx - spawnProtection) * 16, -100, (cz - spawnProtection) * 16, (cx + spawnProtection) * 16, 500, (cz + spawnProtection) * 16);
    for (var game : miniGames) {
      if (game.miniGameGetBoundingBox().overlaps(protectedBounds)) {
        reasons.add("mini-game overlaps world spawn protection: " + game.getClass().getSimpleName());
      }
    }
    disablePluginBecause(reasons);
  }

  @Override
  public @Nullable TrackAndField kibasenTakeTrackAndFieldOwnership() {
    var taf = this.ensureTrackAndField();
    if (this.tafOwner == null || this.tafOwner == TrackAndFieldOwner.KIBASEN) {
      getLogger().log(Level.INFO, "ownership of track and field is taken by kibasen");
      this.tafOwner = TrackAndFieldOwner.KIBASEN;
      return taf;
    } else {
      return null;
    }
  }

  @Override
  public void kibasenReleaseTrackAndFieldOwnership() {
    if (this.tafOwner != null && this.tafOwner == TrackAndFieldOwner.KIBASEN) {
      this.tafOwner = null;
      getLogger().log(Level.INFO, "ownership of track and field was released from kibasen");
    }
  }

  @Override
  public Point3i kibasenGetJoinSignLocation(TeamColor color) {
    var taf = ensureTrackAndField();
    return switch (color) {
      case RED -> taf.kibasenJoinRedSign;
      case WHITE -> taf.kibasenJoinWhiteSign;
      case YELLOW -> taf.kibasenJoinYellowSign;
    };
  }

  @Override
  public Point3i kibasenGetAnnounceEntryListSignLocation() {
    var taf = ensureTrackAndField();
    return taf.kibasenEntryListSign;
  }

  @Override
  public Point3i kibasenGetStartSignLocation() {
    var taf = ensureTrackAndField();
    return taf.kibasenStartSign;
  }

  @Override
  public BoundingBox kibasenGetAnnounceBounds() {
    var taf = ensureTrackAndField();
    return taf.announceBounds;
  }

  @Override
  public Point3i relayGetJoinSignLocation(TeamColor color) {
    var taf = ensureTrackAndField();
    return switch (color) {
      case RED -> taf.relayJoinRedSign;
      case WHITE -> taf.relayJoinWhiteSign;
      case YELLOW -> taf.relayJoinYellowSign;
    };
  }

  @Override
  public Point3i relayGetStartSignLocation() {
    var taf = ensureTrackAndField();
    return taf.relayStartSign;
  }

  @Override
  public Point3i relayGetAnnounceEntryListSignLocation() {
    var taf = ensureTrackAndField();
    return taf.relayAnnounceEntryListSign;
  }

  @Override
  public BoundingBox relayGetAnnounceBounds() {
    var taf = ensureTrackAndField();
    return taf.announceBounds;
  }

  @Override
  public Point3i relayGetAbortSignLocation() {
    var taf = ensureTrackAndField();
    return taf.relayAbortSign;
  }

  @Nullable
  @Override
  public TrackAndField relayTakeTrackAndFieldOwnership() {
    var taf = this.ensureTrackAndField();
    if (this.tafOwner == null || this.tafOwner == TrackAndFieldOwner.RELAY) {
      getLogger().log(Level.INFO, "ownership of track and field is taken by relay");
      this.tafOwner = TrackAndFieldOwner.RELAY;
      return taf;
    } else {
      return null;
    }
  }

  @Override
  public void relayReleaseTrackAndFieldOwnership() {
    if (this.tafOwner != null && this.tafOwner == TrackAndFieldOwner.RELAY) {
      this.tafOwner = null;
      getLogger().log(Level.INFO, "ownership of track and field was released from relay");
    }
  }

  private @Nonnull TrackAndField ensureTrackAndField() {
    if (this.taf != null) {
      return this.taf;
    }
    var taf = new TrackAndField(world, new Point3i(0, 0, 0));
    taf.setMode(TrackAndField.Mode.IDLE);
    this.taf = taf;
    return taf;
  }
}
