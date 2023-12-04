package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class RelayEventListener implements MiniGame {
  public static final Component title = text("[Relay]", AQUA);
  static final Component prefix = title.append(text(" ", WHITE));
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final String sBubbleScoreboardTag = "hololive_sports_festival2023_relay_bubble";
  private static final String sBreadHangerScoreboardTag = "hololive_sports_festival_2023_relay_bread";
  private static final String sItemTag = "hololive_sports_festival_2023_relay_item";

  private final @Nonnull World world;
  private final @Nonnull JavaPlugin owner;
  private final Map<TeamColor, Team> teams = new HashMap<>();
  private final Set<Point3i> pistonPositions;
  private final @Nonnull BukkitTask waveTimer;
  private final List<Wave> waves = new ArrayList<>();
  private boolean wavePrepared = false;
  private boolean breadHangerPrepared = false;
  private final List<ArmorStand> breadHangers = new ArrayList<>();

  public RelayEventListener(@Nonnull World world, @Nonnull JavaPlugin owner) {
    this.world = world;
    this.owner = owner;
    pistonPositions = new HashSet<>();
    pistonPositions.add(pos(57, 79, 57));
    pistonPositions.add(pos(59, 79, 57));
    pistonPositions.add(pos(61, 79, 57));
    this.waveTimer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 1, 1);
  }

  @Override
  public void miniGameReset() {
    reset();
  }

  @Override
  public void miniGameClearItem(Player player) {

  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onItemSpawn(ItemSpawnEvent e) {
    var location = e.getLocation();
    if (location.getWorld() != world) {
      return;
    }
    var item = e.getEntity();
    var stack = item.getItemStack();
    if (stack.getType() != Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
      return;
    }
    if (x(56) <= location.getX() && location.getX() <= x(63) &&
      y(80) <= location.getY() && location.getY() <= y(91) &&
      z(56) <= location.getZ() && location.getZ() <= z(59)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onBlockPistonRetract(BlockPistonRetractEvent e) {
    var block = e.getBlock();
    var location = new Point3i(block.getLocation());
    if (pistonPositions.contains(location)) {
      var pressurePlatePos = location.added(0, 2, 0);
      Bukkit.getScheduler().runTaskLater(owner, () -> {
        Editor.Set(world, pressurePlatePos, Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
      }, 10);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent e) {
    //NOTE: 本家では左クリックでパンを取る事になっているけど右クリックでも取れるようにしておく
    var armorStand = e.getRightClicked();
    var playerItem = e.getPlayerItem();
    var standItem = e.getArmorStandItem();
    ArmorStand target = null;
    for (var stand : breadHangers) {
      if (stand == armorStand) {
        target = stand;
        break;
      }
    }
    if (target == null) {
      return;
    }
    if (playerItem.getType() != Material.AIR) {
      e.setCancelled(true);
      return;
    }
    if (standItem.getType() != Material.BREAD) {
      e.setCancelled(true);
      return;
    }
    var slot = e.getSlot();
    if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST) {
      e.setCancelled(true);
      return;
    }
    var equipment = target.getEquipment();
    if (slot == EquipmentSlot.CHEST) {
      equipment.setHelmet(new ItemStack(Material.AIR));
    } else {
      equipment.setChestplate(new ItemStack(Material.AIR));
    }
    delayRecoverBreads(target);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof ArmorStand armorStand)) {
      return;
    }
    if (!(e.getDamager() instanceof Player player)) {
      return;
    }
    if (breadHangers.stream().noneMatch((it -> it == armorStand))) {
      return;
    }
    e.setCancelled(true);
    var inventory = player.getInventory();
    inventory.addItem(createBread());
    var equipment = armorStand.getEquipment();
    equipment.setHelmet(new ItemStack(Material.AIR));
    equipment.setChestplate(new ItemStack(Material.AIR));
    delayRecoverBreads(armorStand);
  }

  private void delayRecoverBreads(ArmorStand target) {
    Bukkit.getScheduler().runTaskLater(owner, () -> {
      var equipment = target.getEquipment();
      equipment.setHelmet(createBread());
      equipment.setChestplate(createBread());
    }, 20);
  }

  private void reset () {
  }

  private void tick() {
    tickWaves();
    tickBreadHanger();
  }

  private void tickBreadHanger() {
    if (breadHangerPrepared) {
      return;
    }
    var minX = x(55) / 16;
    var maxX = x(63) / 16;
    var cz = z(55) / 16;
    for (int cx = minX; cx <= maxX; cx++) {
      if (!world.isChunkLoaded(cx, cz)) {
        return;
      }
    }
    breadHangerPrepared = true;
    Kill.EntitiesByScoreboardTag(world, sBreadHangerScoreboardTag);
    for (var i = 0; i < 3; i++) {
      var stand = world.spawn(new Location(world, x(57) + 0.5 + i * 2, y(86) + 0.6, z(55) + 0.8), ArmorStand.class, it -> {
        it.addScoreboardTag(sBreadHangerScoreboardTag);
        it.setGravity(false);
        it.setBasePlate(false);
        it.setVisible(false);
        var e = it.getEquipment();
        e.setHelmet(createBread());
        e.setChestplate(createBread());
      });
      breadHangers.add(stand);
    }
  }

  private @Nonnull ItemStack createBread() {
    // https://youtu.be/ls3kb0qhT4E?t=11046
    return ItemBuilder.For(Material.BREAD)
      .displayName(text("元気が出るパン", GOLD))
      .customByteTag(sItemTag)
      .build();
  }

  private void tickWaves() {
    var minX = x(-48) / 16;
    var minZ = z(64) / 16;
    var maxX = x(-16) / 16;
    var maxZ = z(80) / 16;
    for (int cx = minX; cx <= maxX; cx++) {
      for (int cz = minZ; cz <= maxZ; cz++) {
        if (!world.isChunkLoaded(cx, cz)) {
          return;
        }
      }
    }
    if (!wavePrepared) {
      wavePrepared = true;
      Kill.EntitiesByScoreboardTag(world, sBubbleScoreboardTag);
      var yCandidates = new double[]{
        y(83) + 0.5,
        y(82) + 0.5,
        y(81) + 0.5
      };
      var startX = x(-12) + 0.5;
      var endX = x(-39) + 0.5;
      waves.add(new Wave(
        world, sBubbleScoreboardTag,
        startX, endX, x(-16),
        yCandidates, y(79) + 0.5
      ));
      waves.add(new Wave(
        world, sBubbleScoreboardTag,
        startX, endX, x(-25),
        yCandidates, y(79) + 0.5
      ));
      waves.add(new Wave(
        world, sBubbleScoreboardTag,
        startX, endX, x(-34),
        yCandidates, y(79) + 0.5
      ));
    }
    for (var wave : waves) {
      wave.tick();
    }
  }

  private int x(int x) {
    return x + offset.x;
  }

  private int y(int y) {
    return y + offset.y;
  }

  private int z(int z) {
    return z + offset.z;
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
