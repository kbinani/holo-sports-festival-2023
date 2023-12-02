package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;

class IllusionerProjectile {
  interface Delegate {
    void illusionerProjectileDead(IllusionerProjectile sender);
  }

  private static final double sVelocity = 10;

  private final @Nonnull JavaPlugin owner;
  private final @Nonnull Location from;
  private final @Nonnull Location to;
  private final long startTimeMillis;
  private final @Nonnull BukkitTask timer;
  private int particles = 0;
  private final @Nonnull Delegate delegate;
  private final int numParticles;

  IllusionerProjectile(@Nonnull JavaPlugin owner, @Nonnull Location from, @Nonnull Location to, @Nonnull Delegate delegate) {
    this.owner = owner;
    this.from = from;
    this.to = to;
    this.startTimeMillis = System.currentTimeMillis();
    this.timer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 0, 1);
    this.delegate = delegate;
    var distance = to.distance(from);
    this.numParticles = (int) Math.ceil(distance / sVelocity * 20);
  }

  void dispose() {
    timer.cancel();
  }

  private void tick() {
    var now = System.currentTimeMillis();
    if (particles < numParticles) {
      var direction = to.toVector().subtract(from.toVector()).normalize();
      var elapsed = (now - startTimeMillis) / 1000.0;
      var vector = direction.multiply(sVelocity * elapsed);
      var builder = new ParticleBuilder(Particle.REDSTONE);
      builder
        .allPlayers()
        .color(ColorFromNamedTextColor(NamedTextColor.BLUE))
        .location(from.clone().add(vector))
        .count(8)
        .force(true)
        .spawn();
      particles++;
    } else {
      timer.cancel();
      delegate.illusionerProjectileDead(this);
    }
  }

  private static Color ColorFromNamedTextColor(NamedTextColor color) {
    return Color.fromRGB(color.red(), color.green(), color.blue());
  }
}
