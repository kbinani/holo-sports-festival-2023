package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;

class ParticleRing {
  private static final double sNormalOmega = 0.785;
  private static final int sParticlesPerRing = 90;
  private static final double sRadius = 2;

  private final @Nonnull Location center;
  private final @Nonnull Vector normal;
  private final @Nonnull Vector axis;
  private final long startTimeMillis;
  private final @Nonnull NamedTextColor color;
  private final double axisOmega;

  ParticleRing(@Nonnull JavaPlugin owner, @Nonnull Location center, @Nonnull Vector normal, @Nonnull Vector axis, @Nonnull NamedTextColor color, double omega) {
    this.center = center;
    this.normal = normal.normalize();
    this.axis = axis.normalize();
    this.startTimeMillis = System.currentTimeMillis();
    this.color = color;
    this.axisOmega = omega;
  }

  void tick() {
    var elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    var angle = elapsed * axisOmega;
    var normal = this.normal.clone().rotateAroundAxis(axis, angle);
    for (var i = 0; i < sParticlesPerRing; i++) {
      var theta = elapsed * sNormalOmega + i * 2 * Math.PI / (double) sParticlesPerRing;
      var vector = axis.clone().multiply(sRadius).rotateAroundAxis(normal, theta);
      var builder = new ParticleBuilder(Particle.REDSTONE);
      builder
        .allPlayers()
        .location(center.clone().add(vector))
        .count(1)
        .data(new Particle.DustOptions(ColorFromNamedTextColor(color), 0.5f))
        .force(true)
        .spawn();
    }
  }

  private static Color ColorFromNamedTextColor(NamedTextColor color) {
    return Color.fromRGB(color.red(), color.green(), color.blue());
  }
}
