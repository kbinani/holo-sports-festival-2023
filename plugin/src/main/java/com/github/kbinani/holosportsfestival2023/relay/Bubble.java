package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.EntityTracking;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class Bubble {
  private static final int sWidthZ = 5;

  private final @Nonnull World world;
  private final List<EntityTracking<Entity>> clouds = new ArrayList<>();

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
      clouds.add(new EntityTracking<>(cloud));
    }
  }

  void dispose() {
    for (var cloud : clouds) {
      cloud.get().remove();
    }
    clouds.clear();
  }

  void teleport(Location location) {
    var index = 0;
    for (int i = 0; i < sWidthZ; i++) {
      var cloud = clouds.get(index).get();
      var x = location.getX();
      var z = location.getZ() - (double) (sWidthZ / 2) + i;
      cloud.teleport(new Location(world, x, location.getY(), z), TeleportFlag.EntityState.RETAIN_PASSENGERS);
      index++;
    }
  }

  boolean hitTest(Player player) {
    var playerBounds = player.getBoundingBox();
    for (var cloud : clouds) {
      if (cloud.get().getBoundingBox().overlaps(playerBounds)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  BoundingBox getBoundingBox() {
    BoundingBox box = null;
    for (var cloud : clouds) {
      if (box == null) {
        box = cloud.get().getBoundingBox();
      } else {
        box.union(cloud.get().getBoundingBox());
      }
    }
    return box;
  }
}
