package com.github.kbinani.holosportsfestival2023.holoup;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.holoup.HoloUpEventListener.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

class Race {
  interface Delegate {
    void raceDidFinish();

    void raceDidDetectGoal(TeamColor color, Player player);

    void raceDidDetectCheckpoint(TeamColor color, Player player, int index);
  }

  private final Map<TeamColor, EntityTracking<Player>> participants;
  private final JavaPlugin owner;
  private final World world;
  private final Delegate delegate;
  private final BukkitTask timeoutTask;
  private final BukkitTask countdownTask;
  private @Nullable Cancellable countdown;
  private final Map<TeamColor, BossBar> bars = new HashMap<>();
  private final BoundingBox announceBounds;
  private final long startedMillis;
  private final BukkitTask timerTask;
  private final HashMap<TeamColor, Long> goaledMillis = new HashMap<>();
  private final Map<TeamColor, Integer> clearedCheckpoint = new HashMap<>();
  private final Map<Player, BukkitTask> tridentCooldownTask = new HashMap<>();
  private final @Nonnull Announcer announcer;

  private static final long durationSeconds = 300;
  static final int groundLevel = y(100);

  Race(
    @Nonnull JavaPlugin owner,
    @Nonnull World world,
    @Nonnull BoundingBox announceBounds,
    @Nonnull Map<TeamColor, EntityTracking<Player>> registrants,
    @Nonnull Announcer announcer,
    @Nonnull Delegate delegate) //
  {
    this.owner = owner;
    this.world = world;
    this.participants = new HashMap<>(registrants);
    this.delegate = delegate;
    this.announceBounds = announceBounds;
    this.announcer = announcer;
    registrants.clear();
    var scheduler = Bukkit.getScheduler();
    timeoutTask = scheduler.runTaskLater(owner, this::finish, durationSeconds * 20);
    startedMillis = System.currentTimeMillis();
    timerTask = scheduler.runTaskTimer(owner, this::tick, 0, 20);
    countdownTask = scheduler.runTaskLater(owner, this::startCountdown, (durationSeconds - 3) * 20);
    for (var playerTracking : participants.values()) {
      var player = playerTracking.get();
      player.setBedSpawnLocation(player.getLocation(), true);
      player.setGameMode(GameMode.ADVENTURE);
      AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
      if (maxHealth != null) {
        player.setHealth(maxHealth.getValue());
      }
      player.setFoodLevel(20);
      player.setGameMode(GameMode.ADVENTURE);
    }
    updateBars();
  }

  void cancel() {
    if (countdown != null) {
      countdown.cancel();
      countdown = null;
    }
    cleanup();
  }

  void cleanup() {
    timeoutTask.cancel();
    timerTask.cancel();
    countdownTask.cancel();
    countdown = null;
    for (var bar : bars.values()) {
      bar.dispose();
    }
    bars.clear();
    for (var task : tridentCooldownTask.values()) {
      task.cancel();
    }
    tridentCooldownTask.clear();
    for (var playerTracking : participants.values()) {
      Cloakroom.shared.restore(playerTracking.get());
    }
  }

  private @Nullable TeamColor playerColor(Player player) {
    for (var entry : participants.entrySet()) {
      if (entry.getValue().get() == player) {
        return entry.getKey();
      }
    }
    return null;
  }

  void onPlayerMove(Player player) {
    if (!player.isOnGround()) {
      return;
    }
    var score = ScoreFromAltitude(player);
    if (score < 200) {
      return;
    }
    for (var entry : participants.entrySet()) {
      if (entry.getValue().get() == player) {
        var color = entry.getKey();
        if (goaledMillis.containsKey(color)) {
          return;
        }
        goal(color, player);
        return;
      }
    }
  }

  void onPlayerInteract(PlayerInteractEvent e) {
    var player = e.getPlayer();
    switch (e.getAction()) {
      case RIGHT_CLICK_BLOCK -> {
        var block = e.getClickedBlock();
        var material = e.getMaterial();
        if (block == null) {
          if (material == Material.RED_BED) {
            var color = playerColor(player);
            if (color != null) {
              onUseBed(player, color);
            }
          }
          return;
        }
        if (!(block.getState() instanceof Bed)) {
          if (material == Material.RED_BED) {
            var color = playerColor(player);
            if (color != null) {
              onUseBed(player, color);
            }
          }
          return;
        }
        var location = block.getLocation();
        var color = playerColor(player);
        if (color == null) {
          return;
        }
        e.setCancelled(true);
        onClickBed(player, color, location);
      }
      case RIGHT_CLICK_AIR -> {
        var material = e.getMaterial();
        if (material == Material.RED_BED) {
          var color = playerColor(player);
          if (color != null) {
            onUseBed(player, color);
          }
        }
      }
    }
  }

  void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
    var player = e.getPlayer();
    if (playerColor(player) == null) {
      return;
    }
    if (!e.isSneaking() || player.isOnGround()) {
      player.removePotionEffect(PotionEffectType.SLOW_FALLING);
    } else {
      var effect = new PotionEffect(
        PotionEffectType.SLOW_FALLING,
        (int) durationSeconds * 20,
        1,
        false,
        false
      );
      effect.apply(player);
    }
  }

  void onPlayerRiptide(PlayerRiptideEvent e) {
    var player = e.getPlayer();
    if (playerColor(player) == null) {
      return;
    }
    var used = e.getItem();
    used.editMeta(Damageable.class, damageable -> {
      damageable.setDamage(0);
    });
    if (!IsStrongItem(used)) {
      return;
    }

    int delay = 8 * 20;
    player.setCooldown(Material.CLAY_BALL, delay);
    player.setCooldown(Material.TRIDENT, 2 * 20);
    var inventory = player.getInventory();
    var index = -1;
    for (int i = 0; i < inventory.getSize(); i++) {
      var item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      if (IsStrongItem(item)) {
        inventory.clear(i);
        if (index < 0) {
          index = i;
        }
      }
    }
    if (index >= 0) {
      var clayBall = ItemBuilder.For(Material.CLAY_BALL)
        .amount(1)
        .customTag(itemTag)
        .customTag(itemTagStrong)
        .flags(ItemFlag.HIDE_ATTRIBUTES)
        .displayName(text("HoloUp用トライデント（強）", DARK_GRAY))
        .build();
      inventory.setItem(index, clayBall);
      var scheduler = Bukkit.getScheduler();
      var task = scheduler.runTaskLater(owner, () -> {
        RecoverStrongTrident(player);
        tridentCooldownTask.remove(player);
      }, delay);
      tridentCooldownTask.put(player, task);
    }
  }

  void onPlayerLeave(Player player) {
    var color = playerColor(player);
    if (color == null) {
      return;
    }
    broadcast(prefix
      .append(text(String.format("%sが棄権しました", player.getName()), WHITE)));

    participants.remove(color);
    goaledMillis.remove(color);
    clearedCheckpoint.remove(color);

    var bar = bars.get(color);
    if (bar != null) {
      bar.dispose();
      bars.remove(color);
    }

    var task = tridentCooldownTask.get(player);
    if (task != null) {
      task.cancel();
      tridentCooldownTask.remove(player);
    }

    if (participants.isEmpty()) {
      broadcast(prefix
        .append(text("ゲームを中断しました", RED)));
      delegate.raceDidFinish();
    }
  }

  private static void RecoverStrongTrident(Player player) {
    var inventory = player.getInventory();
    var index = -1;
    for (int i = 0; i < inventory.getSize(); i++) {
      var item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      if (IsStrongItem(item)) {
        inventory.clear(i);
        if (index < 0) {
          index = i;
        }
      }
    }
    if (index >= 0) {
      var strong = CreateStrongTrident();
      inventory.setItem(index, strong);
    }
  }

  private void onClickBed(Player player, TeamColor color, Location blockLocation) {
    // 136, 171, 203, 230
    // 35, 32, 27
    // 0, 35, 67, 94
    var index = (int) Math.floor((blockLocation.getBlockY() - y(136)) / 24.0) + 1;
    var cleared = clearedCheckpoint.get(color);
    if (cleared == null || cleared < index) {
      clearedCheckpoint.put(color, index);
      player.setBedSpawnLocation(blockLocation, true);
      player.playSound(blockLocation, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
      player.swingMainHand();
      delegate.raceDidDetectCheckpoint(color, player, index);
    }
  }

  private void onUseBed(Player player, TeamColor color) {
    if (player.isOnGround()) {
      return;
    }
    if (!clearedCheckpoint.containsKey(color)) {
      return;
    }
    var location = player.getBedSpawnLocation();
    if (location == null) {
      return;
    }
    player.teleport(location);
  }

  private void goal(TeamColor color, Player player) {
    goaledMillis.put(color, System.currentTimeMillis());
    delegate.raceDidDetectGoal(color, player);
    boolean ok = true;
    for (var c : participants.keySet()) {
      if (!goaledMillis.containsKey(c)) {
        ok = false;
        break;
      }
    }
    if (ok) {
      finish();
    }
  }

  private void finish() {
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    var title = Title.title(text("ゲームが終了しました！", GOLD), Component.empty(), times);
    Players.Within(world, announceBounds, (player) -> player.showTitle(title));

    timeoutTask.cancel();
    timerTask.cancel();
    countdownTask.cancel();
    if (countdown != null) {
      countdown.cancel();
    }

    broadcast(Component.empty());
    for (var color : TeamColor.all) {
      var playerTracking = participants.get(color);
      if (playerTracking == null) {
        continue;
      }
      broadcast(Component.empty()
        .appendSpace()
        .append(color.component())
      );
      var score = this.score(color);
      var line = Component.empty()
        .appendSpace()
        .append(text(String.format(" - %s", playerTracking.get().getName()), color.textColor))
        .append(text(String.format(" %dm", score), score >= 200 ? GOLD : WHITE));
      var goal = goaledMillis.get(color);
      if (goal != null) {
        //NOTE: 秒数の表示は本家にはない
        var result = (goal - startedMillis) / 1000.0;
        line = line.append(text(String.format(" (%.3f秒)", result), WHITE));
      }
      broadcast(line);
      broadcast(Component.empty());
    }
    delegate.raceDidFinish();
  }

  private void startCountdown() {
    if (countdown != null) {
      countdown.cancel();
    }
    var prefix = text("ゲーム終了まで", GREEN);
    countdown = new Countdown(owner, world, announceBounds, prefix, WHITE, Component.empty(), 3, this::finish);
  }

  private void tick() {
    updateBars();
  }

  private void updateBars() {
    var elapsed = (System.currentTimeMillis() - startedMillis) / 1000;
    var remaining = Math.max(0, durationSeconds - elapsed);
    for (var color : TeamColor.all) {
      var playerTracking = participants.get(color);
      if (playerTracking == null) {
        continue;
      }
      var bar = bars.get(color);
      if (bar == null) {
        bar = new BossBar(owner, world, announceBounds, 0, color.barColor);
        bars.put(color, bar);
      }
      var score = this.score(color);
      var goal = goaledMillis.get(color);
      if (goal != null) {
        remaining = durationSeconds - (goal - startedMillis) / 1000;
      }
      bar.setProgress(score / 200.0f);
      var name = color.component()
        .appendSpace()
        .append(text(playerTracking.get().getName(), WHITE))
        .appendSpace()
        .append(text(String.format("%dm", score), GOLD))
        .append(text(String.format("/200m 残り時間: %d秒", remaining), WHITE));
      bar.setName(name);
    }
  }

  private int score(TeamColor color) {
    var playerTracking = participants.get(color);
    if (playerTracking == null) {
      return 0;
    }
    if (goaledMillis.containsKey(color)) {
      return 200;
    }
    return ScoreFromAltitude(playerTracking.get());
  }

  private static int ScoreFromAltitude(Player player) {
    return Math.min(Math.max(0, player.getLocation().getBlockY() - groundLevel), 200);
  }

  private void broadcast(Component message) {
    announcer.announce(message, announceBounds);
  }
}
