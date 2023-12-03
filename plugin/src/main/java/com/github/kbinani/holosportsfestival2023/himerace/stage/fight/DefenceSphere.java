package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.PI;

class DefenceSphere {
  private static final double sRadius = 2;

  private final @Nonnull World world;
  private final @Nonnull Location center;
  private final @Nonnull List<ParticleRing> rings;
  private final @Nonnull BukkitTask timer;
  private final Set<UUID> reflected = new HashSet<>();
  private final @Nonnull String scoreboardTag;

  DefenceSphere(@Nonnull JavaPlugin owner, @Nonnull World world, @Nonnull Location center, @Nonnull String scoreboardTag) {
    this.world = world;
    this.center = center;
    this.scoreboardTag = scoreboardTag;
    this.rings = Arrays.stream(new ParticleRing[]{
      new ParticleRing(
        owner, world, center, sRadius, scoreboardTag,
        new Vector(0.34, 0.642, -0.687), new Vector(0.34, 0.642, -0.687).rotateAroundAxis(new Vector(1, 0, 0), PI * 0.5),
        NamedTextColor.BLUE, 1.47
      ),
      new ParticleRing(
        owner, world, center, sRadius, scoreboardTag,
        new Vector(0.407, -0.330, -0.852), new Vector(0.407, -0.330, -0.852).rotateAroundAxis(new Vector(0, 1, 0), PI * 0.5),
        NamedTextColor.BLUE, -1.57
      ),
      new ParticleRing(
        owner, world, center, sRadius, scoreboardTag,
        new Vector(0.309, -0.907, 0.287), new Vector(0.309, -0.907, 0.287).rotateAroundAxis(new Vector(0, 0, 1), PI * 0.5),
        NamedTextColor.BLUE, 1.67
      ),
    }).collect(Collectors.toList());
    this.timer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 0, 1);
  }

  void dispose() {
    rings.clear();
    timer.cancel();
    for (var ring : rings) {
      ring.dispose();
    }
    rings.clear();
  }

  private void tick() {
    rings.forEach(ParticleRing::tick);
    var projectiles = world.getNearbyEntitiesByType(Projectile.class, center, sRadius + 1);
    var seen = new HashSet<UUID>();
    for (var projectile : projectiles) {
      if (projectile.getScoreboardTags().contains(scoreboardTag)) {
        continue;
      }
      var id = projectile.getUniqueId();
      seen.add(id);
      if (reflected.contains(id)) {
        continue;
      }
      for (var ring : rings) {
        if (ring.hitTest(projectile)) {
          var velocity = projectile.getVelocity();
          projectile.setVelocity(velocity.multiply(-1));
          reflected.add(id);
          break;
        }
      }
    }
    for (var id : new HashSet<>(reflected)) {
      if (!seen.contains(id)) {
        reflected.remove(id);
      }
    }
  }
}
