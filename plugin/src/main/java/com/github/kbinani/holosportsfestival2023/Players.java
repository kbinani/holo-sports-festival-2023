package com.github.kbinani.holosportsfestival2023;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Players {
  private Players() {
  }

  public static void Within(World world, BoundingBox[] boxes, Consumer<Player> callback) {
    Server server = Bukkit.getServer();
    server.getOnlinePlayers().forEach(player -> {
      if (player.getWorld() != world) {
        return;
      }
      Vector location = player.getLocation().toVector();
      for (BoundingBox box : boxes) {
        if (box.contains(location)) {
          callback.accept(player);
          break;
        }
      }
    });
  }

  public static void Within(World world, BoundingBox box, Consumer<Player> callback) {
    Within(world, new BoundingBox[]{box}, callback);
  }

  public static void Distribute(World world, BoundingBox box, Player player) {
    DistributeImpl(world, box, null, player);
  }

  public static void Distribute(World world, BoundingBox box, float yaw, Player player) {
    DistributeImpl(world, box, yaw, player);
  }

  private static void DistributeImpl(World world, BoundingBox box, @Nullable Float yaw, Player player) {
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
