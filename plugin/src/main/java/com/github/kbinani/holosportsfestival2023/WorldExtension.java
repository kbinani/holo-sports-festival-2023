package com.github.kbinani.holosportsfestival2023;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.HashSet;

public class WorldExtension {
  private static final StackWalker sStackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  public static void fill(@Nonnull World world, Point3i from, Point3i to, String blockDataString) {
    Server server = Bukkit.getServer();
    BlockData blockData = null;
    try {
      blockData = server.createBlockData(blockDataString);
    } catch (Throwable e) {
      e.printStackTrace(System.err);
      return;
    }
    fill(world, from, to, blockData);
  }

  public static void fill(@Nonnull World world, Point3i from, Point3i to, Material material) {
    var blockData = material.createBlockData();
    fill(world, from, to, blockData);
  }

  public static void fill(@Nonnull World world, Point3i from, Point3i to, BlockData blockData) {
    Load(world, from, to, () -> {
      int x0 = Math.min(from.x, to.x);
      int y0 = Math.min(from.y, to.y);
      int z0 = Math.min(from.z, to.z);
      int x1 = Math.max(from.x, to.x);
      int y1 = Math.max(from.y, to.y);
      int z1 = Math.max(from.z, to.z);
      for (int y = y0; y <= y1; y++) {
        for (int z = z0; z <= z1; z++) {
          for (int x = x0; x <= x1; x++) {
            world.setBlockData(x, y, z, blockData);
          }
        }
      }
    });
  }

  public static void set(@Nonnull World world, Point3i pos, Material material) {
    fill(world, pos, pos, material);
  }

  public static void set(@Nonnull World world, Point3i pos, String blockDataString) {
    fill(world, pos, pos, blockDataString);
  }

  public static void set(@Nonnull World world, Point3i pos, BlockData blockData) {
    fill(world, pos, pos, blockData);
  }

  private static void Load(@Nonnull World world, Point3i from, Point3i to, Runnable callback) {
    final Class<?> clazz = sStackWalker.getCallerClass();
    final var plugin = JavaPlugin.getProvidingPlugin(clazz);

    int cx0 = from.x >> 4;
    int cz0 = from.z >> 4;
    int cx1 = to.x >> 4;
    int cz1 = to.z >> 4;
    if (cx1 < cx0) {
      int t = cx0;
      cx0 = cx1;
      cx1 = t;
    }
    if (cz1 < cz0) {
      int t = cz0;
      cz0 = cz1;
      cz1 = t;
    }
    var tickets = new HashSet<Point2i>();
    for (int cx = cx0; cx <= cx1; cx++) {
      for (int cz = cz0; cz <= cz1; cz++) {
        if (!world.isChunkLoaded(cx, cz)) {
          world.loadChunk(cx, cz);
        }
        if (plugin.isEnabled()) {
          world.addPluginChunkTicket(cx, cz, plugin);
          tickets.add(new Point2i(cx, cz));
        }
      }
    }
    callback.run();
    for (var chunk : tickets) {
      world.removePluginChunkTicket(chunk.x, chunk.z, plugin);
    }
  }
}
