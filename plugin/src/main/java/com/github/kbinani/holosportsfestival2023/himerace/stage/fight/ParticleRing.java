package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.itemTag;

class ParticleRing {
  private static final double sNormalOmega = 0.785;
  private static final int sParticlesPerRing = 90;
  private static final int sHitTestArrowsPerRing = 30;

  private final @Nonnull World world;
  private final @Nonnull Location center;
  private final double radius;
  private final @Nonnull Vector normal;
  private final @Nonnull Vector axis;
  private final long startTimeMillis;
  private final @Nonnull NamedTextColor color;
  private final double axisOmega;
  private final List<Arrow> hitTestArrows = new ArrayList<>();
  private final String scoreboardTag;

  ParticleRing(@Nonnull JavaPlugin owner, @Nonnull World world, @Nonnull Location center, double radius, @Nonnull String scoreboardTag, @Nonnull Vector normal, @Nonnull Vector axis, @Nonnull NamedTextColor color, double omega) {
    this.world = world;
    this.center = center;
    this.radius = radius;
    this.normal = normal.normalize();
    this.axis = axis.normalize();
    this.startTimeMillis = System.currentTimeMillis();
    this.color = color;
    this.axisOmega = omega;
    this.scoreboardTag = scoreboardTag;
  }

  void dispose() {
    for (var arrow : hitTestArrows) {
      arrow.remove();
    }
    hitTestArrows.clear();
  }

  boolean hitTest(Projectile projectile, int round) {
    var magnify = Math.pow(0.8, round);
    for (var arrow : hitTestArrows) {
      var bounds = arrow.getBoundingBox();
      var center = bounds.getCenter();
      var widthX = bounds.getWidthX() * magnify;
      var widthZ = bounds.getWidthZ() * magnify;
      var height = bounds.getHeight() * magnify;
      var modified = new BoundingBox(
        center.getX() - widthX * 0.5, center.getY() - height * 0.5, center.getZ() - widthZ * 0.5,
        center.getX() + widthX * 0.5, center.getY() + height * 0.5, center.getZ() + widthZ * 0.5
      );
      if (modified.overlaps(projectile.getBoundingBox())) {
        return true;
      }
    }
    return false;
  }

  void tick() {
    var elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    var angle = elapsed * axisOmega;
    var normal = this.normal.clone().rotateAroundAxis(axis, angle);
    for (var i = 0; i < sParticlesPerRing; i++) {
      var theta = elapsed * sNormalOmega + i * 2 * Math.PI / (double) sParticlesPerRing;
      var vector = axis.clone().multiply(radius).rotateAroundAxis(normal, theta);
      var location = center.clone().add(vector);
      var builder = new ParticleBuilder(Particle.REDSTONE);
      builder
        .allPlayers()
        .location(location)
        .count(1)
        .data(new Particle.DustOptions(ColorFromNamedTextColor(color), 0.5f))
        .force(true)
        .spawn();
    }
    var diff = sHitTestArrowsPerRing - hitTestArrows.size();
    if (diff > 0) {
      for (var i = 0; i < diff; i++) {
        var arrow = world.spawn(center, Arrow.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
          it.setGravity(false);
          it.setLifetimeTicks(Integer.MAX_VALUE);
          it.addScoreboardTag(scoreboardTag);
          it.addScoreboardTag(itemTag);
          it.setVisibleByDefault(false);
        });
        hitTestArrows.add(arrow);
      }
    }
    for (var i = 0; i < sHitTestArrowsPerRing; i++) {
      var theta = elapsed * sNormalOmega + i * 2 * Math.PI / (double) sHitTestArrowsPerRing;
      var vector = axis.clone().multiply(radius).rotateAroundAxis(normal, theta);
      var location = center.clone().add(vector);
      var arrow = hitTestArrows.get(i);
      var direction = vector.clone().multiply(-1);
      var horizontal = direction.clone().setY(0);
      var pitch = direction.angle(horizontal);
      var yaw = horizontal.angle(new Vector(0, 0, 1));
      arrow.teleport(new Location(world, location.x(), location.y(), location.z(), yaw, pitch));
    }
  }

  private static Color ColorFromNamedTextColor(NamedTextColor color) {
    return Color.fromRGB(color.red(), color.green(), color.blue());
  }
}
