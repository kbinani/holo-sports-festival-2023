package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class HimeraceEventListener implements MiniGame, Level.Delegate {
  private final World world;
  private final JavaPlugin owner;
  private final Map<TeamColor, Level> levels = new HashMap<>();
  private final Map<TeamColor, Team> teams = new HashMap<>();
  private Status status = Status.IDLE;
  static final Component title = Component.text("[Himerace]").color(Colors.aqua);
  private final BoundingBox announceBounds;
  private @Nullable Race race;

  public HimeraceEventListener(World world, JavaPlugin owner, int[] mapIDs) {
    if (mapIDs.length < 3) {
      throw new RuntimeException();
    }
    this.world = world;
    this.owner = owner;
    this.levels.put(TeamColor.RED, new Level(world, owner, TeamColor.RED, pos(-100, 80, -61), mapIDs[0], this));
    this.levels.put(TeamColor.WHITE, new Level(world, owner, TeamColor.WHITE, pos(-116, 80, -61), mapIDs[1], this));
    this.levels.put(TeamColor.YELLOW, new Level(world, owner, TeamColor.YELLOW, pos(-132, 80, -61), mapIDs[2], this));
    this.announceBounds = new BoundingBox(-152, -64, -81, -72, 448, 120);
  }

  private void setStatus(Status s) {
    if (s == status) {
      return;
    }
    status = s;
    switch (status) {
      case IDLE -> {
        miniGameReset();
        race = null;
      }
      case CARRY_STAGE -> {
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
    teams.clear();
    Editor.StandingSign(
      world,
      pos(-89, 80, -65),
      Material.OAK_SIGN,
      8,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームスタート").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      pos(-90, 80, -65),
      Material.OAK_SIGN,
      8,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームを中断する").color(Colors.red)
    );
    Editor.StandingSign(
      world,
      pos(-91, 80, -65),
      Material.OAK_SIGN,
      8,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("エントリーリスト").color(Colors.lime)
    );
    race = null;
  }

  @Override
  public void miniGameClearItem(Player player) {
    //TODO:
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
      if (location.equals(pos(-93, 80, -65))) {
        join(player, TeamColor.RED, Role.PRINCESS);
      } else if (location.equals(pos(-95, 80, -65))) {
        join(player, TeamColor.RED, Role.KNIGHT);
      } else if (location.equals(pos(-109, 80, -65))) {
        join(player, TeamColor.WHITE, Role.PRINCESS);
      } else if (location.equals(pos(-111, 80, -65))) {
        join(player, TeamColor.WHITE, Role.KNIGHT);
      } else if (location.equals(pos(-125, 80, -65))) {
        join(player, TeamColor.YELLOW, Role.PRINCESS);
      } else if (location.equals(pos(-127, 80, -65))) {
        join(player, TeamColor.YELLOW, Role.KNIGHT);
      } else if (location.equals(pos(-89, 80, -65))) {
        start();
      } else if (location.equals(pos(-90, 80, -65))) {
        stop();
      } else if (location.equals(pos(-91, 80, -65))) {
        announceParticipants();
      } else {
        return;
      }
      e.setCancelled(true);
    } else {
      if (block != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && pos(-90, 80, -65).equals(new Point3i(block.getLocation()))) {
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

  private void announceParticipants() {
    if (status != Status.IDLE) {
      return;
    }
    broadcast(
      Component.text("----------").color(Colors.lime)
        .appendSpace()
        .append(title)
        .appendSpace()
        .append(Component.text("エントリー者").color(Colors.aqua))
        .appendSpace()
        .append(Component.text("----------").color(Colors.lime))
    );
    for (int i = 0; i < TeamColor.all.length; i++) {
      var color = TeamColor.all[i];
      var team = teams.get(color);
      broadcast(
        Component.text(" ")
          .append(color.component())
          .appendSpace()
          .append(Component.text(String.format(" (%d) ", team == null ? 0 : team.size())).color(color.sign))
      );
      if (team != null) {
        var princess = team.getPrincess();
        if (princess != null) {
          broadcast(
            Component.text(String.format("  - [姫] %s", princess.getName())).color(Colors.red)
          );
        }
        for (var knight : team.getKnights()) {
          broadcast(
            Component.text(String.format("  - %s", knight.getName())).color(Colors.red)
          );
        }
      }
      if (i < 2) {
        broadcast(Component.empty());
      }
    }
  }

  private void join(Player player, TeamColor color, Role role) {
    if (status != Status.IDLE) {
      return;
    }
    Team team = teams.get(color);
    if (team == null) {
      team = new Team();
      teams.put(color, team);
    }
    if (team.add(player, role)) {
      broadcast(title
        .append(Component.text(" " + player.getName() + " が").color(Colors.white))
        .append(color.component())
        .append(Component.text("の").color(Colors.white))
        .append(role.component())
        .append(Component.text("にエントリーしました。").color(Colors.white))
      );
    }
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }

  private void start() {
    if (status != Status.IDLE) {
      return;
    }
    var race = new Race(teams);
    if (race.isEmpty()) {
      return;
    }
    this.race = race;
    //TODO: カウントダウン
    setStatus(Status.CARRY_STAGE);
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

  private static Point3i pos(int x, int y, int z) {
    return new Point3i(x, y, z);
  }

  @Override
  public void levelDidFinish(TeamColor color) {
    var race = this.race;
    if (race == null) {
      return;
    }
    if (!race.finish(color)) {
      return;
    }
    broadcast(title
      .appendSpace()
      .append(color.component())
      .append(Component.text("がゴールしました！").color(Colors.white)));
    if (!race.isAllTeamsFinished()) {
      return;
    }
    broadcast(title
      .appendSpace()
      .append(Component.text("ゲームが終了しました！").color(Colors.white)));
    broadcast(Component.empty());
    var separator = "▪"; //TODO: この文字本当は何なのかが分からない
    broadcast(
      Component.text(separator.repeat(32)).color(Colors.lightgray)
        .appendSpace()
        .append(title)
        .appendSpace()
        .append(Component.text(separator.repeat(32)).color(Colors.lightgray))
    );
    broadcast(Component.empty());
    race.result((i, c, durationMillis) -> {
      long seconds = durationMillis / 1000;
      long millis = durationMillis - seconds * 1000;
      long minutes = seconds / 60;
      seconds = seconds - minutes * 60;
      broadcast(Component.text(String.format(" - %d位 ", i + 1)).color(Colors.aqua)
        .append(c.component())
        .appendSpace()
        .append(Component.text(String.format("(%d:%02d:%03d)", minutes, seconds, millis)).color(c.sign))
      );
    });
    broadcast(Component.empty());
    //TODO: ステージ内に人が残っていた場合
    setStatus(Status.IDLE);
  }
}
