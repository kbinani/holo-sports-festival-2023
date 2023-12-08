package com.github.kbinani.holosportsfestival2023.himerace.stage;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractStage {
  protected final World world;
  protected final JavaPlugin owner;
  protected final Point3i origin;
  protected final int sizeX;
  protected final int sizeZ;
  protected final BoundingBox bounds;
  protected boolean started = false;
  protected boolean finished = false;
  private @Nullable BukkitTask openGateTask;
  private @Nullable Integer gateState;

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
    onReset();
    finished = false;
    started = false;
    world.getNearbyEntities(bounds).forEach(it -> {
      if (it instanceof Arrow arrow) {
        arrow.remove();
      }
    });
  }

  public final void tick() {
    if (started && !finished) {
      onTick();
    }
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


  public final void inventoryClick(InventoryClickEvent e, Participation participation) {
    if (started && !finished) {
      onInventoryClick(e, participation);
    }
  }

  public final void playerItemConsume(PlayerItemConsumeEvent e, Participation participation) {
    if (started && !finished) {
      onPlayerItemConsume(e, participation);
    }
  }

  public final void blockDropItem(BlockDropItemEvent e) {
    if (started && !finished) {
      onBlockDropItem(e);
    }
  }

  public final void furnaceSmelt(FurnaceSmeltEvent e) {
    if (started && !finished) {
      onFurnaceSmelt(e);
    }
  }

  public final void entityDeath(EntityDeathEvent e) {
    if (started && !finished) {
      onEntityDeath(e);
    }
  }

  public final void entitySpawn(EntitySpawnEvent e) {
    if (started && !finished) {
      onEntitySpawn(e);
    }
  }

  public final void playerDeath(PlayerDeathEvent e, Participation participation) {
    if (started && !finished) {
      onPlayerDeath(e, participation);
    }
  }

  public final void entityTargetLivingEntity(EntityTargetLivingEntityEvent e, Participation participation) {
    if (started && !finished) {
      onEntityTargetLivingEntity(e, participation);
    }
  }

  public final void playerInteractEntity(PlayerInteractEntityEvent e, Role player, Role rightClicked) {
    if (started && !finished) {
      onPlayerInteractEntity(e, player, rightClicked);
    }
  }

  public final void entityDamageByEntity(EntityDamageByEntityEvent e) {
    if (started && !finished) {
      onEntityDamageByEntity(e);
    }
  }

  public final void entityDismount(EntityDismountEvent e, Participation participation) {
    if (started && !finished) {
      onEntityDismount(e, participation);
    }
  }

  public final void blockBreak(BlockBreakEvent e, Participation participation) {
    onBlockBreak(e, participation);
  }

  public final void blockPlace(BlockPlaceEvent e, Participation participation) {
    onBlockPlace(e, participation);
  }

  public final void entityPlace(EntityPlaceEvent e, Participation participation) {
    onEntityPlace(e, participation);
  }

  public final void entityRegainHealth(EntityRegainHealthEvent e, Participation participation) {
    if (started && !finished) {
      onEntityRegainHealth(e, participation);
    }
  }

  public final void playerItemDamage(PlayerItemDamageEvent e, Participation participation) {
    if (started && !finished) {
      onPlayerItemDamage(e, participation);
    }
  }

  protected void onTick() {
  }

  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {
  }

  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
  }

  protected void onInventoryClick(InventoryClickEvent e, Participation participation) {
  }

  protected void onPlayerItemConsume(PlayerItemConsumeEvent e, Participation participation) {
  }

  protected void onBlockDropItem(BlockDropItemEvent e) {
  }

  protected void onFurnaceSmelt(FurnaceSmeltEvent e) {
  }

  protected void onEntityDeath(EntityDeathEvent e) {
  }

  protected void onEntitySpawn(EntitySpawnEvent e) {
  }

  protected void onPlayerDeath(PlayerDeathEvent e, Participation participation) {
  }

  protected void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent e, Participation participation) {
  }

  protected void onPlayerInteractEntity(PlayerInteractEntityEvent e, Role player, Role rightClicked) {
  }

  protected void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
  }

  protected void onEntityDismount(EntityDismountEvent e, Participation participation) {
  }

  protected void onBlockBreak(BlockBreakEvent e, Participation participation) {
    e.setCancelled(true);
  }

  protected void onBlockPlace(BlockPlaceEvent e, Participation participation) {
    e.setCancelled(true);
  }

  protected void onEntityPlace(EntityPlaceEvent e, Participation participation) {
    e.setCancelled(true);
  }

  protected void onEntityRegainHealth(EntityRegainHealthEvent e, Participation participation) {
  }

  protected void onPlayerItemDamage(PlayerItemDamageEvent e, Participation participation) {
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
      openGate(true);
      onStart();
    }
  }

  public final void openGate(boolean animate) {
    if (openGateTask != null) {
      openGateTask.cancel();
      openGateTask = null;
    }
    if (gateState != null && gateState == 3) {
      return;
    }

    if (animate) {
      var scheduler = Bukkit.getScheduler();
      var interval = 15;

      setGateState(1);

      final var count = new AtomicInteger(1);
      openGateTask = scheduler.runTaskTimer(owner, () -> {
        var c = count.incrementAndGet();
        if (c == 3) {
          openGateTask.cancel();
          openGateTask = null;
        }
        setGateState(c);
      }, interval, interval);
    } else {
      setGateState(3);
    }
  }

  public final void closeGate() {
    if (openGateTask != null) {
      openGateTask.cancel();
      openGateTask = null;
    }
    setGateState(0);
  }

  private void setGateState(int state) {
    if (gateState != null && gateState == state) {
      return;
    }
    // origin: dark_oak_fence の一番北西下の位置
    var origin = this.origin.added(4, 0, 0);
    switch (state) {
      case 0 -> {
        // fully closed
        Editor.Fill(world, origin, origin.added(4, 2, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
        Editor.Fill(world, origin.added(0, 0, 1), origin.added(4, 2, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
        Editor.Fill(world, origin.added(1, 4, 0), origin.added(3, 6, 1), "barrier");
        Editor.Fill(world, origin.added(0, 4, 0), origin.added(0, 6, 1), "chain[axis=y]");
        Editor.Fill(world, origin.added(4, 4, 0), origin.added(4, 6, 1), "chain[axis=y]");
        world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        gateState = 0;
      }
      case 1 -> {
        // 1 block opened
        Editor.Fill(world, origin, origin.added(4, 0, 1), "air");
        Editor.Fill(world, origin.added(0, 4, 0), origin.added(4, 4, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
        Editor.Fill(world, origin.added(0, 4, 1), origin.added(4, 4, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
        world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        gateState = 1;
      }
      case 2 -> {
        // 2 blocks opened
        Editor.Fill(world, origin.added(0, 1, 0), origin.added(4, 1, 1), "air");
        Editor.Fill(world, origin.added(0, 5, 0), origin.added(4, 5, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
        Editor.Fill(world, origin.added(0, 5, 1), origin.added(4, 5, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
        world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        gateState = 2;
      }
      default -> {
        // fully opened
        Editor.Fill(world, origin.added(0, 2, 0), origin.added(4, 2, 1), "air");
        Editor.Fill(world, origin.added(0, 6, 0), origin.added(0, 6, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=false]");
        Editor.Fill(world, origin.added(0, 6, 1), origin.added(0, 6, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=false]");
        Editor.Fill(world, origin.added(1, 6, 0), origin.added(3, 6, 0), "dark_oak_fence[east=true,north=false,south=true,waterlogged=false,west=true]");
        Editor.Fill(world, origin.added(1, 6, 1), origin.added(3, 6, 1), "dark_oak_fence[east=true,north=true,south=false,waterlogged=false,west=true]");
        Editor.Fill(world, origin.added(4, 6, 0), origin.added(4, 6, 0), "dark_oak_fence[east=false,north=false,south=true,waterlogged=false,west=true]");
        Editor.Fill(world, origin.added(4, 6, 1), origin.added(4, 6, 1), "dark_oak_fence[east=false,north=true,south=false,waterlogged=false,west=true]");
        world.playSound(origin.added(2, 0, 1).toLocation(world), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        gateState = 3;
      }
    }
  }
}
