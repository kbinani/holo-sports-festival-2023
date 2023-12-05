package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.Result;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

class Race {
  private final @Nonnull Map<TeamColor, Team> teams;

  private Race(Map<TeamColor, Team> teams) {
    this.teams = new HashMap<>(teams);
    teams.clear();
  }

  void teleport(Location location) {
    for (var team : teams.values()) {
      team.players().forEach(player -> {
        player.teleport(location);
      });
    }
  }

  @Nonnull
  static Result<Race, Component> From(Map<TeamColor, Team> teams) {
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
    return new Result<>(new Race(teams), null);
  }

  Map<TeamColor, Team> abort() {
    var ret = new HashMap<>(this.teams);
    this.teams.clear();
    return ret;
  }

  void dispose() {
    for (var team : teams.values()) {
      team.dispose();
    }
    teams.clear();
  }
}
