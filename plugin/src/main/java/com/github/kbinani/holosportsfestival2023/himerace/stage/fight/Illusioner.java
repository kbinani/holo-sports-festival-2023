package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spellcaster;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.PI;

class Illusioner implements IllusionerProjectile.Delegate {
  final @Nonnull org.bukkit.entity.Illusioner entity;
  private final @Nonnull JavaPlugin owner;
  private final @Nonnull BukkitTask attackTimer;
  private final @Nonnull BoundingBox attackBounds;
  private final @Nonnull World world;
  private @Nullable BukkitTask attackMotionTimeoutTimer;
  private final @Nonnull List<IllusionerProjectile> projectiles = new ArrayList<>();
  private final @Nonnull List<ParticleRing> rings;

  Illusioner(@Nonnull JavaPlugin owner, @Nonnull org.bukkit.entity.Illusioner entity, @Nonnull BoundingBox attackBounds) {
    this.owner = owner;
    this.entity = entity;
    this.attackBounds = attackBounds;
    int period = 20;
    this.attackTimer = Bukkit.getScheduler().runTaskTimer(owner, this::attack, period, period);
    this.world = entity.getWorld();
    var center = entity.getLocation().add(0, 1, 0);
    this.rings = Arrays.stream(new ParticleRing[]{
      new ParticleRing(
        owner, center,
        new Vector(0.34, 0.642, -0.687), new Vector(0.34, 0.642, -0.687).rotateAroundAxis(new Vector(1, 0, 0), PI * 0.5),
        NamedTextColor.BLUE, 1.47
      ),
      new ParticleRing(
        owner, center,
        new Vector(0.407, -0.330, -0.852), new Vector(0.407, -0.330, -0.852).rotateAroundAxis(new Vector(0, 1, 0), PI * 0.5),
        NamedTextColor.BLUE, -1.57
      ),
      new ParticleRing(
        owner, center,
        new Vector(0.309, -0.907, 0.287), new Vector(0.309, -0.907, 0.287).rotateAroundAxis(new Vector(0, 0, 1), PI * 0.5),
        NamedTextColor.BLUE, 1.67
      ),
    }).collect(Collectors.toList());
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
    for (var ring : rings) {
      ring.dispose();
    }
    rings.clear();
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
