package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Projectile;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

class ParticleRing {
  private static final double sNormalOmega = 0.785;
  private static final int sParticlesPerRing = 90;
  private static final int sHitTestArrowsPerRing = 30;
  private static final double sHitTestBaseRadius = 0.25;

  private final @Nonnull Location center;
  private final double radius;
  private final @Nonnull Vector normal;
  private final @Nonnull Vector axis;
  private final long startTimeMillis;
  private final @Nonnull NamedTextColor color;
  private final double axisOmega;
  private final List<Vector> hitTestPoints = new ArrayList<>();

  ParticleRing(
    @Nonnull Location center,
    double radius,
    @Nonnull Vector normal,
    @Nonnull Vector axis,
    @Nonnull NamedTextColor color,
    double omega //
  ) {
    this.center = center;
    this.radius = radius;
    this.normal = normal.normalize();
    this.axis = axis.normalize();
    this.startTimeMillis = System.currentTimeMillis();
    this.color = color;
    this.axisOmega = omega;
  }

  boolean hitTest(Projectile projectile, int round) {
    var radius = sHitTestBaseRadius * Math.pow(0.8, round);
    for (var center : hitTestPoints) {
      var bounds = new BoundingBox(
        center.getX() - radius, center.getY() - radius, center.getZ() - radius,
        center.getX() + radius, center.getY() + radius, center.getZ() + radius
      );
      if (bounds.overlaps(projectile.getBoundingBox())) {
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
    var diff = sHitTestArrowsPerRing - hitTestPoints.size();
    if (diff > 0) {
      for (var i = 0; i < diff; i++) {
        hitTestPoints.add(center.toVector());
      }
    }
    for (var i = 0; i < sHitTestArrowsPerRing; i++) {
      var theta = elapsed * sNormalOmega + i * 2 * Math.PI / (double) sHitTestArrowsPerRing;
      var vector = axis.clone().multiply(radius).rotateAroundAxis(normal, theta);
      var location = center.toVector().add(vector);
      hitTestPoints.set(i, location);
    }
  }

  private static Color ColorFromNamedTextColor(NamedTextColor color) {
    return Color.fromRGB(color.red(), color.green(), color.blue());
  }
}
