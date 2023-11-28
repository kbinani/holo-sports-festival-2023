package com.github.kbinani.holosportsfestival2023.himerace.stage;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractStage {
  protected final World world;
  protected final JavaPlugin owner;
  protected final Point3i origin;
  protected final int sizeX ;
  protected final int sizeZ;
  protected final BoundingBox bounds;
  protected boolean started = false;
  protected boolean finished = false;
  private @Nullable BukkitTask openGateTask;
  private Boolean gateOpened;

  protected AbstractStage(World world, JavaPlugin owner, Point3i origin, int sizeX, int sizeZ) {
    this.world = world;
    this.owner = owner;
    this.origin = origin;
    this.sizeX = sizeX;
    this.sizeZ = sizeZ;
    this.bounds = new BoundingBox(origin.x, origin.y, origin.z, origin.x + sizeX, 500, origin.z + sizeZ);
  }

  public final void start() {
    if (started || finished) {
      return;
    }
    setStarted(true);
  }

  public final void reset() {
    closeGate();
    onReset();
    finished = false;
    started = false;
  }

  public final void playerMove(PlayerMoveEvent e, Participation participation) {
    if (started && !finished) {
      onPlayerMove(e, participation);
    }
  }

  public final void playerInteract(PlayerInteractEvent e, Participation participation) {
    if (started && !finished) {
      onPlayerInteract(e, participation);
    }
  }

  public void tick() {
  }

  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {
  }

  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
  }

  public void onInventoryClick(InventoryClickEvent e, Participation participation) {
  }

  public void onPlayerItemConsume(PlayerItemConsumeEvent e, Participation participation) {
  }

  public void onBlockDropItem(BlockDropItemEvent e) {
  }

  public void onFurnaceSmelt(FurnaceSmeltEvent e) {
  }

  public void onEntityDeath(EntityDeathEvent e) {
  }

  protected abstract void onStart();

  protected abstract void onReset();

  protected abstract void onFinish();

  public abstract float getProgress();

  public abstract @Nonnull Component getActionBar(Role role);

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
    if (openGateTask != null) {
      openGateTask.cancel();
      openGateTask = null;
    }
    if (gateOpened != null && gateOpened) {
      return;
    }

    // origin: dark_oak_fence の一番北西下の位置
    var origin = this.origin.added(4, 0, 0);
    var server = Bukkit.getServer();
    var scheduler = server.getScheduler();
    var interval = 15;

    Editor.Fill(world, origin, origin.added(4, 0, 1), "air");
    Editor.Fill(world, origin.added(0, 4, 0), origin.added(4, 4, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(0, 4, 1), origin.added(4, 4, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
    world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);

    var task = scheduler.runTaskLater(owner, () -> {
      Editor.Fill(world, origin.added(0, 1, 0), origin.added(4, 1, 1), "air");
      Editor.Fill(world, origin.added(0, 5, 0), origin.added(4, 5, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(0, 5, 1), origin.added(4, 5, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
      world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
    }, interval);

    scheduler.runTaskLater(owner, () -> {
      if (task == openGateTask) {
        openGateTask = null;
      }
      if (task.isCancelled()) {
        return;
      }
      Editor.Fill(world, origin.added(0, 2, 0), origin.added(4, 2, 1), "air");
      Editor.Fill(world, origin.added(0, 6, 0), origin.added(0, 6, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=false]");
      Editor.Fill(world, origin.added(0, 6, 1), origin.added(0, 6, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=false]");
      Editor.Fill(world, origin.added(1, 6, 0), origin.added(3, 6, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(1, 6, 1), origin.added(3, 6, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(4, 6, 0), origin.added(4, 6, 0), "dark_oak_fence[east=false,north=false,south=true,waterlogged=false,west=true]");
      Editor.Fill(world, origin.added(4, 6, 1), origin.added(4, 6, 1), "dark_oak_fence[east=false,north=true,south=false,waterlogged=false,west=true]");
      world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
      gateOpened = true;
    }, interval * 2);

    openGateTask = task;
  }

  protected final void closeGate() {
    if (openGateTask != null) {
      openGateTask.cancel();
      openGateTask = null;
    }
    if (gateOpened != null && !gateOpened) {
      return;
    }
    var origin = this.origin.added(4, 0, 0);
    Editor.Fill(world, origin, origin.added(4, 2, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(0, 0, 1), origin.added(4, 2, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
    Editor.Fill(world, origin.added(1, 4, 0), origin.added(3, 6, 1), "barrier");
    Editor.Fill(world, origin.added(0, 4, 0), origin.added(0, 6, 1), "chain[axis=y]");
    Editor.Fill(world, origin.added(4, 4, 0), origin.added(4, 6, 1), "chain[axis=y]");
    world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
    gateOpened = false;
  }
}
