package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

abstract class Stage {
  protected final World world;
  protected final JavaPlugin owner;
  protected final Point3i origin;

  Stage(World world, JavaPlugin owner, Point3i origin) {
    this.world = world;
    this.owner = owner;
    this.origin = origin;
  }

  abstract void stageStart();

  abstract void stageReset();

  abstract void stageOnPlayerMove(PlayerMoveEvent e, Participation participation);

  abstract void stageOnPlayerInteract(PlayerInteractEvent e, Participation participation);

  final void stageOpenGate() {
    // origin: dark_oak_fence の一番北西下の位置
    var origin = this.origin.added(4, 0, 0);
    var server = Bukkit.getServer();
    var scheduler = server.getScheduler();
    var interval = 15;

    Editor.Fill(world, origin, origin.added(4, 0, 1), "air");
    Editor.Fill(world, origin.added(0, 4, 0), origin.added(4, 4, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(0, 4, 1), origin.added(4, 4, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
    world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);

    scheduler.runTaskLater(owner, () -> {
      Editor.Fill(world, origin.added(0, 1, 0), origin.added(4, 1, 1), "air");
      Editor.Fill(world, origin.added(0, 5, 0), origin.added(4, 5, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(0, 5, 1), origin.added(4, 5, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
      world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
    }, interval);

    scheduler.runTaskLater(owner, () -> {
      Editor.Fill(world, origin.added(0, 2, 0), origin.added(4, 2, 1), "air");
      Editor.Fill(world, origin.added(0, 6, 0), origin.added(0, 6, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=false]");
      Editor.Fill(world, origin.added(0, 6, 1), origin.added(0, 6, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=false]");
      Editor.Fill(world, origin.added(1, 6, 0), origin.added(3, 6, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(1, 6, 1), origin.added(3, 6, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(4, 6, 0), origin.added(4, 6, 0), "dark_oak_fence[east=false,north=false,south=true,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(4, 6, 1), origin.added(4, 6, 1), "dark_oak_fence[east=false,north=true,south=false,waterlogged=false,west=true]");
      world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
    }, interval * 2);
  }

  final void stageCloseGate() {
    var origin = this.origin.added(4, 0, 0);
    Editor.Fill(world, origin, origin.added(4, 2, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(0, 0, 1), origin.added(4, 2, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(1, 4, 0), origin.added(3, 6, 1), "air");
    Editor.Fill(world, origin.added(0, 4, 0), origin.added(0, 6, 1), "chain[axis=y]");
    Editor.Fill(world, origin.added(4, 4, 0), origin.added(4, 6, 1), "chain[axis=y]");
    world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
  }
}
