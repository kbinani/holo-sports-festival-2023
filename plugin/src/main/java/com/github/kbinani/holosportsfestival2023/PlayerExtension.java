package com.github.kbinani.holosportsfestival2023;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerExtension {
  public static void spread(Player player, BoundingBox box) {
    SpreadImpl(player.getWorld(), box, null, player);
  }

  public static void spread(Player player, BoundingBox box, float yaw) {
    SpreadImpl(player.getWorld(), box, yaw, player);
  }

  private static void SpreadImpl(World world, BoundingBox box, @Nullable Float yaw, Player player) {
    if (player.getWorld() != world) {
      return;
    }
    var random = ThreadLocalRandom.current();
    var x = box.getMinX();
    if (box.getMinX() < box.getMaxX()) {
      x = random.nextDouble(box.getMinX(), box.getMaxX());
    }
    var y = box.getMinY();
    if (box.getMinY() < box.getMaxY()) {
      y = random.nextDouble(box.getMinY(), box.getMaxY());
    }
    var z = box.getMinZ();
    if (box.getMinZ() < box.getMaxZ()) {
      z = random.nextDouble(box.getMinZ(), box.getMaxZ());
    }
    var location = player.getLocation();
    location.setX(x);
    location.setY(y);
    location.setZ(z);
    if (yaw != null) {
      location.setYaw(yaw);
    }
    player.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND);
  }
}
