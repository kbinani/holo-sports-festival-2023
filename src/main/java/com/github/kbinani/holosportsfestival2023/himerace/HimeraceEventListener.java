package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class HimeraceEventListener implements MiniGame {
  private final World world;
  private final JavaPlugin owner;
  private final Map<TeamColor, Level> levels = new HashMap<>();
  private final Map<TeamColor, Team> teams = new HashMap<>();
  private Status status = Status.IDLE;
  static final Component title = Component.text("[Himerace]").color(Colors.aqua);
  private final BoundingBox announceBounds;

  public HimeraceEventListener(World world, JavaPlugin owner) {
    this.world = world;
    this.owner = owner;
    this.levels.put(TeamColor.RED, new Level(world, owner, TeamColor.RED, pos(-23, -60, -16)));
    this.levels.put(TeamColor.WHITE, new Level(world, owner, TeamColor.WHITE, pos(-39, -60, -16)));
    this.levels.put(TeamColor.YELLOW, new Level(world, owner, TeamColor.YELLOW, pos(-55, -60, -16)));
    this.announceBounds = new BoundingBox(x(-75), -64, z(-36), x(5), 448, z(165));
  }

  private void setStatus(Status s) {
    if (s == status) {
      return;
    }
    status = s;
    switch (status) {
      case IDLE -> miniGameReset();
      case BLOCK_HEAD_STAGE -> {
        for (var level : levels.values()) {
          level.openGateBlockHead();
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
        pos(-12, -60, -20),
        Material.OAK_SIGN,
        8,
        title,
        Component.empty(),
        Component.empty(),
        Component.text("ゲームスタート").color(Colors.aqua)
    );
    Editor.StandingSign(
        world,
        pos(-13, -60, -20),
        Material.OAK_SIGN,
        8,
        title,
        Component.empty(),
        Component.empty(),
        Component.text("ゲームを中断する").color(Colors.aqua)
    );
    Editor.StandingSign(
        world,
        pos(-14, -60, -20),
        Material.OAK_SIGN,
        8,
        title,
        Component.empty(),
        Component.empty(),
        Component.text("エントリーリスト").color(Colors.aqua)
    );
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerMove(PlayerMoveEvent e) {
    Player player = e.getPlayer();
    var participation = getCurrentParticipation(player);
    if (participation == null) {
      return;
    }
    var level = levels.get(participation.color);
    level.onPlayerMove(e, participation, ensureTeam(participation.color));
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    Block block = e.getClickedBlock();
    var participation = getCurrentParticipation(player);
    if (participation != null) {
      var level = levels.get(participation.color);
      level.onPlayerInteract(e, participation, ensureTeam(participation.color));
    }
    if (block == null) {
      return;
    }
    Point3i location = new Point3i(block.getLocation());
    switch (e.getAction()) {
      case RIGHT_CLICK_BLOCK -> {
        if (location.equals(pos(-16, -60, -20))) {
          join(player, TeamColor.RED, Role.PRINCESS);
        } else if (location.equals(pos(-18, -60, -20))) {
          join(player, TeamColor.RED, Role.KNIGHT);
        } else if (location.equals(pos(-32, -60, -20))) {
          join(player, TeamColor.WHITE, Role.PRINCESS);
        } else if (location.equals(pos(-34, -60, -20))) {
          join(player, TeamColor.WHITE, Role.KNIGHT);
        } else if (location.equals(pos(-48, -60, -20))) {
          join(player, TeamColor.YELLOW, Role.PRINCESS);
        } else if (location.equals(pos(-50, -60, -20))) {
          join(player, TeamColor.YELLOW, Role.KNIGHT);
        } else if (location.equals(pos(-12, -60, -20))) {
          start();
        } else if (location.equals(pos(-13, -60, -20))) {
          stop();
        } else if (location.equals(pos(-14, -60, -20))) {
          announceParticipants();
        } else {
          return;
        }
        e.setCancelled(true);
      }
    }
  }

  private void announceParticipants() {
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
      var team = ensureTeam(color);
      broadcast(
          Component.text(" ")
              .append(color.component())
              .appendSpace()
              .append(Component.text(String.format(" (%d) ", team.size())).color(color.sign))
      );
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
      if (i < 2) {
        broadcast(Component.empty());
      }
    }
  }

  private void join(Player player, TeamColor color, Role role) {
    var team = ensureTeam(color);
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
    setStatus(Status.BLOCK_HEAD_STAGE);
  }

  private void stop() {
    setStatus(Status.IDLE);
    miniGameReset();
  }

  @Nullable
  private Participation getCurrentParticipation(Player player) {
    for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
      Role role = it.getValue().getCurrentRole(player);
      if (role == null) {
        continue;
      }
      return new Participation(it.getKey(), role);
    }
    return null;
  }

  @Nonnull
  private Team ensureTeam(TeamColor color) {
    Team team = teams.get(color);
    if (team == null) {
      team = new Team();
      teams.put(color, team);
    }
    return team;
  }

  private static int x(int x) {
    // 座標が間違っていたらここでオフセットする
    return x;
  }

  private static int y(int y) {
    // 座標が間違っていたらここでオフセットする
    return y;
  }

  private static int z(int z) {
    // 座標が間違っていたらここでオフセットする
    return z;
  }

  private static Point3i pos(int x, int y, int z) {
    return new Point3i(x(x), y(y), z(z));
  }
}
