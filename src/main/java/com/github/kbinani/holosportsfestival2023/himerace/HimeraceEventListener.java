package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.MiniGame;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;

public class HimeraceEventListener implements MiniGame {
  private final World world;
  private final Map<TeamColor, Level> levels = new HashMap<>();

  public HimeraceEventListener(World world) {
    this.world = world;
    this.levels.put(TeamColor.RED, new Level(world, TeamColor.RED, new Point3i(-23, -60, -16)));
    this.levels.put(TeamColor.WHITE, new Level(world, TeamColor.WHITE, new Point3i(-39, -60, -16)));
    this.levels.put(TeamColor.YELLOW, new Level(world, TeamColor.YELLOW, new Point3i(-55, -60, -16)));
  }

  @Override
  public void miniGameReset() {
    levels.forEach((color, level) -> {
      level.reset();
    });
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent e) {
    for (var level : levels.values()) {
      level.debugOnPlayerMove(e);
    }
  }
}