package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nonnull;

public class Teams {
  private final String prefix;
  private final boolean allowFriendlyFire;

  public Teams(String prefix, boolean allowFriendlyFire) {
    this.prefix = prefix;
    this.allowFriendlyFire = allowFriendlyFire;
  }

  public @Nonnull Team ensure(TeamColor color) {
    var name = prefix + "_" + color.text;
    return ensure(name, color.teamColor);
  }

  private @Nonnull Team ensure(String name, NamedTextColor teamColor) {
    var server = Bukkit.getServer();
    var manager = server.getScoreboardManager();
    var scoreboard = manager.getMainScoreboard();
    var team = scoreboard.getTeam(name);
    if (team == null) {
      team = scoreboard.registerNewTeam(name);
    }
    team.color(teamColor);
    team.setAllowFriendlyFire(allowFriendlyFire);
    return team;
  }
}
