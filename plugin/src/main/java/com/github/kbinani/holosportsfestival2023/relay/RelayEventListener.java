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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class RelayEventListener implements MiniGame {
  public interface Delegate {
    Point3i relayGetJoinSignLocation(TeamColor color);

    Point3i relayGetAnnounceEntryListSignLocation();

    Point3i relayGetStartSignLocation();

    BoundingBox relayGetAnnounceBounds();
  }

  public static final Component title = text("[Relay]", AQUA);
  static final Component prefix = title.append(text(" ", WHITE));
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final String sBubbleScoreboardTag = "hololive_sports_festival2023_relay_bubble";
  private static final String sBreadHangerScoreboardTag = "hololive_sports_festival_2023_relay_bread";
  private static final String sItemTag = "hololive_sports_festival_2023_relay_item";

  private final @Nonnull World world;
  private final @Nonnull JavaPlugin owner;
  private final @Nonnull Map<TeamColor, Team> teams = new HashMap<>();
  private final Set<Point3i> pistonPositions;
  private final @Nonnull BukkitTask waveTimer;
  private final List<Wave> waves = new ArrayList<>();
  private boolean wavePrepared = false;
  private boolean breadHangerPrepared = false;
  private final List<ArmorStand> breadHangers = new ArrayList<>();
  private Status status = Status.IDLE;
  private final @Nonnull Delegate delegate;
  private @Nullable Race race;

  public RelayEventListener(@Nonnull World world, @Nonnull JavaPlugin owner, @Nonnull Delegate delegate) {
    this.world = world;
    this.owner = owner;
    this.delegate = delegate;
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
    var armorStand = e.getRightClicked();
    for (var stand : breadHangers) {
      if (stand == armorStand) {
        e.setCancelled(true);
        return;
      }
    }
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
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    var action = e.getAction();
    var player = e.getPlayer();
    if (status == Status.IDLE) {
      if (action == Action.RIGHT_CLICK_BLOCK) {
        var block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        var location = new Point3i(block.getLocation());
        for (var color : TeamColor.all) {
          if (delegate.relayGetJoinSignLocation(color).equals(location)) {
            onClickJoin(player, color);
            e.setCancelled(true);
            return;
          }
        }
        if (delegate.relayGetStartSignLocation().equals(location)) {
          onClickStart();
          e.setCancelled(true);
          return;
        } else if (delegate.relayGetAnnounceEntryListSignLocation().equals(location)) {
          onClickAnnounceEntryList();
          e.setCancelled(true);
          return;
        }
      }
    }
  }

  private void onClickAnnounceEntryList() {

  }

  private void onClickJoin(Player player, TeamColor color) {
    for (var team : teams.values()) {
      if (team.players().contains(player)) {
        team.remove(player);
        player.sendMessage(prefix.append(text("エントリー登録を解除しました。", WHITE)));
        return;
      }
    }
    var team = ensureTeam(color);
    team.add(player);
    broadcast(prefix
      .append(text(String.format("%sが", player.getName()), WHITE))
      .append(color.component())
      .append(text("にエントリーしました。", WHITE)));
  }

  private @Nonnull Team ensureTeam(TeamColor color) {
    var team = teams.get(color);
    if (team == null) {
      var t = new Team(color);
      teams.put(color, t);
      return t;
    } else {
      return team;
    }
  }

  private void onClickStart() {

  }

  private void reset() {
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
        e.setHelmet(new ItemStack(Material.BREAD));
      });
      breadHangers.add(stand);
    }
  }

  private @Nonnull ItemStack createBread() {
    if (status == Status.ACTIVE) {
      return ItemBuilder.For(Material.BREAD)
        .displayName(text("元気が出るパン", GOLD))
        .customByteTag(sItemTag)
        .build();
    } else {
      return new ItemStack(Material.BREAD);
    }
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

  private void broadcast(Component message) {
    Players.Within(world, delegate.relayGetAnnounceBounds(), player -> player.sendMessage(message));
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
