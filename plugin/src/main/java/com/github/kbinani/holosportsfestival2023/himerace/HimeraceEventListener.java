package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class HimeraceEventListener implements MiniGame, Race.Delegate {
  private static final Point3i offset = new Point3i(0, 0, 0);
  public static final Component title = text("[Himerace]", AQUA);
  static final Component prefix = title.appendSpace();
  static final BoundingBox announceBounds = new BoundingBox(X(-152), Y(-64), Z(-81), X(-72), Y(448), Z(120));
  public static final String itemTag = "hololive_sports_festival_2023_himerace";

  private final World world;
  private final JavaPlugin owner;
  private final Map<TeamColor, Level> levels = new HashMap<>();
  private final Map<TeamColor, Team> teams = new HashMap<>();
  private Status status = Status.IDLE;
  private @Nullable Race race;
  private @Nullable Cancellable countdown;

  public HimeraceEventListener(World world, JavaPlugin owner, int[] mapIDs) {
    if (mapIDs.length < 3) {
      throw new RuntimeException();
    }
    this.world = world;
    this.owner = owner;
    this.levels.put(TeamColor.RED, new Level(world, owner, TeamColor.RED, Pos(-100, 80, -61), mapIDs[0]));
    this.levels.put(TeamColor.WHITE, new Level(world, owner, TeamColor.WHITE, Pos(-116, 80, -61), mapIDs[1]));
    this.levels.put(TeamColor.YELLOW, new Level(world, owner, TeamColor.YELLOW, Pos(-132, 80, -61), mapIDs[2]));
  }

  private void setStatus(Status s) {
    if (s == status) {
      return;
    }
    status = s;
    switch (status) {
      case IDLE -> {
        miniGameReset();
      }
      case COUNTDOWN -> {

      }
      case ACTIVE -> {
        if (this.race == null) {
          miniGameReset();
          status = Status.IDLE;
          return;
        }
        for (var color : this.race.teams.keySet()) {
          var level = levels.get(color);
          level.start();
        }
      }
    }
  }

  @Override
  public void miniGameReset() {
    levels.forEach((color, level) -> {
      level.reset();
    });
    for (var team : teams.values()) {
      team.dispose();
    }
    teams.clear();
    Editor.StandingSign(world, Pos(-89, 80, -65), Material.OAK_SIGN, 8,
      title, Component.empty(), Component.empty(), text("ゲームスタート", AQUA));
    Editor.StandingSign(world, Pos(-90, 80, -65), Material.OAK_SIGN, 8,
      title, Component.empty(), Component.empty(), text("ゲームを中断する", RED));
    Editor.StandingSign(world, Pos(-91, 80, -65), Material.OAK_SIGN, 8,
      title, Component.empty(), Component.empty(), text("エントリーリスト", GREEN));
    if (race != null) {
      race.dispose();
      race = null;
    }
    if (countdown != null) {
      countdown.cancel();
      countdown = null;
    }
    Kill.EntitiesByScoreboardTag(world, itemTag);
    Bukkit.getServer().getOnlinePlayers().forEach(it -> ClearItems(it, itemTag));
  }

  @Override
  public void miniGameClearItem(Player player) {
    ClearItems(player, itemTag);
  }

  static void ClearItems(Player player, String tag) {
    var inventory = player.getInventory();
    for (int i = 0; i < inventory.getSize(); i++) {
      var item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      if (ItemTag.HasByte(item, tag)) {
        inventory.clear(i);
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerMove(PlayerMoveEvent e) {
    if (status == Status.IDLE) {
      return;
    }
    Player player = e.getPlayer();
    var participation = getCurrentParticipation(player);
    if (participation == null) {
      return;
    }
    var level = levels.get(participation.color);
    level.onPlayerMove(e, participation);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    Block block = e.getClickedBlock();
    if (status == Status.IDLE) {
      if (block == null) {
        return;
      }
      if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
        return;
      }
      Point3i location = new Point3i(block.getLocation());
      if (location.equals(Pos(-93, 80, -65))) {
        onClickJoin(player, TeamColor.RED, Role.PRINCESS);
      } else if (location.equals(Pos(-95, 80, -65))) {
        onClickJoin(player, TeamColor.RED, Role.KNIGHT);
      } else if (location.equals(Pos(-109, 80, -65))) {
        onClickJoin(player, TeamColor.WHITE, Role.PRINCESS);
      } else if (location.equals(Pos(-111, 80, -65))) {
        onClickJoin(player, TeamColor.WHITE, Role.KNIGHT);
      } else if (location.equals(Pos(-125, 80, -65))) {
        onClickJoin(player, TeamColor.YELLOW, Role.PRINCESS);
      } else if (location.equals(Pos(-127, 80, -65))) {
        onClickJoin(player, TeamColor.YELLOW, Role.KNIGHT);
      } else if (location.equals(Pos(-89, 80, -65))) {
        startCountdown();
      } else if (location.equals(Pos(-90, 80, -65))) {
        stop();
      } else if (location.equals(Pos(-91, 80, -65))) {
        announceParticipants();
      } else {
        return;
      }
      e.setCancelled(true);
    } else {
      if (block != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && Pos(-90, 80, -65).equals(new Point3i(block.getLocation()))) {
        stop();
        return;
      }
      var participation = getCurrentParticipation(player);
      if (participation != null) {
        var level = levels.get(participation.color);
        level.onPlayerInteract(e, participation);
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onInventoryClick(InventoryClickEvent e) {
    if (status == Status.IDLE) {
      return;
    }
    if (!(e.getWhoClicked() instanceof Player player)) {
      return;
    }
    var participation = getCurrentParticipation(player);
    if (participation == null) {
      return;
    }
    var level = levels.get(participation.color);
    level.onInventoryClick(e, participation);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    var player = e.getPlayer();
    var participation = getCurrentParticipation(player);
    if (participation == null) {
      return;
    }
    var level = levels.get(participation.color);
    level.onPlayerItemConsume(e, participation);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityDeathEvent(EntityDeathEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    for (var level : levels.values()) {
      level.onEntityDeath(e);
    }
  }

  private void announceParticipants() {
    if (status != Status.IDLE) {
      return;
    }
    broadcast(
      text("----------", GREEN)
        .appendSpace()
        .append(title)
        .appendSpace()
        .append(text("エントリー者", AQUA))
        .appendSpace()
        .append(text("----------", GREEN))
    );
    for (int i = 0; i < TeamColor.all.length; i++) {
      var color = TeamColor.all[i];
      var team = teams.get(color);
      broadcast(
        text(" ")
          .append(color.component())
          .appendSpace()
          .append(text(String.format(" (%d) ", team == null ? 0 : team.size()), color.textColor))
      );
      if (team != null) {
        var princess = team.getPrincess();
        if (princess != null) {
          broadcast(
            text(String.format("  - [姫] %s", princess.getName()), RED)
          );
        }
        for (var knight : team.getKnights()) {
          broadcast(
            text(String.format("  - %s", knight.getName()), RED)
          );
        }
      }
      if (i < 2) {
        broadcast(Component.empty());
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onBlockDropItem(BlockDropItemEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    for (var level : levels.values()) {
      level.onBlockDropItem(e);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onFurnaceSmelt(FurnaceSmeltEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    for (var level : levels.values()) {
      level.onFurnaceSmelt(e);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntitySpawn(EntitySpawnEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    for (var level : levels.values()) {
      level.onEntitySpawn(e);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerDeath(PlayerDeathEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    var player = e.getPlayer();
    var participation = getCurrentParticipation(player);
    if (participation == null) {
      return;
    }
    var level = levels.get(participation.color);
    level.onPlayerDeath(e, participation);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    if (!(e.getTarget() instanceof Player player)) {
      return;
    }
    if (!announceBounds.contains(e.getEntity().getLocation().toVector())) {
      return;
    }
    var participation = getCurrentParticipation(player);
    if (participation == null) {
      e.setCancelled(true);
    } else {
      var level = levels.get(participation.color);
      level.onEntityTargetLivingEntity(e, participation);
    }
  }

  private void onClickJoin(Player player, TeamColor color, Role role) {
    if (status != Status.IDLE) {
      return;
    }
    Team team = teams.get(color);
    if (team == null) {
      team = new Team(owner, color, levels.get(color));
      teams.put(color, team);
    }
    var current = getCurrentParticipation(player);
    if (current != null) {
      var inventory = player.getInventory();
      for (var i = 0; i < inventory.getSize(); i++) {
        var item = inventory.getItem(i);
        if (item == null) {
          continue;
        }
        if (ItemTag.HasByte(item, itemTag)) {
          inventory.clear(i);
        }
      }
      team.remove(player);
      player.sendMessage(prefix
        .append(text("エントリー登録を解除しました。", WHITE))
      );
      return;
    }
    if (team.add(player, role)) {
      broadcast(title
        .append(text(" " + player.getName() + " が", WHITE))
        .append(color.component())
        .append(text("の", WHITE))
        .append(role.component())
        .append(text("にエントリーしました。", WHITE))
      );
      var equipment = player.getEquipment();
      equipment.setHelmet(ItemBuilder.For(role == Role.PRINCESS ? Material.GOLDEN_HELMET : Material.IRON_HELMET)
        .customByteTag(itemTag)
        .customByteTag(Stage.FIGHT.tag)
        .build());
      equipment.setChestplate(ItemBuilder.For(role == Role.PRINCESS ? Material.GOLDEN_CHESTPLATE : Material.IRON_CHESTPLATE)
        .customByteTag(itemTag)
        .customByteTag(Stage.FIGHT.tag)
        .build());
      equipment.setLeggings(ItemBuilder.For(role == Role.PRINCESS ? Material.GOLDEN_LEGGINGS : Material.IRON_LEGGINGS)
        .customByteTag(itemTag)
        .customByteTag(Stage.FIGHT.tag)
        .build());
      equipment.setBoots(ItemBuilder.For(role == Role.PRINCESS ? Material.GOLDEN_BOOTS : Material.IRON_BOOTS)
        .customByteTag(itemTag)
        .customByteTag(Stage.FIGHT.tag)
        .build());
    }
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }

  private void startCountdown() {
    if (status != Status.IDLE) {
      return;
    }
    if (countdown != null) {
      countdown.cancel();
    }
    var titlePrefix = text("スタートまで", AQUA);
    var subtitle = text("姫護衛レース", GREEN);
    countdown = new Countdown(
      owner, world, announceBounds,
      titlePrefix, AQUA, subtitle,
      10, this::start
    );
    announceParticipants();
    setStatus(Status.COUNTDOWN);
  }

  private void start() {
    if (status != Status.COUNTDOWN) {
      return;
    }
    var race = new Race(owner, world, teams, this);
    if (race.isEmpty()) {
      return;
    }
    this.race = race;
    setStatus(Status.ACTIVE);
  }

  private void stop() {
    setStatus(Status.IDLE);
    miniGameReset();
  }

  @Nullable
  private Participation getCurrentParticipation(Player player) {
    if (status == Status.IDLE) {
      for (var it : teams.entrySet()) {
        var color = it.getKey();
        var team = it.getValue();
        Role role = team.getCurrentRole(player);
        if (role == null) {
          continue;
        }
        return new Participation(color, role, team);
      }
    } else if (race != null) {
      for (var it : race.teams.entrySet()) {
        var color = it.getKey();
        var team = it.getValue();
        Role role = team.getCurrentRole(player);
        if (role == null) {
          continue;
        }
        return new Participation(color, role, team);
      }
    }
    return null;
  }

  private static int X(int x) {
    return x + offset.x;
  }

  private static int Y(int y) {
    return y + offset.y;
  }

  private static int Z(int z) {
    return z + offset.z;
  }

  private static Point3i Pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }

  @Override
  public void raceDidFinish() {
    var race = this.race;
    if (race == null) {
      return;
    }
    setStatus(Status.IDLE);
  }
}
