package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class HealthDisplay {
  private final @Nonnull Player player;
  private final @Nonnull Entity healthDisplay;
  private @Nullable BukkitTask delayMountTask;

  public HealthDisplay(@Nonnull JavaPlugin owner, @Nonnull Player player, @Nonnull String scoreboardTag) {
    this.player = player;
    var location = player.getLocation();
    location.setY(location.getBlockY() - 8);
    this.healthDisplay = player.getWorld().spawn(location, AreaEffectCloud.class, (it) -> {
      it.setCustomNameVisible(false);
      it.setInvulnerable(true);
      it.setDuration(365 * 24 * 60 * 60 * 20);
      it.setRadius(0);
      it.addScoreboardTag(scoreboardTag);
    });
    this.delayMountTask = Bukkit.getScheduler().runTaskLater(owner, () -> {
      // particle が出現する瞬間が見えないように ride を遅らせる
      player.addPassenger(healthDisplay);
      healthDisplay.customName(createHealthDisplayComponent());
      healthDisplay.setCustomNameVisible(true);
      this.delayMountTask = null;
    }, 30);
  }

  public void dispose() {
    if (delayMountTask != null) {
      delayMountTask.cancel();
      delayMountTask = null;
    }
    player.removePassenger(healthDisplay);
    healthDisplay.remove();
  }

  public void update() {
    healthDisplay.customName(createHealthDisplayComponent());
  }

  private Component createHealthDisplayComponent() {
    var health = (int) Math.ceil(player.getHealth() * 0.5);
    var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    double maxHealth = 20;
    if (attribute != null) {
      maxHealth = attribute.getValue();
    }
    var max = (int) Math.ceil(maxHealth * 0.5);
    return text("♥".repeat(health), RED)
      .append(text("♡".repeat(max - health), WHITE));
  }
}
