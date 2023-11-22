package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.stream.StreamSupport;

public class BossBar {
  private final World world;
  private final BoundingBox bounds;
  private final net.kyori.adventure.bossbar.BossBar instance;
  private final BukkitTask timer;

  public BossBar(
    JavaPlugin owner,
    World world,
    BoundingBox bounds,
    float progress,
    net.kyori.adventure.bossbar.BossBar.Color color
  ) {
    this.world = world;
    this.bounds = bounds;
    this.instance = net.kyori.adventure.bossbar.BossBar.bossBar(
      Component.empty(), progress, color, net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_6);
    this.timer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 0, 20);
  }

  public void setName(Component name) {
    this.instance.name(name);
  }

  public void setProgress(float progress) {
    this.instance.progress(progress);
  }

  public void dispose() {
    timer.cancel();
    StreamSupport.stream(instance.viewers().spliterator(), false).toList().forEach(viewer -> {
      if (viewer instanceof Audience audience) {
        audience.hideBossBar(instance);
      }
    });
  }

  private void tick() {
    StreamSupport.stream(instance.viewers().spliterator(), false).toList().forEach(viewer -> {
      if (viewer instanceof Player player) {
        if (!bounds.contains(player.getLocation().toVector())) {
          player.hideBossBar(instance);
        }
      }
    });
    Players.Within(world, bounds, (player) -> {
      player.showBossBar(instance);
    });
  }
}
