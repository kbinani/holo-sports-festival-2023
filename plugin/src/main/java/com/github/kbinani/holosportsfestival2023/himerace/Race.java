package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.TeamColor;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class Race {
  final Map<TeamColor, Team> teams;
  final long startTimeMillis;
  private final Map<TeamColor, Long> finishTimeMillis = new HashMap<>();

  Race(Map<TeamColor, Team> teams) {
    this.teams = new HashMap<>(teams);
    teams.clear();
    startTimeMillis = System.currentTimeMillis();
  }

  boolean isEmpty() {
    int count = 0;
    for (var team : teams.values()) {
      count += team.size();
    }
    return count == 0;
  }

  boolean finish(TeamColor color) {
    if (!teams.containsKey(color)) {
      return false;
    }
    if (finishTimeMillis.containsKey(color)) {
      return false;
    }
    finishTimeMillis.put(color, System.currentTimeMillis());
    return true;
  }

  @Nullable
  private Long durationMillis(TeamColor color) {
    var goal = finishTimeMillis.get(color);
    if (goal == null) {
      return null;
    }
    return goal - startTimeMillis;
  }

  boolean isAllTeamsFinished() {
    for (var color : teams.keySet()) {
      if (!finishTimeMillis.containsKey(color)) {
        return false;
      }
    }
    return true;
  }

  void result(TriConsumer<Integer, TeamColor, Long> callback) {
    record Entry(TeamColor color, long durationMillis) {
    }
    var records = new ArrayList<Entry>();
    for (var it : teams.entrySet()) {
      var color = it.getKey();
      var durationMillis = this.durationMillis(color);
      if (durationMillis == null) {
        continue;
      }
      records.add(new Entry(color, durationMillis));
    }
    records.sort(Comparator.comparingLong(it -> it.durationMillis));
    int i = 0;
    for (var record : records) {
      callback.accept(i, record.color, record.durationMillis);
      i++;
    }
  }
}
