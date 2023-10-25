package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

interface Stage {
  void stageReset();

  void stageOnPlayerMove(PlayerMoveEvent e, Participation participation, Team team);

  void stageOnPlayerInteract(PlayerInteractEvent e, Participation participation, Team team);

  void stageOpenGate();

  void stageCloseGate();

  // origin: dark_oak_fence の一番北西下の位置
  static void OpenGate(JavaPlugin owner, World world, Point3i origin) {
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

  static void CloseGate(JavaPlugin owner, World world, Point3i origin) {
    var server = Bukkit.getServer();
    Editor.Fill(world, origin, origin.added(4, 2, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(0, 0, 1), origin.added(4, 2, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(1, 4, 0), origin.added(3, 6, 1), "air");
    Editor.Fill(world, origin.added(0, 4, 0), origin.added(0, 6, 1), "chain[axis=y]");
    Editor.Fill(world, origin.added(4, 4, 0), origin.added(4, 6, 1), "chain[axis=y]");
    world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
  }
}
