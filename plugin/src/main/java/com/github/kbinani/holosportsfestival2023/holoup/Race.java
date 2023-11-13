package com.github.kbinani.holosportsfestival2023.holoup;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class Race {
  interface Delegate {
    void raceDidFinish();
    void raceDidDetectGoal(TeamColor color, Player player);
  }

  private final Map<TeamColor, Player> participants;
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

  private static final long durationSeconds = 300;
  private static final int groundLevel = 100;

  Race(JavaPlugin owner, World world, BoundingBox announceBounds, Map<TeamColor, Player> registrants, Delegate delegate) {
    this.owner = owner;
    this.world = world;
    this.participants = new HashMap<>(registrants);
    this.delegate = delegate;
    this.announceBounds = announceBounds;
    registrants.clear();
    var scheduler = Bukkit.getScheduler();
    timeoutTask = scheduler.runTaskLater(owner, this::finish, durationSeconds * 20);
    startedMillis = System.currentTimeMillis();
    timerTask = scheduler.runTaskTimer(owner, this::tick, 0, 20);
    countdownTask = scheduler.runTaskLater(owner, this::startCountdown, (durationSeconds - 3) * 20);
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
      bar.viewers().forEach(viewer -> {
        if (viewer instanceof Player player) {
          player.hideBossBar(bar);
        }
      });
    }
    bars.clear();
  }

  void onPlayerMove(Player player) {
    if (!player.isOnGround()) {
      return;
    }
    var score = this.scoreFromAltitude(player);
    if (score < 200) {
      return;
    }
    for (var entry : participants.entrySet()) {
      if (entry.getValue() == player) {
        var color = entry.getKey();
        if (goaledMillis.containsKey(color)) {
          return;
        }
        goal(color, player);
        return;
      }
    }
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
    var title = Title.title(Component.text("ゲームが終了しました！").color(Colors.orange), Component.empty(), times);
    Players.Within(world, announceBounds, (player) -> player.showTitle(title));

    timeoutTask.cancel();
    timerTask.cancel();
    countdownTask.cancel();
    if (countdown != null) {
      countdown.cancel();
    }

    broadcast(Component.empty());
    for (var color : TeamColor.all) {
      var player = participants.get(color);
      if (player == null) {
        continue;
      }
      broadcast(Component.empty()
        .appendSpace()
        .append(color.component())
      );
      var score = this.score(color);
      broadcast(Component.empty()
        .appendSpace()
        .append(Component.text(String.format(" - %s", player.getName())).color(color.sign))
        .append(Component.text(String.format(" %dm", score)).color(score >= 200 ? Colors.orange : Colors.white))
      );
      broadcast(Component.empty());
    }
    delegate.raceDidFinish();
  }

  private void startCountdown() {
    if (countdown != null) {
      countdown.cancel();
    }
    var prefix = Component.text("ゲーム終了まで").color(Colors.lime);
    countdown = new Countdown(owner, world, announceBounds, prefix, Colors.white, Component.empty(), 3, this::finish);
  }

  private void tick() {
    updateBars();
  }

  private void updateBars() {
    var elapsed = (System.currentTimeMillis() - startedMillis) / 1000;
    var remaining = Math.max(0, durationSeconds - elapsed);
    for (var color : TeamColor.all) {
      var player = participants.get(color);
      if (player == null) {
        continue;
      }
      var bar = bars.get(color);
      if (bar == null) {
        bar = BossBar.bossBar(Component.empty(), 0, color.barColor, BossBar.Overlay.NOTCHED_6);
        bars.put(color, bar);
      }
      var score = this.score(color);
      var goal = goaledMillis.get(color);
      if (goal != null) {
        remaining = durationSeconds - (goal - startedMillis) / 1000;
      }
      bar.progress(score / 200.0f);
      var name = color.component()
        .appendSpace()
        .append(Component.text(player.getName()).color(Colors.white))
        .appendSpace()
        .append(Component.text(String.format("%dm", score)).color(Colors.orange))
        .append(Component.text(String.format("/200m 残り時間: %d秒", remaining)).color(Colors.white));
      bar.name(name);
    }
    for (var bar : bars.values()) {
      bar.viewers().forEach(viewer -> {
        if (viewer instanceof Player player) {
          if (!announceBounds.contains(player.getLocation().toVector())) {
            player.hideBossBar(bar);
          }
        }
      });
    }
    Players.Within(world, announceBounds, (player) -> {
      for (var bar : bars.values()) {
        player.showBossBar(bar);
      }
    });
  }

  private int score(TeamColor color) {
    var player = participants.get(color);
    if (player == null) {
      return 0;
    }
    if (goaledMillis.containsKey(color)) {
      return 200;
    }
    return scoreFromAltitude(player);
  }

  private int scoreFromAltitude(Player player) {
    return Math.min(Math.max(0, player.getLocation().getBlockY() - groundLevel), 200);
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }
}
