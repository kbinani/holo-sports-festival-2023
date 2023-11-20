package com.github.kbinani.holosportsfestival2023;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Countdown implements Cancellable {
  private final List<BukkitTask> tasks = new ArrayList<>();

  public Countdown(
    JavaPlugin plugin,
    World world,
    BoundingBox bounds,
    Component titlePrefix,
    TextColor counterColor,
    Component subtitle,
    long seconds,
    Runnable then
  ) {
    var main = titlePrefix.append(Component.text(String.format(" %d...", seconds)).color(counterColor));
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    var title = Title.title(main, subtitle, times);
    Players.Within(world, bounds, (player) -> {
      player.clearTitle();
      player.showTitle(title);
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    });
    var scheduler = Bukkit.getScheduler();
    for (long i = seconds - 1; i >= 1; i--) {
      var t = Title.title(
        titlePrefix.append(Component.text(String.format(" %d...", i)).color(counterColor)),
        subtitle,
        times
      );
      var task = scheduler.runTaskLater(plugin, () -> {
        Players.Within(world, bounds, (player) -> {
          player.showTitle(t);
          player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        });
      }, 20 * (seconds - i));
      tasks.add(task);
    }
    var last = scheduler.runTaskLater(plugin, then, 20 * seconds);
    tasks.add(last);
  }

  @Override
  public void cancel() {
    for (var task : tasks) {
      task.cancel();
    }
  }
}
