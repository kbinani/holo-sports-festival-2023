package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.Math.PI;

class IllusionerProjectile {
  interface Delegate {
    void illusionerProjectileDead(IllusionerProjectile sender);
  }

  private static final double sVelocity = 10;
  private static final int ringEffectDurationTicks = 40;

  private final @Nonnull Location from;
  private final @Nonnull Location to;
  private final long startTimeMillis;
  private final @Nonnull BukkitTask trajectoryTimer;
  private @Nullable BukkitTask ringEffectTimeoutTimer;
  private int particles = 0;
  private final @Nonnull Delegate delegate;
  private final int numParticles;
  private final boolean strong;
  private final @Nonnull JavaPlugin owner;
  private int ringEffectTicks = 0;

  IllusionerProjectile(@Nonnull JavaPlugin owner, @Nonnull Location from, @Nonnull Location to, boolean strong, @Nonnull Delegate delegate) {
    this.owner = owner;
    this.from = from;
    this.startTimeMillis = System.currentTimeMillis();
    this.trajectoryTimer = Bukkit.getScheduler().runTaskTimer(owner, this::tickTrajectory, 0, 1);
    this.delegate = delegate;
    var world = from.getWorld();
    if (strong) {
      // 強攻撃はブロックを貫通しているように見える
      var distance = to.distance(from);
      this.numParticles = (int) Math.ceil(distance / sVelocity * 20);
      this.to = to;
    } else {
      // 弱攻撃はブロックで遮蔽できているように見える.
      var traced = world.rayTraceBlocks(from, to.toVector().subtract(from.toVector()), 48);
      if (traced == null) {
        this.numParticles = 0;
        this.to = to;
      } else {
        var hit = traced.getHitPosition();
        var distance = hit.distance(from.toVector());
        this.numParticles = (int) Math.ceil(distance / sVelocity * 20);
        this.to = new Location(world, hit.getX(), hit.getY(), hit.getZ());
      }
    }
    this.strong = strong;
  }

  void dispose() {
    trajectoryTimer.cancel();
    if (ringEffectTimeoutTimer != null) {
      ringEffectTimeoutTimer.cancel();
    }
  }

  private void tickTrajectory() {
    var now = System.currentTimeMillis();
    var world = from.getWorld();
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
    } else if (strong) {
      trajectoryTimer.cancel();
      ringEffectTimeoutTimer = Bukkit.getScheduler().runTaskTimer(owner, this::tickRing, 0, 1);
      delegate.illusionerProjectileDead(this);
    } else {
      world.getNearbyPlayers(this.to, 0.5, 0.5, 0.5).forEach(player -> {
        var mode = player.getGameMode();
        if (mode != GameMode.ADVENTURE && mode != GameMode.SURVIVAL) {
          return;
        }
        if (player.getVehicle() != null) {
          return;
        }
        player.damage(0.5);
      });
      trajectoryTimer.cancel();
      delegate.illusionerProjectileDead(this);
    }
  }

  private void tickRing() {
    ringEffectTicks++;

    var world = from.getWorld();
    double outerRadius = 2;
    int points = 45;
    var outer = new Vector(outerRadius, 0, 0);
    var particle = Particle.FALLING_DUST;
    double radian = -30.0 / 180.0 * PI;
    double delta = ringEffectTicks / (double) ringEffectDurationTicks;
    var blockData = Material.SNOW_BLOCK.createBlockData();
    for (var i = 0; i < points; i++) {
      var angle = 2 * PI / (double) points * i + radian * delta;
      var location = to.toVector().add(outer.clone().rotateAroundAxis(new Vector(0, 1, 0), angle));
      var builder = new ParticleBuilder(particle);
      builder
        .allPlayers()
        .location(location.toLocation(world))
        .data(blockData)
        .count(1)
        .force(true)
        .spawn();
    }
    double innerRadius = outerRadius;
    if (delta >= 0.5) {
      innerRadius = (1 - (ringEffectTicks - (double) ringEffectDurationTicks / 2) / (double) (ringEffectDurationTicks / 2)) * outerRadius;
    }
    var inner = new Vector(innerRadius, 0, 0);
    for (var i = 0; i < points; i++) {
      var angle = 2 * PI / (double) points * i + radian * delta;
      var location = to.toVector().add(inner.clone().rotateAroundAxis(new Vector(0, 1, 0), angle));
      var builder = new ParticleBuilder(particle);
      builder
        .allPlayers()
        .location(location.toLocation(world))
        .data(blockData)
        .count(1)
        .force(true)
        .spawn();
    }

    if (ringEffectTicks >= ringEffectDurationTicks) {
      if (ringEffectTimeoutTimer != null) {
        ringEffectTimeoutTimer.cancel();
      }
      ringEffectTimeoutTimer = null;

      world.spawn(to.clone().add(0, 0.5, 0), AreaEffectCloud.class, it -> {
        it.setDuration(30);
        it.setParticle(Particle.SPELL_WITCH);
        it.setRadius(1);
      });

      delegate.illusionerProjectileDead(this);
    }
  }

  private static Color ColorFromNamedTextColor(NamedTextColor color) {
    return Color.fromRGB(color.red(), color.green(), color.blue());
  }
}
