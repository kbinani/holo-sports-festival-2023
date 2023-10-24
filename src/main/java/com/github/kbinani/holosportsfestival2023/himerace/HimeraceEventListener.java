package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.MiniGame;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

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

  public HimeraceEventListener(World world, JavaPlugin owner) {
    this.world = world;
    this.owner = owner;
    this.levels.put(TeamColor.RED, new Level(world, owner, TeamColor.RED, pos(-23, -60, -16)));
    this.levels.put(TeamColor.WHITE, new Level(world, owner, TeamColor.WHITE, pos(-39, -60, -16)));
    this.levels.put(TeamColor.YELLOW, new Level(world, owner, TeamColor.YELLOW, pos(-55, -60, -16)));
  }

  private void setStatus(Status s) {
    if (s == status) {
      return;
    }
  }

  @Override
  public void miniGameReset() {
    levels.forEach((color, level) -> {
      level.reset();
    });
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
    level.onPlayerMove(player, participation, ensureTeam(participation.color));
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    Block block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    Point3i location = new Point3i(block.getLocation());
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
    } else {
      return;
    }
    e.setCancelled(true);
  }

  private void join(Player player, TeamColor color, Role role) {
    var team = ensureTeam(color);
    team.add(player, role);
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

  private static Point3i pos(int x, int y, int z) {
    // 座標が間違っていたらここでオフセットする
    return new Point3i(x, y, z);
  }
}
