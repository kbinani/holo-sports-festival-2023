package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.annotation.Nullable;

public class KibasenEventListener implements MiniGame, Registrants.Delegate {
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final Component title = Component.text("[Kibasen]").color(Colors.aqua);
  static final Component prefix = title.append(Component.text(" ").color(Colors.white));
  private static final Point3i joinRedSign = pos(-30, 80, 50);
  private static final Point3i joinWhiteSign = pos(-30, 80, 51);
  private static final Point3i joinYellowSign = pos(-30, 80, 52);
  private static final Point3i startSign = pos(-30, 80, 54);
  private static final Point3i abortSign = pos(-30, 80, 55);
  private static final Point3i entryListSign = pos(-30, 80, 56);
  private static final String itemTag = "hololive_sports_festival_2023_kibasen";
  private static final BoundingBox announceBounds = new BoundingBox(x(-63), y(80), z(13), x(72), 500, z(92));
  private static final String teamNamePrefix = "hololive_sports_festival_2023_kibasen";
  private static final Point3i leaderRegistrationBarrel = pos(-30, 63, 53);
  private static final int durationSec = 90;

  private final World world;
  private final JavaPlugin owner;
  private Status status = Status.IDLE;
  private final Teams teams = new Teams(teamNamePrefix);
  private final Registrants registrants = new Registrants(teams, this);

  public KibasenEventListener(World world, JavaPlugin owner) {
    this.owner = owner;
    this.world = world;
  }

  @Override
  public void miniGameReset() {
    reset();
  }

  @Override
  public void miniGameClearItem(Player player) {
    ClearItems(player);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    switch (status) {
      case IDLE -> {
        var action = e.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK) {
          Block block = e.getClickedBlock();
          if (block != null) {
            Point3i location = new Point3i(block.getLocation());
            if (location.equals(joinRedSign)) {
              onClickJoin(player, TeamColor.RED);
              e.setCancelled(true);
              return;
            } else if (location.equals(joinWhiteSign)) {
              onClickJoin(player, TeamColor.WHITE);
              e.setCancelled(true);
              return;
            } else if (location.equals(joinYellowSign)) {
              onClickJoin(player, TeamColor.YELLOW);
              e.setCancelled(true);
              return;
            }
          }
        }
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
          var item = e.getItem();
          if (item != null) {
            if (item.getType() == Material.BOOK) {
              var meta = item.getItemMeta();
              if (meta != null) {
                var store = meta.getPersistentDataContainer();
                if (store.has(NamespacedKey.minecraft(itemTag), PersistentDataType.BYTE)) {
                  var inventory = openLeaderRegistrationInventory();
                  if (inventory != null) {
                    player.openInventory(inventory);
                    e.setCancelled(true);
                    return;
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
    var attacker = e.getPlayer();
    var hand = e.getHand();
    var inventory = attacker.getInventory();
    var item = inventory.getItem(hand);
    if (item.getType() != Material.SADDLE) {
      return;
    }
    if (!(e.getRightClicked() instanceof Player vehicle)) {
      return;
    }
    var meta = item.getItemMeta();
    if (meta == null) {
      return;
    }
    var store = meta.getPersistentDataContainer();
    if (!store.has(NamespacedKey.minecraft(itemTag), PersistentDataType.BYTE)) {
      return;
    }
    registrants.addPassenger(attacker, vehicle);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityDismount(EntityDismountEvent e) {
    if (!(e.getDismounted() instanceof Player vehicle)) {
      return;
    }
    if (!(e.getEntity() instanceof Player attacker)) {
      return;
    }
    var participation = registrants.getParticipation(vehicle);
    if (participation == null) {
      return;
    }
    registrants.dismount(attacker);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerQuit(PlayerQuitEvent e) {
    var player = e.getPlayer();
    registrants.remove(player);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onInventoryClick(InventoryClickEvent e) {
    var inventory = e.getInventory();
    if (!(inventory.getHolder(false) instanceof Barrel barrel)) {
      return;
    }
    var location = barrel.getLocation();
    if (location.getWorld() != world) {
      return;
    }
    if (!leaderRegistrationBarrel.equals(new Point3i(location))) {
      return;
    }
    if (!(e.getWhoClicked() instanceof Player player)) {
      return;
    }
    e.setCancelled(true);
    var current = registrants.getParticipation(player);
    if (current == null) {
      return;
    }
    if (e.getAction() != InventoryAction.PICKUP_ALL || !e.isLeftClick()) {
      return;
    }
    var slot = e.getSlot();
    if (slot != 10 && slot != 13 && slot != 16) {
      return;
    }
    var index = (slot - 10) / 3;
    var color = TeamColor.all[index];
    var item = inventory.getItem(slot);
    if (item == null) {
      return;
    }
    if (color != current.color) {
      return;
    }
    registrants.onClickBecomeLeader(player);
  }

  @Override
  public void registrantsBroadcast(Component message) {
    broadcast(message);
  }

  @Override
  public @Nullable Inventory registrantsOpenLeaderRegistrationInventory() {
    return openLeaderRegistrationInventory();
  }

  static void ClearItems(Player player) {
    var inventory = player.getInventory();
    for (int i = 0; i < inventory.getSize(); i++) {
      var item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      var meta = item.getItemMeta();
      if (meta == null) {
        continue;
      }
      var container = meta.getPersistentDataContainer();
      if (container.has(NamespacedKey.minecraft(itemTag), PersistentDataType.BYTE)) {
        inventory.clear(i);
      }
    }
  }

  // Component.text("{name}に馬が居ないため、ゲームを開始できません。").color(Colors.red)): https://youtu.be/D9vmP7Qj4TI?t=1398

  private void onClickJoin(Player player, TeamColor color) {
    var current = registrants.getParticipation(player);
    if (current == null) {
      registrants.addAttacker(player, color);
    } else {
      registrants.remove(player);
    }
  }

  static ItemStack CreateSaddle() {
    return ItemBuilder.For(Material.SADDLE)
      .displayName(Component.text("自分の馬を右クリックしてください！").color(Colors.orange))
      .customByteTag(itemTag, (byte) 1)
      .build();
  }

  static ItemStack CreateBook() {
    return ItemBuilder.For(Material.BOOK)
      .displayName(Component.text("大将にエントリー (右クリックで使用)").color(Colors.orange))
      .customByteTag(itemTag, (byte) 1)
      .build();
  }

  static ItemStack CreateWool(TeamColor color) {
    return ItemBuilder.For(color.wool)
      .displayName(color.component().append(Component.text(" の大将になる！").color(Colors.white)))
      .build();
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }

  private void setEnablePhotoSpot(boolean enable) {
    if (enable) {
      Editor.Fill(world, pos(-6, 80, 48), pos(0, 80, 53), Material.WHITE_CONCRETE);
      Editor.Fill(world, pos(-6, 81, 48), pos(9, 81, 51), Material.WHITE_CONCRETE);
      Editor.Fill(world, pos(-6, 82, 48), pos(0, 82, 49), Material.WHITE_CONCRETE);
      Editor.Fill(world, pos(1, 80, 48), pos(7, 80, 53), Material.PINK_CONCRETE);
      Editor.Fill(world, pos(1, 81, 48), pos(7, 81, 51), Material.PINK_CONCRETE);
      Editor.Fill(world, pos(1, 82, 48), pos(7, 82, 49), Material.PINK_CONCRETE);
      Editor.Fill(world, pos(8, 80, 48), pos(14, 80, 53), Material.YELLOW_CONCRETE);
      Editor.Fill(world, pos(8, 81, 48), pos(14, 81, 51), Material.YELLOW_CONCRETE);
      Editor.Fill(world, pos(8, 82, 48), pos(14, 82, 49), Material.YELLOW_CONCRETE);
    } else {
      Editor.Fill(world, pos(-6, 80, 48), pos(14, 80, 53), Material.AIR);
      Editor.Fill(world, pos(-6, 81, 48), pos(14, 81, 51), Material.AIR);
      Editor.Fill(world, pos(-6, 82, 48), pos(14, 82, 49), Material.AIR);
    }
  }

  private void setEnableWall(boolean enable) {
    var material = enable ? Material.BARRIER : Material.AIR;
    Editor.Fill(world, pos(-24, 81, 31), pos(-24, 86, 73), material);
    Editor.Fill(world, pos(32, 81, 31), pos(32, 86, 73), material);
    Editor.Fill(world, pos(-23, 81, 31), pos(31, 86, 31), material);
    Editor.Fill(world, pos(-23, 81, 73), pos(31, 86, 73), material);
  }

  private void reset() {
    Editor.StandingSign(world, joinRedSign, Material.OAK_SIGN, 4,
      title,
      TeamColor.RED.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );
    Editor.StandingSign(world, joinWhiteSign, Material.OAK_SIGN, 4,
      title,
      TeamColor.WHITE.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );
    Editor.StandingSign(world, joinYellowSign, Material.OAK_SIGN, 4,
      title,
      TeamColor.YELLOW.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );

    Editor.StandingSign(world, startSign, Material.OAK_SIGN, 4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームスタート").color(Colors.lime)
    );
    Editor.StandingSign(world, abortSign, Material.OAK_SIGN, 4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームを中断する").color(Colors.red)
    );
    Editor.StandingSign(world, entryListSign, Material.OAK_SIGN, 4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("エントリーリスト").color(Colors.aqua)
    );

    setEnablePhotoSpot(true);
    setEnableWall(false);

    Editor.Set(world, leaderRegistrationBarrel, Material.BARREL.createBlockData());
    registrants.clearLeaderRegistrationBarrel();
  }

  private @Nullable Inventory openLeaderRegistrationInventory() {
    var block = world.getBlockAt(leaderRegistrationBarrel.toLocation(world));
    var state = block.getState(false);
    if (state instanceof Barrel barrel) {
      return barrel.getInventory();
    } else {
      return null;
    }
  }

  private static int x(int x) {
    return x + offset.x;
  }

  static int y(int y) {
    return y + offset.y;
  }

  private static int z(int z) {
    return z + offset.z;
  }

  private static Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
