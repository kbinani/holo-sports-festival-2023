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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

class Illusioner implements IllusionerProjectile.Delegate {
  private static final long sStrongAttackCooltimeDurationMillis = 5 * 1000;

  final @Nonnull org.bukkit.entity.Illusioner entity;
  private final @Nonnull JavaPlugin owner;
  private final @Nonnull BukkitTask attackTimer;
  private final @Nonnull BoundingBox attackBounds;
  private final @Nonnull World world;
  private @Nullable BukkitTask attackMotionTimeoutTimer;
  private final @Nonnull List<IllusionerProjectile> projectiles = new ArrayList<>();
  private @Nullable Long strongAttackCooltimeMillis;
  private final int round;
  private final @Nonnull DefenceSphere defenceSphere;

  Illusioner(@Nonnull JavaPlugin owner, @Nonnull org.bukkit.entity.Illusioner entity, @Nonnull BoundingBox attackBounds, int round) {
    this.owner = owner;
    this.entity = entity;
    this.attackBounds = attackBounds;
    int period = 20;
    this.attackTimer = Bukkit.getScheduler().runTaskTimer(owner, this::attack, period, period);
    this.world = entity.getWorld();
    this.round = round;
    this.defenceSphere = new DefenceSphere(owner, entity.getLocation().add(0, 1, 0));
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
    defenceSphere.dispose();
  }

  private void attack() {
    if (entity.isDead()) {
      dispose();
      return;
    }
    var now = System.currentTimeMillis();
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
    }).collect(Collectors.toList());
    if (locations.isEmpty()) {
      return;
    }
    if (strongAttackCooltimeMillis == null || strongAttackCooltimeMillis < now) {
      this.strongAttackCooltimeMillis = now + sStrongAttackCooltimeDurationMillis;
      entity.setSpell(Spellcaster.Spell.WOLOLO);
      if (attackMotionTimeoutTimer != null) {
        attackMotionTimeoutTimer.cancel();
      }
      attackMotionTimeoutTimer = Bukkit.getScheduler().runTaskLater(owner, () -> {
        this.attackMotionTimeoutTimer = null;
        this.entity.setSpell(Spellcaster.Spell.NONE);
      }, 2 * 20);
      locations.forEach(it -> launchProjectile(it, true));
    } else {
      var extra = locations.size() * 3;
      for (var i = 0; i < extra; i++) {
        var x = ThreadLocalRandom.current().nextDouble(attackBounds.getMinX(), attackBounds.getMaxX());
        var z = ThreadLocalRandom.current().nextDouble(attackBounds.getMinZ(), attackBounds.getMaxZ());
        locations.add(new Location(world, x, attackBounds.getMinY(), z));
      }
      locations.forEach(it -> launchProjectile(it, false));
    }
  }

  private void launchProjectile(Location location, boolean strong) {
    var projectile = new IllusionerProjectile(owner, entity.getLocation().add(0, 2, 0), location, strong, round, this);
    this.projectiles.add(projectile);
  }

  @Override
  public void illusionerProjectileDead(IllusionerProjectile sender) {
    this.projectiles.remove(sender);
  }
}
