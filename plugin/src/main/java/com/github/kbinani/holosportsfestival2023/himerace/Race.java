package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.BossBar;
import com.github.kbinani.holosportsfestival2023.Players;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

class Race implements Team.Delegate {
  interface Delegate {
    void raceDidFinish();
  }

  private final JavaPlugin owner;
  private final World world;
  final Map<TeamColor, Team> teams;
  final long startTimeMillis;
  private final Map<TeamColor, Long> finishTimeMillis = new HashMap<>();
  private final Map<TeamColor, BossBar> bossBars = new HashMap<>();
  private final BukkitTask timer;
  private final @Nonnull Delegate delegate;

  Race(JavaPlugin owner, World world, Map<TeamColor, Team> teams, @Nonnull Delegate delegate) {
    this.owner = owner;
    this.world = world;
    for (var team : teams.values()) {
      team.delegate = this;
      team.onStart();
    }
    this.teams = new HashMap<>(teams);
    this.delegate = delegate;
    teams.clear();
    startTimeMillis = System.currentTimeMillis();
    timer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 0, 10);
    start();
  }

  @Override
  public void teamDidFinish(TeamColor color) {
    if (!finish(color)) {
      return;
    }
    broadcast(prefix
      .append(color.component())
      .append(text("がゴールしました！", WHITE)));
    if (!isAllTeamsFinished()) {
      return;
    }
    broadcast(prefix
      .append(text("ゲームが終了しました！", WHITE)));
    broadcast(Component.empty());
    var separator = "▪"; //TODO: この文字本当は何なのかが分からない
    broadcast(
      text(separator.repeat(32), GRAY)
        .appendSpace()
        .append(title)
        .appendSpace()
        .append(text(separator.repeat(32), GRAY))
    );
    broadcast(Component.empty());
    result((i, c, durationMillis) -> {
      long seconds = durationMillis / 1000;
      long millis = durationMillis - seconds * 1000;
      long minutes = seconds / 60;
      seconds = seconds - minutes * 60;
      broadcast(text(String.format(" - %d位 ", i + 1), AQUA)
        .append(c.component())
        .appendSpace()
        .append(text(String.format("(%d:%02d:%03d)", minutes, seconds, millis), c.textColor))
      );
    });
    broadcast(Component.empty());
    //TODO: ステージ内に人が残っていた場合
    delegate.raceDidFinish();
  }

  void dispose() {
    timer.cancel();
    for (var bar : bossBars.values()) {
      bar.dispose();
    }
    bossBars.clear();
    for (var team : teams.values()) {
      team.dispose();
    }
    teams.clear();
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
      var team = teams.get(color);
      if (team == null) {
        continue;
      }
      var bar = new BossBar(owner, world, announceBounds, 0, color.barColor);
      bossBars.put(color, bar);
    }
  }

  private void tick() {
    for (var team : teams.values()) {
      team.tick();
    }
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

  private boolean isAllTeamsFinished() {
    for (var color : teams.keySet()) {
      if (!finishTimeMillis.containsKey(color)) {
        return false;
      }
    }
    return true;
  }

  private void result(TriConsumer<Integer, TeamColor, Long> callback) {
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

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
    owner.getComponentLogger().info(message);
  }
}
