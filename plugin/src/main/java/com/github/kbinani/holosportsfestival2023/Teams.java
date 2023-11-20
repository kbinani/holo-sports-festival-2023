package  com.github.kbinani.holosportsfestival2023;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nonnull;

public class Teams {
  private final String prefix;

  public Teams(String prefix) {
    this.prefix = prefix;
  }

  public @Nonnull Team ensure(TeamColor color) {
    var server = Bukkit.getServer();
    var manager = server.getScoreboardManager();
    var scoreboard = manager.getMainScoreboard();
    var name = prefix + "_" + color.text;
    var team = scoreboard.getTeam(name);
    if (team == null) {
      team = scoreboard.registerNewTeam(name);
    }
    team.color(color.namedTextColor);
    team.setAllowFriendlyFire(false);
    return team;
  }
}
