package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spellcaster;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class Illusioner implements IllusionerProjectile.Delegate {
  final @Nonnull org.bukkit.entity.Illusioner entity;
  private final @Nonnull JavaPlugin owner;
  private final @Nonnull BukkitTask attackTimer;
  private final @Nonnull BoundingBox attackBounds;
  private final @Nonnull World world;
  private @Nullable BukkitTask attackMotionTimeoutTimer;
  private final @Nonnull List<IllusionerProjectile> projectiles = new ArrayList<>();

  Illusioner(JavaPlugin owner, org.bukkit.entity.Illusioner entity, BoundingBox attackBounds) {
    this.owner = owner;
    this.entity = entity;
    this.attackBounds = attackBounds;
    int period = 6 * 20;
    this.attackTimer = Bukkit.getScheduler().runTaskTimer(owner, this::attack, period, period);
    this.world = entity.getWorld();
  }

  void dispose() {
    entity.remove();
    attackTimer.cancel();
    if (attackMotionTimeoutTimer != null) {
      attackMotionTimeoutTimer.cancel();
      attackMotionTimeoutTimer = null;
    }
    for (var projectile : projectiles) {
      projectile.dispose();
    }
    projectiles.clear();
  }

  private void attack() {
    if (entity.isDead()) {
      attackTimer.cancel();
      return;
    }
    var locations = world.getNearbyEntities(attackBounds).stream().filter(it -> {
      if (!(it instanceof Player player)) {
        return false;
      }
      var gameMode = player.getGameMode();
      if (gameMode == GameMode.SPECTATOR || gameMode == GameMode.CREATIVE) {
        return false;
      }
      return player.getVehicle() == null;
    }).map(it -> {
      var location = it.getLocation();
      location.setY(attackBounds.getMinY());
      return location;
    }).toList();
    if (locations.isEmpty()) {
      return;
    }

    entity.setSpell(Spellcaster.Spell.WOLOLO);
    if (attackMotionTimeoutTimer != null) {
      attackMotionTimeoutTimer.cancel();
    }
    attackMotionTimeoutTimer = Bukkit.getScheduler().runTaskLater(owner, () -> {
      this.attackMotionTimeoutTimer = null;
      this.entity.setSpell(Spellcaster.Spell.NONE);
    }, 2 * 20);
    locations.forEach(this::launchProjectile);
  }

  private void launchProjectile(Location location) {
    var projectile = new IllusionerProjectile(owner, entity.getLocation().add(0, 2, 0), location, this);
    this.projectiles.add(projectile);
  }

  @Override
  public void illusionerProjectileDead(IllusionerProjectile sender) {
    this.projectiles.remove(sender);
  }
}
