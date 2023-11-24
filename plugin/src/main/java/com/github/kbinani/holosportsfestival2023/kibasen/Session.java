package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

import static com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.*;

class Session {
  interface Delegate {
    void sessionDidFinish();
  }

  private static final int durationSec = 90;
  private static final int countdownSec = 3;
  static final UUID maxHealthModifierUUID = UUID.fromString("AEC6C8F7-F4DA-437F-A564-04B146972E7A");
  static final String maxHealthModifierName = "hololive_sports_festival_2023_kibasen";

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
  private final BukkitTask tick;
  private final BossBar bossBar;
  private final long startTimeMillis;

  Session(
    JavaPlugin owner,
    World world,
    BoundingBox announceBounds,
    @Nonnull Delegate delegate,
    @Nonnull Teams teams,
    Map<TeamColor, ArrayList<Unit>> participants
  ) {
    this.owner = owner;
    this.world = world;
    this.announceBounds = announceBounds;
    this.teams = teams;
    this.participants = new HashMap<>(participants);
    var scheduler = Bukkit.getScheduler();
    this.countdownStarter = scheduler.runTaskLater(owner, this::startCountdown, (durationSec - countdownSec) * 20);
    this.tick = scheduler.runTaskTimer(owner, this::tick, 0, 20);
    this.delegate = delegate;
    respawnLocation.put(TeamColor.RED, pos(4, 80, 69));
    respawnLocation.put(TeamColor.WHITE, pos(-13, 80, 35));
    respawnLocation.put(TeamColor.YELLOW, pos(21, 80, 35));
    this.bossBar = new BossBar(
      owner, world, announceBounds,
      0, net.kyori.adventure.bossbar.BossBar.Color.RED
    );
    this.startTimeMillis = System.currentTimeMillis();

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
          .append(Component.text(" --[大将撃破]-> ").color(NamedTextColor.GOLD))
          .append(defence.teamDisplayName())
        );
        var count = leaderKillCount.computeIfAbsent(offenceUnit.color, (c) -> 0);
        leaderKillCount.put(offenceUnit.color, count + 1);
      } else {
        broadcast(prefix
          .append(offence.teamDisplayName())
          .append(Component.text(" --[撃破]-> ").color(NamedTextColor.GRAY))
          .append(defence.teamDisplayName())
        );
      }
      offenceUnit.kill(defenceUnit.attacker);
      updateBossBarName();
      var location = respawnLocation.get(defenceUnit.color);
      if (location != null) {
        defenceUnit.teleport(location.toLocation(world));
      }
    }
  }

  void onEntityRegainHealth(EntityRegainHealthEvent e) {
    if (!(e.getEntity() instanceof Player player)) {
      return;
    }
    if (!isParticipant(player)) {
      return;
    }
    e.setCancelled(true);
  }

  void onEntityExhaustion(EntityExhaustionEvent e) {
    if (!(e.getEntity() instanceof Player player)) {
      return;
    }
    if (!isParticipant(player)) {
      return;
    }
    e.setCancelled(true);
    player.setFoodLevel(20);
  }

  private void updateBossBarName() {
    var name = Component.empty();
    for (int i = 0; i < TeamColor.all.length; i++) {
      var color = TeamColor.all[i];
      int count = 0;
      var units = participants.get(color);
      if (units != null) {
        for (var unit : units) {
          count += unit.getKills();
        }
      }
      name = name.append(color.component())
        .append(Component.text(String.format(": %d", count)).color(NamedTextColor.WHITE));
      if (i == 2) {
        break;
      }
      name = name.append(Component.text(" | ").color(NamedTextColor.GOLD));
    }
    bossBar.setName(name);
    bossBar.setProgress(getBossBarProgress());
  }

  private float getBossBarProgress() {
    float elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000.0f;
    return Math.min(Math.max(elapsed / (durationSec), 0.0f), 1.0f);
  }

  private void tick() {
    for (var units : participants.values()) {
      for (var unit : units) {
        unit.tick();
      }
    }
    bossBar.setProgress(getBossBarProgress());
    updateBossBarName();
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

  private boolean isParticipant(Player player) {
    for (var entry : participants.entrySet()) {
      for (var unit : entry.getValue()) {
        if (unit.attacker == player) {
          return true;
        }
        if (unit.vehicle == player) {
          return true;
        }
      }
    }
    return false;
  }

  private void prepare() {
    for (var entry : this.participants.entrySet()) {
      var color = entry.getKey();
      var respawn = respawnLocation.get(color);
      for (var unit : entry.getValue()) {
        unit.prepare();
        if (respawn != null) {
          unit.vehicle.teleport(
            respawn.toLocation(world),
            PlayerTeleportEvent.TeleportCause.COMMAND,
            TeleportFlag.EntityState.RETAIN_PASSENGERS
          );
        }
      }
    }
  }

  void clear(BoundingBox safeRespawnBounds) {
    this.bossBar.dispose();
    for (var entry : participants.entrySet()) {
      var color = entry.getKey();
      var team = teams.ensure(color);
      for (var unit : entry.getValue()) {
        team.removePlayer(unit.attacker);
        team.removePlayer(unit.vehicle);
        unit.clean();
        Players.Distribute(world, safeRespawnBounds, unit.vehicle);
        Players.Distribute(world, safeRespawnBounds, unit.attacker);
      }
    }
  }

  void abort() {
    this.countdownStarter.cancel();
    if (this.countdown != null) {
      this.countdown.cancel();
      this.countdown = null;
    }
    this.tick.cancel();
    this.delegate.sessionDidFinish();
  }

  private void timeout() {
    this.countdown = null;
    this.tick.cancel();
    var separator = "▪"; //TODO: この文字本当は何なのかが分からない
    broadcast(
      Component.empty()
        .appendSpace()
        .append(Component.text(separator.repeat(32)).color(NamedTextColor.GREEN))
        .appendSpace()
        .append(prefix)
        .append(Component.text(separator.repeat(32)).color(NamedTextColor.GREEN))
    );
    broadcast(Component.empty());
    class Record {
      final Unit unit;
      final int kills;
      int order;

      Record(Unit unit, int kills) {
        this.unit = unit;
        this.kills = kills;
        this.order = -1;
      }
    }
    var records = new ArrayList<Record>();
    for (var color : TeamColor.all) {
      var units = participants.get(color);
      if (units == null) {
        broadcast(Component.text(String.format(" %s 総キル数: 0", color.text)).color(color.textColor));
      } else {
        int count = 0;
        for (var unit : units) {
          count += unit.getKills();
          records.add(new Record(unit, unit.getKills()));
        }
        broadcast(Component.text(String.format(" %s 総キル数: %d", color.text, count)).color(color.textColor));
      }
      var leaderKills = leaderKillCount.getOrDefault(color, 0);
      broadcast(Component.text(String.format("  - 大将キル数: %d", leaderKills)).color(NamedTextColor.AQUA));
      broadcast(Component.empty());
    }
    records.sort(Comparator.comparingInt(record -> -record.kills));
    broadcast(Component.text(" ◆ 個人キルランキング").color(NamedTextColor.GOLD));
    int last = -1;
    int order = 1;
    int nextOrder = 1;
    for (var record : records) {
      if (last == record.kills) {
        nextOrder++;
      } else {
        order = nextOrder;
        nextOrder++;
        last = record.kills;
      }
      record.order = order;
    }
    for (var record : records) {
      broadcast(
        Component.text(
            String.format(
              "  #%d: %s & %s - %d kill%s",
              record.order,
              record.unit.attacker.getName(),
              record.unit.vehicle.getName(),
              record.kills,
              record.kills > 1 ? "s" : ""
            )
          )
          .color(record.unit.color.textColor)
      );
    }
    broadcast(Component.empty());
    this.delegate.sessionDidFinish();

    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    var title = Title.title(
      Component.text("ゲームが終了しました！").color(NamedTextColor.GOLD),
      Component.empty(),
      times
    );
    Players.Within(world, announceBounds, (player) -> {
      player.showTitle(title);
      player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    });
  }

  private void startCountdown() {
    var title = Component.text("ゲーム終了まで").color(NamedTextColor.GREEN);
    this.countdown = new Countdown(
      owner, world, announceBounds,
      title, NamedTextColor.WHITE, Component.empty(),
      countdownSec, this::timeout
    );
  }
}
