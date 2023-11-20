package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.leaderRegistrationBarrel;

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
    for (var units : this.participants.values()) {
      for (var unit : units) {
        var view = unit.attacker.getOpenInventory();
        for (var index = 0; index < view.countSlots(); index++) {
          var inventory = view.getInventory(index);
          if (inventory == null) {
            continue;
          }
          var holder = inventory.getHolder();
          if (holder == null) {
            continue;
          }
          if (holder instanceof Barrel barrel) {
            var pos = new Point3i(barrel.getLocation());
            if (pos.equals(leaderRegistrationBarrel)) {
              view.close();
              break;
            }
          }
        }
      }
    }
  }

  void clear() {
    for (var entry : participants.entrySet()) {
      var color = entry.getKey();
      var team = teams.ensure(color);
      for (var unit : entry.getValue()) {
        team.removePlayer(unit.attacker);
        team.removePlayer(unit.vehicle);
        unit.vehicle.removePassenger(unit.attacker);
        unit.vehicle.removePassenger(unit.healthDisplay);
        unit.healthDisplay.remove();
        unit.attacker.removePotionEffect(PotionEffectType.GLOWING);
        unit.vehicle.removePotionEffect(PotionEffectType.GLOWING);
      }
    }
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
