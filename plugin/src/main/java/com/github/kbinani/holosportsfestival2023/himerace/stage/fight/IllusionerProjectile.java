package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;

class IllusionerProjectile {
  interface Delegate {
    void illusionerProjectileDead(IllusionerProjectile sender);
  }

  private static final double sVelocity = 10;

  private final @Nonnull Location from;
  private final @Nonnull Location to;
  private final long startTimeMillis;
  private final @Nonnull BukkitTask timer;
  private int particles = 0;
  private final @Nonnull Delegate delegate;
  private final int numParticles;
  private final boolean strong;

  IllusionerProjectile(@Nonnull JavaPlugin owner, @Nonnull Location from, @Nonnull Location to, boolean strong, @Nonnull Delegate delegate) {
    this.from = from;
    this.startTimeMillis = System.currentTimeMillis();
    this.timer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 0, 1);
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
    timer.cancel();
  }

  private void tick() {
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
      //TODO:
      timer.cancel();
      delegate.illusionerProjectileDead(this);
    } else {
      world.getNearbyPlayers(this.to, 0.5, 0.5, 0.5).forEach(player -> {
        var mode = player.getGameMode();
        if (mode != GameMode.ADVENTURE && mode != GameMode.SURVIVAL) {
          return;
        }
        player.damage(0.5);
      });
      timer.cancel();
      delegate.illusionerProjectileDead(this);
    }
  }

  private static Color ColorFromNamedTextColor(NamedTextColor color) {
    return Color.fromRGB(color.red(), color.green(), color.blue());
  }
}
