package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KibasenEventListener implements MiniGame {
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final Component title = Component.text("[Kibasen]").color(Colors.aqua);
  private static final Component prefix = title.append(Component.text(" ").color(Colors.white));
  private static final Point3i joinRedSign = pos(-30, 80, 50);
  private static final Point3i joinWhiteSign = pos(-30, 80, 51);
  private static final Point3i joinYellowSign = pos(-30, 80, 52);
  private static final Point3i startSign = pos(-30, 80, 54);
  private static final Point3i abortSign = pos(-30, 80, 55);
  private static final Point3i entryListSign = pos(-30, 80, 56);
  private static final String itemTag = "hololive_sports_festival_2023_kibasen";
  private static final BoundingBox announceBounds = new BoundingBox(x(-63), y(80), z(13), x(72), 500, z(92));
  private static final String teamNamePrefix = "hololive_sports_festival_2023_kibasen";
  private static final Point3i generalRegistrationBarrel = pos(-30, 63, 53);
  private static final int durationSec = 90;

  private final World world;
  private final JavaPlugin owner;
  private Status status = Status.IDLE;
  private final Map<TeamColor, ArrayList<MutableUnit>> registrants = new HashMap<>();

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
    clearItems(player);
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
                  var inventory = openGeneralRegistrationInventory();
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
    var p = getParticipation(attacker);
    if (p == null) {
      return;
    }
    if (!p.isAttacker) {
      return;
    }
    if (p.unit.vehicle != null) {
      return;
    }
    if (getParticipation(vehicle) != null) {
      // そのプレイヤーは馬にできません。: https://youtu.be/D9vmP7Qj4TI?t=1058
      attacker.sendMessage(Component.text("そのプレイヤーは馬にできません。").color(Colors.red));
      return;
    }
    if (!vehicle.addPassenger(attacker)) {
      attacker.sendMessage(Component.text("そのプレイヤーは馬にできません。").color(Colors.red));
      return;
    }
    p.unit.vehicle = vehicle;
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityMount(EntityMountEvent e) {
    if (!(e.getMount() instanceof Player vehicle)) {
      return;
    }
    if (!(e.getEntity() instanceof Player attacker)) {
      return;
    }
    var p = getParticipation(attacker);
    if (p == null) {
      return;
    }
    p.unit.vehicle = vehicle;
    var team = ensureTeam(p.color);
    team.addPlayer(vehicle);
    clearItems(attacker);
    var book = CreateBook();
    var inventory = attacker.getInventory();
    inventory.setItem(0, book);
    vehicle.sendMessage(prefix
      .append(Component.text(String.format("%sの騎馬になりました！", attacker.getName())).color(Colors.white)));
    attacker.sendMessage(prefix
      .append(Component.text(String.format("%sを騎馬にしました！", vehicle.getName())).color(Colors.white)));
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
    var participation = getParticipation(vehicle);
    if (participation == null) {
      return;
    }
    var unit = participation.unit;
    var p = getParticipation(attacker);
    if (p == null) {
      return;
    }
    if (p.unit != unit) {
      return;
    }
    unit.vehicle = null;
    var team = ensureTeam(p.color);
    team.removePlayer(vehicle);
    clearItems(attacker);
    var inventory = attacker.getInventory();
    inventory.setItem(0, CreateSaddle());
    if (unit.isGeneral) {
      unit.isGeneral = false;
      broadcast(prefix
        .append(Component.text(unit.attacker.getName()).color(Colors.orange))
        .append(Component.text("が").color(Colors.white))
        .append(p.color.component())
        .append(Component.text("の大将を辞めました！").color(Colors.white)));
      updateGeneralRegistrationBarrel();
      vehicle.removePotionEffect(PotionEffectType.GLOWING);
      attacker.removePotionEffect(PotionEffectType.GLOWING);
    }

    // https://youtu.be/D9vmP7Qj4TI?t=1217
    vehicle.sendMessage(prefix
      .append(Component.text("騎士があなたから降りたため、エントリーが解除されました。").color(Colors.white)));
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerQuit(PlayerQuitEvent e) {
    var player = e.getPlayer();
    var current = getParticipation(player);
    if (current == null) {
      return;
    }
    clearItems(player);
    var team = ensureTeam(current.color);
    team.removePlayer(player);
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
    if (!generalRegistrationBarrel.equals(new Point3i(location))) {
      return;
    }
    if (!(e.getWhoClicked() instanceof Player player)) {
      return;
    }
    e.setCancelled(true);
    var current = getParticipation(player);
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
    var units = registrants.get(color);
    if (units == null) {
      return;
    }
    for (var unit : units) {
      if (unit == current.unit && unit.isGeneral) {
        unit.isGeneral = false;
        updateGeneralRegistrationBarrel();
        broadcast(prefix
          .append(Component.text(player.getName()).color(Colors.orange))
          .append(Component.text("が").color(Colors.white))
          .append(color.component())
          .append(Component.text("の大将を辞めました！").color(Colors.white)));
        if (current.unit.vehicle != null) {
          current.unit.vehicle.removePotionEffect(PotionEffectType.GLOWING);
        }
        current.unit.attacker.removePotionEffect(PotionEffectType.GLOWING);
        return;
      }
    }
    if (color.wool != item.getType()) {
      // https://youtu.be/uEpmE5WJPW8?t=1987
      player.sendMessage(prefix.append(Component.text("他のプレイヤーが選択しています。").color(Colors.red)));
      return;
    }
    current.unit.isGeneral = true;
    updateGeneralRegistrationBarrel();
    if (current.unit.vehicle != null) {
      current.unit.vehicle.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
    }
    current.unit.attacker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
    broadcast(prefix
      .append(color.component())
      .append(Component.text("の大将に").color(Colors.white))
      .append(Component.text(player.getName()).color(Colors.orange))
      .append(Component.text("がエントリーしました！").color(Colors.white)));
  }

  private void updateGeneralRegistrationBarrel() {
    var inventory = openGeneralRegistrationInventory();
    if (inventory == null) {
      return;
    }
    for (var color : TeamColor.all) {
      MutableUnit general = null;
      var units = registrants.get(color);
      if (units != null) {
        for (var unit : units) {
          if (unit.isGeneral) {
            general = unit;
            break;
          }
        }
      }
      var index = 10 + 3 * color.ordinal();
      if (general == null) {
        inventory.setItem(index, CreateWool(color));
      } else {
        var attacker = general.attacker;
        var name = color
          .component()
          .appendSpace()
          .append(Component.text("大将").color(Colors.yellow))
          .appendSpace()
          .append(Component.text(attacker.getName()).color(Colors.orange));
        var head = ItemBuilder.For(Material.PLAYER_HEAD)
          .displayName(name)
          .build();
        head.editMeta(SkullMeta.class, (skull) -> {
          skull.setOwningPlayer(attacker);
        });
        inventory.setItem(index, head);
      }
    }
  }

  private @Nonnull Team ensureTeam(TeamColor color) {
    var server = Bukkit.getServer();
    var manager = server.getScoreboardManager();
    var scoreboard = manager.getMainScoreboard();
    var name = teamNamePrefix + "_" + color.japanese;
    var team = scoreboard.getTeam(name);
    if (team == null) {
      team = scoreboard.registerNewTeam(name);
    }
    team.color(color.namedTextColor);
    team.setAllowFriendlyFire(false);
    return team;
  }

  private void clearItems(Player player) {
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
    var current = getParticipation(player);
    if (current != null) {
      if (current.isAttacker) {
        if (current.unit.vehicle != null) {
          current.unit.vehicle.removePassenger(player);
        }
        registrants.get(current.color).remove(current.unit);
        clearItems(player);
        var team = ensureTeam(current.color);
        team.removePlayer(player);
        if (current.unit.isGeneral) {
          var inventory = openGeneralRegistrationInventory();
          if (inventory != null) {
            var wool = CreateWool(current.color);
            inventory.setItem(10 + 3 * current.color.ordinal(), wool);
          }
        }
        // https://youtu.be/D9vmP7Qj4TI?t=1462
        player.sendMessage(prefix.append(Component.text("エントリー登録を解除しました。").color(Colors.white)));
      }
      return;
    }
    var inventory = player.getInventory();
    var saddle = CreateSaddle();
    if (inventory.getItem(0) != null) {
      warnNonEmptySlot(player, 0);
      clearItems(player);
      return;
    }
    inventory.setItem(0, saddle);

    var team = ensureTeam(color);
    team.addPlayer(player);

    broadcast(prefix
      .append(Component.text(player.getName() + "が").color(Colors.white))
      .append(color.component())
      .append(Component.text("にエントリーしました。").color(Colors.white))
    );

    player.sendMessage(prefix
      .append(Component.text("Right-click with the saddle on the player you want to make your horse!").color(Colors.white))
    );

    var unit = new MutableUnit(player);
    var units = registrants.computeIfAbsent(color, (c) -> new ArrayList<>());
    units.add(unit);
  }

  private static ItemStack CreateSaddle() {
    return ItemBuilder.For(Material.SADDLE)
      .displayName(Component.text("自分の馬を右クリックしてください！").color(Colors.orange))
      .customByteTag(itemTag, (byte) 1)
      .build();
  }

  private static ItemStack CreateBook() {
    return ItemBuilder.For(Material.BOOK)
      .displayName(Component.text("大将にエントリー (右クリックで使用)").color(Colors.orange))
      .customByteTag(itemTag, (byte) 1)
      .build();
  }

  private static ItemStack CreateWool(TeamColor color) {
    return ItemBuilder.For(color.wool)
      .displayName(color.component().append(Component.text(" の大将になる！").color(Colors.white)))
      .build();
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }

  private void warnNonEmptySlot(Player player, int index) {
    player.sendMessage(prefix
      .append(Component.text(String.format("インベントリのスロット%dに既にアイテムがあるため競技用アイテムを渡せません", index)).color(Colors.red))
    );
  }

  private @Nullable Participation getParticipation(Player player) {
    for (var entry : registrants.entrySet()) {
      for (var unit : entry.getValue()) {
        var color = entry.getKey();
        if (unit.vehicle == player) {
          return new Participation(color, unit, false);
        }
        if (unit.attacker == player) {
          return new Participation(color, unit, true);
        }
      }
    }
    return null;
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

    Editor.Set(world, generalRegistrationBarrel, Material.BARREL.createBlockData());
    var block = world.getBlockAt(generalRegistrationBarrel.toLocation(world));
    if (block.getState(false) instanceof Barrel barrel) {
      // https://youtu.be/gp6ABH58SGA?t=2068
      barrel.customName(prefix.append(Component.text("大将").color(Colors.green)));
      barrel.update();

      var inventory = barrel.getInventory();
      inventory.clear();
      var materials = new Material[]{Material.RED_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE};
      for (var x = 0; x < 3; x++) {
        var material = materials[x];
        for (var y = 0; y < 3; y++) {
          for (var i = 0; i < 3; i++) {
            var index = x * 3 + i + y * 9;
            var item = ItemBuilder.For(material)
              .displayName(Component.empty())
              .build();
            inventory.setItem(index, item);
          }
        }
      }
      for (var x = 0; x < 3; x++) {
        var color = TeamColor.all[x];
        var wool = CreateWool(color);
        var index = 10 + x * 3;
        inventory.setItem(index, wool);
      }
    }
  }

  private @Nullable Inventory openGeneralRegistrationInventory() {
    var block = world.getBlockAt(generalRegistrationBarrel.toLocation(world));
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
