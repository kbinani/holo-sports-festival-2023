package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

import static com.github.kbinani.holosportsfestival2023.relay.RelayEventListener.CreateBaton;
import static com.github.kbinani.holosportsfestival2023.relay.RelayEventListener.prefix;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

class Race {
  interface Delegate {
    void raceDidFinish();
  }

  private record Record(TeamColor color, long goalTimeMillis) {
  }

  private final @Nonnull JavaPlugin owner;
  private final @Nonnull World world;
  final @Nonnull Map<TeamColor, Team> teams;
  private final @Nonnull Point3i offset;
  private final BoundingBox batonPassAreaEven;
  private final BoundingBox batonPassAreaOdd;
  private final @Nonnull BoundingBox startingArea;
  private final @Nonnull BoundingBox startingAreaEven;
  private final @Nonnull BoundingBox startingAreaOdd;
  private final @Nonnull BoundingBox goalDetectionArae;
  private final @Nonnull Delegate delegate;
  private final @Nonnull BoundingBox announceBounds;
  private long startTimeMillis;
  private final List<Record> goals = new ArrayList<>();
  private final Map<TeamColor, BossBar> bars = new HashMap<>();
  private final @Nonnull Point3i safeSpot;
  private final @Nonnull UUID id = UUID.randomUUID();

  private Race(
    @Nonnull JavaPlugin owner,
    @Nonnull World world,
    @Nonnull Map<TeamColor, Team> teams,
    @Nonnull Point3i offset,
    @Nonnull BoundingBox announceBounds,
    @Nonnull Point3i safeSpot,
    @Nonnull Delegate delegate) //
  {
    this.owner = owner;
    this.world = world;
    this.offset = offset;
    this.delegate = delegate;
    this.announceBounds = announceBounds;
    this.safeSpot = safeSpot;
    this.teams = new HashMap<>(teams);
    batonPassAreaEven = new BoundingBox(x(-1), y(80), z(76), x(10), y(85), z(83));
    batonPassAreaOdd = new BoundingBox(x(-1), y(80), z(22), x(10), y(85), z(29));
    startingArea = new BoundingBox(x(4), y(80), z(76), x(5), y(80), z(83));
    startingAreaEven = new BoundingBox(x(5), y(80), z(76), x(6), y(80), z(79));
    startingAreaOdd = new BoundingBox(x(3), y(80), z(26), x(4), y(80), z(29));
    var count = 0;
    for (var entry : this.teams.entrySet()) {
      var color = entry.getKey();
      var team = entry.getValue();
      count = Math.max(count, team.getOrderLength());
      var bar = new BossBar(owner, world, announceBounds, 0, color.barColor);
      bars.put(color, bar);
      bar.setName(team.getBossBarName());
    }
    if (count % 2 == 0) {
      goalDetectionArae = new BoundingBox(x(4) + 0.5, y(80), z(76), x(10), y(85), z(83));
    } else {
      goalDetectionArae = new BoundingBox(x(-1), y(80), z(22), x(4) + 0.5, y(85), z(29));
    }
    teams.clear();
  }

  String getBreadId() {
    return id.toString();
  }

  void teleportAll(Location location) {
    for (var team : teams.values()) {
      team.players().forEach(player -> {
        player.teleport(location);
      });
    }
  }

  void prepare() {
    eachPlayers(RelayEventListener::ClearItem);
    for (var entry : teams.entrySet()) {
      var team = entry.getValue();
      var player = team.getAssignedPlayer(0);
      if (player != null) {
        Players.Distribute(world, getStartingArea(0), player);
      }
    }
  }

  void start() {
    startTimeMillis = System.currentTimeMillis();
    for (var entry : teams.entrySet()) {
      var color = entry.getKey();
      var team = entry.getValue();
      team.currentRunningOrder = 0;
      var player = team.getAssignedPlayer(0);
      if (player != null) {
        player.getInventory().setItemInOffHand(CreateBaton(color));
        var next = team.getAssignedPlayer(1);
        if (next != null) {
          Players.Distribute(world, getStartingArea(1), next);
          notifyNextRunner(next, player);
        }
      }
    }
  }

  @Nonnull
  static Result<Race, Component> From(
    @Nonnull JavaPlugin owner,
    @Nonnull World world,
    @Nonnull Map<TeamColor, Team> teams,
    @Nonnull Point3i offset,
    @Nonnull BoundingBox announceBounds,
    @Nonnull Point3i safeSpot,
    @Nonnull Delegate delegate) //
  {
    int count = -1;
    int total = 0;
    for (var entry : teams.entrySet()) {
      var team = entry.getValue();
      var size = team.getOrderLength();
      if (team.getParticipantsCount() == 0) {
        continue;
      }
      if (team.getParticipantsCount() != team.getOrderLength()) {
        // https://youtu.be/uEpmE5WJPW8?t=5333
        return new Result<>(null, text("走順が正しく選択出来ていないチームがあるため、ゲームを開始できません。", RED));
      }
      if (count < 0) {
        count = size;
      } else if (count != size) {
        return new Result<>(null, text("参加者数が違うチームがあるため、ゲームを開始できません。", RED));
      }
      total += size;
    }
    if (count < 0) {
      return new Result<>(null, text("参加者がいません", RED));
    }

    var ids = new HashSet<UUID>();
    for (var team : teams.values()) {
      for (var player : team.players()) {
        ids.add(player.getUniqueId());
      }
    }
    if (ids.size() != total) {
      return new Result<>(null, text("複数のチームに重複して参加登録しているプレイヤーがいます", RED));
    }
    return new Result<>(new Race(owner, world, teams, offset, announceBounds, safeSpot, delegate), null);
  }

  private void updateBossBar(TeamColor color) {
    var bar = bars.get(color);
    if (bar == null) {
      return;
    }
    var team = teams.get(color);
    if (team == null) {
      return;
    }
    bar.setName(team.getBossBarName());
    bar.setProgress(team.getBossBarProgress());
  }

  Map<TeamColor, Team> abort() {
    var ret = new HashMap<>(this.teams);
    this.teams.clear();
    for (var bar : bars.values()) {
      bar.dispose();
    }
    bars.clear();
    return ret;
  }

  void dispose() {
    for (var team : teams.values()) {
      team.dispose();
    }
    teams.clear();
    for (var bar : bars.values()) {
      bar.dispose();
    }
    bars.clear();
  }

  void eachPlayers(Consumer<Player> cb) {
    for (var team : teams.values()) {
      for (var player : team.players()) {
        cb.accept(player);
      }
    }
  }

  void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof Player defender)) {
      return;
    }
    if (!(e.getDamager() instanceof Player attacker)) {
      return;
    }
    var defenderColor = getTeamColor(defender);
    if (defenderColor == null) {
      return;
    }
    var attackerColor = getTeamColor(attacker);
    if (attackerColor == null) {
      return;
    }
    if (defenderColor != attackerColor) {
      return;
    }
    var team = teams.get(attackerColor);
    if (team == null) {
      return;
    }
    var defenderOrder = team.getCurrentOrder(defender);
    if (defenderOrder == null) {
      return;
    }
    var attackerOrder = team.getCurrentOrder(attacker);
    if (attackerOrder == null) {
      return;
    }
    if (attackerOrder + 1 != defenderOrder) {
      return;
    }
    var batonPassArea = getBatonPassArea(attackerOrder);
    if (batonPassArea.contains(attacker.getLocation().toVector()) && batonPassArea.contains(defender.getLocation().toVector())) {
      attacker.sendMessage(prefix.append(text("バトンパス可能なエリアから外れています", RED)));
      defender.sendMessage(prefix.append(text("バトンパス可能なエリアから外れています", RED)));
      return;
    }
    var baton = removeBaton(attacker, attackerColor);
    if (baton == null) {
      return;
    }
    var inventory = defender.getInventory();
    inventory.setItemInOffHand(baton);
    team.currentRunningOrder = defenderOrder;
    updateBossBar(attackerColor);
    // https://youtu.be/0zFjBmflulU?t=11048
    // defender の次の走者を所定の位置に移動
    var next = team.getAssignedPlayer(defenderOrder + 1);
    if (next != null) {
      Players.Distribute(world, getStartingArea(defenderOrder + 1), next);
      notifyNextRunner(next, defender);
    }
    attacker.sendMessage(prefix.append(text("3秒後にスポーン地点にレポートされます！", WHITE)));
    Bukkit.getScheduler().runTaskLater(owner, () -> {
      attacker.teleport(safeSpot.toLocation(world));
    }, 3 * 20);
  }

  private @Nullable ItemStack removeBaton(Player player, TeamColor color) {
    var baton = CreateBaton(color);
    var equipment = player.getEquipment();
    var offHand = equipment.getItemInOffHand();
    var mainHand = equipment.getItemInMainHand();
    if (offHand.isSimilar(baton)) {
      equipment.setItemInOffHand(null);
      return baton;
    }
    if (mainHand.isSimilar(baton)) {
      equipment.setItemInMainHand(null);
      return baton;
    }
    return null;
  }

  private void notifyNextRunner(Player next, Player prev) {
    next.sendMessage(prefix
      .append(text(prev.getName(), GOLD))
      .append(text("からバトンを受け取って下さい！", WHITE)));
    next.sendMessage(prefix
      .append(text("Have ", WHITE))
      .append(text(prev.getName(), GOLD))
      .append(text(" pass you the baton!", WHITE))
    );
  }

  void onPlayerMove(PlayerMoveEvent e) {
    var player = e.getPlayer();
    if (!goalDetectionArae.contains(player.getLocation().toVector())) {
      return;
    }
    var color = getTeamColor(player);
    if (color == null) {
      return;
    }
    var team = teams.get(color);
    if (team == null) {
      return;
    }
    var order = team.getCurrentOrder(player);
    if (order == null) {
      return;
    }
    if (order != team.currentRunningOrder) {
      return;
    }
    var orderLength = team.getOrderLength();
    if (order + 1 != orderLength) {
      // 最終走者じゃない
      return;
    }
    var baton = removeBaton(player, color);
    if (baton == null) {
      return;
    }
    team.currentRunningOrder = orderLength;
    //TODO: 花火
    goals.add(new Record(color, System.currentTimeMillis()));
    broadcast(prefix.append(color.component()).append(text("がゴールしました！", WHITE)));
    updateBossBar(color);
    for (var t : teams.values()) {
      if (t.currentRunningOrder < t.getOrderLength()) {
        player.sendMessage(prefix.append(text("3秒後にスポーン地点にレポートされます！", WHITE)));
        Bukkit.getScheduler().runTaskLater(owner, () -> {
          player.teleport(safeSpot.toLocation(world));
        }, 3 * 20);
        return;
      }
    }
    broadcast(prefix.append(text("ゲームが終了しました！", WHITE)));
    var separator = "▪"; //TODO: この文字本当は何なのかが分からない
    broadcast(
      Component.empty()
        .appendSpace()
        .append(text(separator.repeat(32), GREEN))
        .appendSpace()
        .append(prefix)
        .append(text(separator.repeat(32), GREEN))
    );
    broadcast(Component.empty());
    for (var i = 0; i < goals.size(); i++) {
      var goal = goals.get(i);
      var durationMillis = goal.goalTimeMillis - startTimeMillis;
      long seconds = durationMillis / 1000;
      long millis = durationMillis - seconds * 1000;
      long minutes = seconds / 60;
      seconds = seconds - minutes * 60;
      broadcast(text(String.format(" - %d位 ", i + 1), AQUA)
        .append(goal.color.component())
        .append(text(String.format(" (%02d:%02d:%03d)", minutes, seconds, millis), goal.color.textColor)));
    }
    broadcast(Component.empty());
    delegate.raceDidFinish();
  }

  private BoundingBox getBatonPassArea(int orderFrom) {
    if (orderFrom % 2 == 0) {
      return batonPassAreaEven;
    } else {
      return batonPassAreaOdd;
    }
  }

  private BoundingBox getStartingArea(int order) {
    if (order == 0) {
      return startingArea;
    } else if (order % 2 == 0) {
      return startingAreaEven;
    } else {
      return startingAreaOdd;
    }
  }

  @Nullable
  TeamColor getTeamColor(Player player) {
    for (var entry : teams.entrySet()) {
      var team = entry.getValue();
      var color = entry.getKey();
      if (team.contains(player)) {
        return color;
      }
    }
    return null;
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }

  private int x(int x) {
    return x + offset.x;
  }

  private int y(int y) {
    return y + offset.y;
  }

  private int z(int z) {
    return z + offset.z;
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
