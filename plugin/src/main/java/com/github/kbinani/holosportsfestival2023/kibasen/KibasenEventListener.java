package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

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
        Block block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
          return;
        }
        Point3i location = new Point3i(block.getLocation());
        if (location.equals(joinRedSign)) {
          onClickJoin(player, TeamColor.RED);
        } else if (location.equals(joinWhiteSign)) {
          onClickJoin(player, TeamColor.WHITE);
        } else if (location.equals(joinYellowSign)) {
          onClickJoin(player, TeamColor.YELLOW);
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
    if (!vehicle.addPassenger(attacker)) {
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
    //TODO:
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

  // {name}の騎馬になりました！
  // 他のプレイヤーが選択しています。
  // {name}を騎馬にしました！
  // 騎士があなたから降りたため、エントリーが解除されました。

  private void onClickJoin(Player player, TeamColor color) {
    if (getParticipation(player) != null) {
      return;
    }
    var inventory = player.getInventory();
    var saddle = ItemBuilder.For(Material.SADDLE)
      .displayName(Component.text("自分の馬を右クリックしてください！").color(Colors.orange))
      .customByteTag(itemTag, (byte) 1)
      .build();
    if (inventory.getItem(0) != null) {
      warnNonEmptySlot(player, 0);
      clearItems(player);
      return;
    }
    inventory.setItem(0, saddle);

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

  private void reset() {
    Editor.StandingSign(
      world,
      joinRedSign,
      Material.OAK_SIGN,
      4,
      title,
      TeamColor.RED.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );
    Editor.StandingSign(
      world,
      joinWhiteSign,
      Material.OAK_SIGN,
      4,
      title,
      TeamColor.WHITE.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );
    Editor.StandingSign(
      world,
      joinYellowSign,
      Material.OAK_SIGN,
      4,
      title,
      TeamColor.YELLOW.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );

    Editor.StandingSign(
      world,
      startSign,
      Material.OAK_SIGN,
      4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームスタート").color(Colors.lime)
    );
    Editor.StandingSign(
      world,
      abortSign,
      Material.OAK_SIGN,
      4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームを中断する").color(Colors.red)
    );
    Editor.StandingSign(
      world,
      entryListSign,
      Material.OAK_SIGN,
      4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("エントリーリスト").color(Colors.aqua)
    );
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
