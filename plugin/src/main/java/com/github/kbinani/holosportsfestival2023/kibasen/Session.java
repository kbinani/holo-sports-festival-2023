package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Session {
  interface Delegate {
    void sessionDidFinish();
  }

  private static final int durationSec = 90;
  private static final int countdownSec = 3;

  private final JavaPlugin owner;
  private final World world;
  private final BoundingBox announceBounds;
  private final Teams teams;
  private final Map<TeamColor, ArrayList<Unit>> participants;
  private final BukkitTask countdownStarter;
  private @Nullable Cancellable countdown;
  private final @Nonnull Delegate delegate;

  Session(JavaPlugin owner, World world, BoundingBox announceBounds, @Nonnull Delegate delegate, @Nonnull Teams teams, Map<TeamColor, ArrayList<Unit>> participants) {
    this.owner = owner;
    this.world = world;
    this.announceBounds = announceBounds;
    this.teams = teams;
    this.participants = new HashMap<>(participants);
    var scheduler = Bukkit.getScheduler();
    this.countdownStarter = scheduler.runTaskLater(owner, this::startCountdown, (durationSec - countdownSec) * 20);
    this.delegate = delegate;
  }

  void abort() {
    this.countdownStarter.cancel();
    if (this.countdown != null) {
      this.countdown.cancel();
      this.countdown = null;
    }
    this.delegate.sessionDidFinish();
  }

  private void timeout() {
    this.countdown = null;
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    var title = Title.title(
      Component.text("ゲームが終了しました！").color(Colors.orange),
      Component.empty(),
      times
    );
    Players.Within(world, announceBounds, (player) -> {
      player.showTitle(title);
      player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    });
    this.delegate.sessionDidFinish();
  }

  private void startCountdown() {
    var title = Component.text("ゲーム終了まで").color(Colors.lime);
    this.countdown = new Countdown(
      owner, world, announceBounds,
      title, Colors.white, Component.empty(),
      countdownSec, this::timeout
    );
  }
}
