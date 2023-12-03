package com.github.kbinani.holosportsfestival2023.relay;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

class Bubble {
  private static final int sWidthZ = 5;

  private final @Nonnull World world;
  private final List<Entity> clouds = new ArrayList<>();

  Bubble(@Nonnull World world, @Nonnull Location center, @Nonnull String scoreboardTag) {
    this.world = world;
    for (int i = 0; i < sWidthZ; i++) {
      var x = center.getX() + 0.5;
      var z = center.getZ() - (double) sWidthZ / 2 + i + 0.5;
      var cloud = world.spawn(new Location(world, x, center.getY(), z), AreaEffectCloud.class, it -> {
        it.setDuration(365 * 24 * 60 * 60 * 20);
        it.setRadius(0.5f);
        it.setParticle(Particle.WATER_BUBBLE);
        it.addScoreboardTag(scoreboardTag);
      });
      clouds.add(cloud);
    }
  }

  void dispose() {
    for (var cloud : clouds) {
      cloud.remove();
    }
    clouds.clear();
  }

  void teleport(Location location) {
    var index = 0;
    for (int i = 0; i < sWidthZ; i++) {
      var cloud = clouds.get(index);
      var x = location.getX();
      var z = location.getZ() - (double) (sWidthZ / 2) + i;
      index++;
      cloud.teleport(new Location(world, x, location.getY(), z), TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }
  }
}
