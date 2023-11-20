package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.*;

class Session {
  interface Delegate {
    void sessionDidFinish();
  }

  private static final int durationSec = 90;
  private static final int countdownSec = 3;
  private final Map<TeamColor, Point3i> respawnLocation = new HashMap<>();

  private final JavaPlugin owner;
  private final World world;
  private final BoundingBox announceBounds;
  private final Teams teams;
  private final Map<TeamColor, ArrayList<Unit>> participants;
  private final BukkitTask countdownStarter;
  private @Nullable Cancellable countdown;
  private final @Nonnull Delegate delegate;
  private final @Nonnull Map<TeamColor, Integer> leaderKillCount = new HashMap<>();

  Session(JavaPlugin owner, World world, BoundingBox announceBounds, @Nonnull Delegate delegate, @Nonnull Teams teams, Map<TeamColor, ArrayList<Unit>> participants) {
    this.owner = owner;
    this.world = world;
    this.announceBounds = announceBounds;
    this.teams = teams;
    this.participants = new HashMap<>(participants);
    var scheduler = Bukkit.getScheduler();
    this.countdownStarter = scheduler.runTaskLater(owner, this::startCountdown, (durationSec - countdownSec) * 20);
    this.delegate = delegate;
    respawnLocation.put(TeamColor.RED, pos(4, 80, 69));
    respawnLocation.put(TeamColor.WHITE, pos(-13, 80, 35));
    respawnLocation.put(TeamColor.YELLOW, pos(21, 80, 35));

    prepare();
  }

  void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof Player defence)) {
      return;
    }
    if (!(e.getDamager() instanceof Player offence)) {
      return;
    }
    var defenceUnit = getUnitByAttacker(defence);
    if (defenceUnit == null) {
      return;
    }
    var offenceUnit = getUnitByAttacker(offence);
    if (offenceUnit == null) {
      return;
    }
    e.setCancelled(true);
    var equipment = offence.getEquipment();
    var item = equipment.getItemInMainHand();
    if (item.getType() != Material.WOODEN_SWORD) {
      return;
    }
    var meta = item.getItemMeta();
    if (meta == null) {
      return;
    }
    var store = meta.getPersistentDataContainer();
    if (!store.has(NamespacedKey.minecraft(itemTag), PersistentDataType.BYTE)) {
      return;
    }
    if (defenceUnit.damagedBy(offence)) {
      if (defenceUnit.isLeader) {
        broadcast(prefix
          .append(offence.teamDisplayName())
          .append(Component.text(" --[大将撃破]-> ").color(Colors.orange))
          .append(defence.teamDisplayName())
        );
        var count = leaderKillCount.computeIfAbsent(offenceUnit.color, (c) -> 0);
        leaderKillCount.put(offenceUnit.color, count + 1);
      } else {
        broadcast(prefix
          .append(offence.teamDisplayName())
          .append(Component.text(" --[撃破]-> ").color(Colors.gray))
          .append(defence.teamDisplayName())
        );
      }
      offenceUnit.kill(defenceUnit.attacker);
      var location = respawnLocation.get(defenceUnit.color);
      if (location != null) {
        defenceUnit.teleport(location.toLocation(world));
      }
    }
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }

  private @Nullable Unit getUnitByAttacker(Player attacker) {
    for (var entry : participants.entrySet()) {
      for (var unit : entry.getValue()) {
        if (unit.attacker == attacker) {
          return unit;
        }
      }
    }
    return null;
  }

  private void prepare() {
    for (var units : this.participants.values()) {
      for (var unit : units) {
        unit.prepare();
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
        unit.clean();
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
