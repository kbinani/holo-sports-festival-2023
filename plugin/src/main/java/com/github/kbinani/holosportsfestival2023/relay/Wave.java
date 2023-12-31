package com.github.kbinani.holosportsfestival2023.relay;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

class Wave {
  private static final double sSpeed = 3;

  private final @Nonnull World world;
  private final @Nonnull double[] yCandidates;
  private final double startX;
  private final double endX;
  private final double z;
  private final int[] yIndex;
  private final long startTimeMillis;
  private final ArrayList<Bubble> bubbles;
  private final double initX;
  private final int sign;
  private final double[] xLast;

  Wave(@Nonnull World world, String scoreboardTag, double startX, double endX, double initX, @Nonnull double[] yCandidates, double z) {
    this.world = world;
    this.yCandidates = yCandidates;
    if (startX < endX) {
      this.sign = 1;
      this.startX = startX;
      this.endX = endX;
      this.initX = initX;
    } else {
      this.sign = -1;
      this.startX = -startX;
      this.endX = -endX;
      this.initX = -initX;
    }
    this.z = z;
    var yIndex = ThreadLocalRandom.current().nextInt(yCandidates.length);
    this.yIndex = new int[]{
      yIndex, yIndex, yIndex, yIndex, yIndex
    };
    this.startTimeMillis = System.currentTimeMillis();
    this.bubbles = new ArrayList<Bubble>();
    bubbles.add(new Bubble(world, new Location(world, initX - 2, yCandidates[yIndex], z), scoreboardTag));
    bubbles.add(new Bubble(world, new Location(world, initX - 1, yCandidates[yIndex], z), scoreboardTag));
    bubbles.add(new Bubble(world, new Location(world, initX, yCandidates[yIndex], z), scoreboardTag));
    bubbles.add(new Bubble(world, new Location(world, initX + 1, yCandidates[yIndex], z), scoreboardTag));
    bubbles.add(new Bubble(world, new Location(world, initX + 2, yCandidates[yIndex], z), scoreboardTag));
    this.xLast = new double[]{this.initX - 2, this.initX - 1, this.initX, this.initX + 1, this.initX + 2};

    tick();
  }

  void dispose() {
    for (var bubble : bubbles) {
      bubble.dispose();
    }
    bubbles.clear();
  }

  void tick() {
    var t = (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    var distance = t * sSpeed;
    var width = endX - startX;

    BoundingBox bounds = null;

    for (int i = 0; i < 5; i++) {
      var bubble = bubbles.get(i);

      var nx = initX + distance - startX + 2 - i;
      var dx = nx - width * (int) Math.floor(nx / width);
      double x = startX + dx;

      if (x < this.xLast[i]) {
        if (i == 0) {
          yIndex[0] = ThreadLocalRandom.current().nextInt(yCandidates.length);
        } else {
          yIndex[i] = yIndex[0];
        }
      }
      this.xLast[i] = x;
      var y = yCandidates[yIndex[i]];
      bubble.teleport(new Location(world, x * sign, y, z));
      var b = bubble.getBoundingBox();
      if (b != null) {
        if (bounds == null) {
          bounds = bubble.getBoundingBox();
        } else {
          bounds.union(b);
        }
      }
    }
    if (bounds != null) {
      world.getNearbyEntities(bounds).forEach(it -> {
        if (!(it instanceof Player player)) {
          return;
        }
        if (player.getPortalCooldown() == 0 && hitTest(player)) {
          var velocity = player.getVelocity();
          velocity.setX(-0.25);
          player.setVelocity(velocity);
          player.setPortalCooldown(20);
        }
      });
    }
  }

  boolean hitTest(Player player) {
    for (var bubble : bubbles) {
      if (bubble.hitTest(player)) {
        return true;
      }
    }
    return false;
  }
}
