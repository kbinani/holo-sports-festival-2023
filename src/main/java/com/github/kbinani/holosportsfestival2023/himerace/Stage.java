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
  protected boolean started = false;
  protected boolean finished = false;

  Stage(World world, JavaPlugin owner, Point3i origin) {
    this.world = world;
    this.owner = owner;
    this.origin = origin;
  }

  final void start() {
    if (started || finished) {
      return;
    }
    setStarted(true);
  }

  final void reset() {
    closeGate();
    onReset();
    finished = false;
    started = false;
  }

  final void playerMove(PlayerMoveEvent e, Participation participation) {
    if (started && !finished) {
      onPlayerMove(e, participation);
    }
  }

  final void playerInteract(PlayerInteractEvent e, Participation participation) {
    if (started && !finished) {
      onPlayerInteract(e, participation);
    }
  }

  protected abstract void onPlayerMove(PlayerMoveEvent e, Participation participation);

  protected abstract void onPlayerInteract(PlayerInteractEvent e, Participation participation);

  protected abstract void onStart();

  protected abstract void onReset();

  protected abstract void onFinish();

  protected final void setFinished(boolean v) {
    if (finished == v) {
      return;
    }
    finished = v;
    if (finished) {
      onFinish();
    }
  }

  protected final void setStarted(boolean v) {
    if (started == v) {
      return;
    }
    started = v;
    if (started) {
      openGate();
      onStart();
    }
  }

  protected final void openGate() {
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

  protected final void closeGate() {
    var origin = this.origin.added(4, 0, 0);
    Editor.Fill(world, origin, origin.added(4, 2, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(0, 0, 1), origin.added(4, 2, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(1, 4, 0), origin.added(3, 6, 1), "air");
    Editor.Fill(world, origin.added(0, 4, 0), origin.added(0, 6, 1), "chain[axis=y]");
    Editor.Fill(world, origin.added(4, 4, 0), origin.added(4, 6, 1), "chain[axis=y]");
    world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
  }
}
