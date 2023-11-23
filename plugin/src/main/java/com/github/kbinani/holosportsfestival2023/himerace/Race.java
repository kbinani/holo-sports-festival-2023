package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.BossBar;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.announceBounds;

class Race {
  private final JavaPlugin owner;
  private final World world;
  final Map<TeamColor, Team> teams;
  final long startTimeMillis;
  private final Map<TeamColor, Long> finishTimeMillis = new HashMap<>();
  private final Map<TeamColor, BossBar> bossBars = new HashMap<>();
  private final BukkitTask timer;

  Race(JavaPlugin owner, World world, Map<TeamColor, Team> teams) {
    this.owner = owner;
    this.world = world;
    this.teams = new HashMap<>(teams);
    teams.clear();
    startTimeMillis = System.currentTimeMillis();
    timer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 0, 20);
    start();
  }

  void dispose() {
    timer.cancel();
    for (var bar : bossBars.values()) {
      bar.dispose();
    }
    bossBars.clear();
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

  private void start() {
    for (var color : TeamColor.all) {
      if (!teams.containsKey(color)) {
        continue;
      }
      var bar = new BossBar(owner, world, announceBounds, 0, color.barColor);
      bossBars.put(color, bar);
    }
  }

  private void tick() {
    updateBossBar();
  }

  private void updateBossBar() {
    for (var entry : bossBars.entrySet()) {
      var color = entry.getKey();
      var team = teams.get(color);
      if (team == null) {
        continue;
      }
      var name = team.getBossBarName();
      var bar = entry.getValue();
      bar.setName(name);
      bar.setProgress(team.getProgress());
    }
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
