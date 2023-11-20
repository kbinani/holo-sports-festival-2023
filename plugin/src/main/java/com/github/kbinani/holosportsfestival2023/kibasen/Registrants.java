package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import com.github.kbinani.holosportsfestival2023.Teams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.*;

class Registrants {
  interface Delegate {
    @Nullable
    Inventory registrantsOpenLeaderRegistrationInventory();

    void registrantsBroadcast(Component message);
  }

  private final @Nonnull Delegate delegate;
  private final Map<TeamColor, ArrayList<MutableUnit>> registrants = new HashMap<>();
  private final @Nonnull Teams teams;

  Registrants(@Nonnull Teams teams, @Nonnull Delegate delegate) {
    this.teams = teams;
    this.delegate = delegate;
  }

  void clear() {
    for (var entry : registrants.entrySet()) {
      var color = entry.getKey();
      var team = teams.ensure(color);
      for (var unit : entry.getValue()) {
        team.removePlayer(unit.attacker);
        unit.attacker.removePotionEffect(PotionEffectType.GLOWING);
        ClearItems(unit.attacker);
        if (unit.vehicle != null) {
          team.removePlayer(unit.vehicle);
          unit.vehicle.removePassenger(unit.attacker);
          unit.vehicle.removePotionEffect(PotionEffectType.GLOWING);
          ClearItems(unit.vehicle);
        }
      }
    }
    registrants.clear();
  }

  boolean validate() {
    for (var entry : registrants.entrySet()) {
      for (var unit : entry.getValue()) {
        if (!unit.attacker.isOnline()) {
          continue;
        }
        if (unit.vehicle == null || !unit.vehicle.isOnline()) {
          // https://youtu.be/D9vmP7Qj4TI?t=1398
          broadcast(Component.text(String.format("%sに馬が居ないため、ゲームを開始できません。", unit.attacker.getName())).color(Colors.red));
          return false;
        }
      }
    }
    return true;
  }

  @Nullable Session promote(JavaPlugin owner, World world, BoundingBox announceBounds, Session.Delegate delegate) {
    if (registrants.isEmpty()) {
      return null;
    }
    if (!validate()) {
      return null;
    }
    var participants = new HashMap<TeamColor, ArrayList<Unit>>();
    for (var entry : registrants.entrySet()) {
      var color = entry.getKey();
      var units = new ArrayList<Unit>();
      for (var unit : entry.getValue()) {
        if (!unit.attacker.isOnline()) {
          continue;
        }
        if (unit.vehicle == null || !unit.vehicle.isOnline()) {
          continue;
        }
        var location = unit.attacker.getLocation();
        var display = world.spawn(location, ArmorStand.class, (it) -> {
          it.customName(Component.text("♥♥♥").color(NamedTextColor.RED));
          it.setCustomNameVisible(true);
          it.setVisible(false);
          it.setInvulnerable(true);
          it.setSmall(true);
          it.setBasePlate(false);
          it.addScoreboardTag(healthDisplayScoreboardTag);
        });
        unit.attacker.addPassenger(display);
        units.add(new Unit(color, unit.attacker, unit.vehicle, display, unit.isLeader));
      }
      participants.put(color, units);
    }
    registrants.clear();
    return new Session(owner, world, announceBounds, delegate, teams, participants);
  }

  void announceEntryList() {
    // https://youtu.be/uEpmE5WJPW8?t=2680
    broadcast(
      Component.text("-".repeat(10)).color(Colors.lime)
        .appendSpace()
        .append(prefix)
        .appendSpace()
        .append(Component.text("エントリー者 ").color(Colors.aqua))
        .append(Component.text("-".repeat(10)).color(Colors.lime))
    );
    var first = true;
    for (var color : TeamColor.all) {
      if (!first) {
        broadcast(Component.empty());
      }
      first = false;
      var units = registrants.get(color);
      if (units == null) {
        broadcast(Component.text(String.format(" %s (0)", color.text)).color(color.textColor));
        continue;
      }
      broadcast(Component.text(String.format(" %s (%d)", color.text, units.size())).color(color.textColor));
      for (var unit : units) {
        if (unit.vehicle != null) {
          broadcast(Component.text(String.format(" - [騎手] %s & %s", unit.attacker.getName(), unit.vehicle.getName())).color(color.textColor));
        } else {
          broadcast(Component.text(String.format(" - [騎手] %s", unit.attacker.getName())).color(color.textColor));
        }
      }
    }
  }

  void addAttacker(Player player, TeamColor color) {
    if (getParticipation(player) != null) {
      return;
    }
    var inventory = player.getInventory();
    var saddle = CreateSaddle();
    inventory.setItem(0, saddle);

    var team = teams.ensure(color);
    team.addPlayer(player);

    broadcast(Component.empty()
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

  void addPassenger(Player attacker, Player vehicle) {
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
      // https://youtu.be/D9vmP7Qj4TI?t=1058
      attacker.sendMessage(Component.text("そのプレイヤーは馬にできません。").color(Colors.red));
      return;
    }
    if (!vehicle.addPassenger(attacker)) {
      attacker.sendMessage(Component.text("そのプレイヤーは馬にできません。").color(Colors.red));
      return;
    }
    p.unit.vehicle = vehicle;

    var team = teams.ensure(p.color);
    team.addPlayer(vehicle);
    ClearItems(attacker);
    var book = CreateBook();
    var inventory = attacker.getInventory();
    inventory.setItem(0, book);
    vehicle.sendMessage(prefix
      .append(Component.text(String.format("%sの騎馬になりました！", attacker.getName())).color(Colors.white))
    );
    attacker.sendMessage(prefix
      .append(Component.text(String.format("%sを騎馬にしました！", vehicle.getName())).color(Colors.white))
    );
  }

  void onClickBecomeLeader(Player player) {
    var current = getParticipation(player);
    if (current == null) {
      return;
    }
    var units = registrants.get(current.color);
    if (units == null) {
      return;
    }
    for (var unit : units) {
      if (!unit.isLeader) {
        continue;
      }
      if (unit == current.unit) {
        retireLeader(player);
        return;
      }
      // https://youtu.be/uEpmE5WJPW8?t=1987
      player.sendMessage(prefix.append(Component.text("他のプレイヤーが選択しています。").color(Colors.red)));
      return;
    }
    current.unit.isLeader = true;
    updateLeaderRegistrationBarrel();
    if (current.unit.vehicle != null) {
      current.unit.vehicle.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
    }
    current.unit.attacker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
    broadcast(prefix
      .append(current.color.component())
      .append(Component.text("の大将に").color(Colors.white))
      .append(Component.text(player.getName()).color(Colors.orange))
      .append(Component.text("がエントリーしました！").color(Colors.white))
    );
  }

  private void broadcast(Component message) {
    delegate.registrantsBroadcast(message);
  }

  private void retireLeader(Player player) {
    var current = getParticipation(player);
    if (current == null) {
      return;
    }
    if (!current.isAttacker || !current.unit.isLeader) {
      return;
    }
    current.unit.isLeader = false;
    updateLeaderRegistrationBarrel();
    broadcast(prefix
      .append(Component.text(player.getName()).color(Colors.orange))
      .append(Component.text("が").color(Colors.white))
      .append(current.color.component())
      .append(Component.text("の大将を辞めました！").color(Colors.white))
    );
    if (current.unit.vehicle != null) {
      current.unit.vehicle.removePotionEffect(PotionEffectType.GLOWING);
    }
    current.unit.attacker.removePotionEffect(PotionEffectType.GLOWING);
  }

  void dismount(Player player) {
    var current = getParticipation(player);
    if (current == null) {
      return;
    }
    if (current.isVehicle) {
      return;
    }
    if (current.unit.isLeader) {
      retireLeader(player);
    }
    var inventory = player.getInventory();
    inventory.setItem(0, CreateSaddle());

    if (current.unit.vehicle != null) {
      remove(current.unit.vehicle);
    }
  }

  void remove(Player player) {
    var current = getParticipation(player);
    if (current == null) {
      return;
    }

    var team = teams.ensure(current.color);
    team.removePlayer(player);
    ClearItems(player);
    player.removePotionEffect(PotionEffectType.GLOWING);

    if (current.isAttacker) {
      registrants.get(current.color).remove(current.unit);

      if (current.unit.isLeader) {
        broadcast(prefix
          .append(Component.text(current.unit.attacker.getName()).color(Colors.orange))
          .append(Component.text("が").color(Colors.white))
          .append(current.color.component())
          .append(Component.text("の大将を辞めました！").color(Colors.white))
        );
        updateLeaderRegistrationBarrel();
        if (current.unit.vehicle != null) {
          current.unit.vehicle.removePotionEffect(PotionEffectType.GLOWING);
        }
      }

      // https://youtu.be/D9vmP7Qj4TI?t=1462
      player.sendMessage(prefix
        .append(Component.text("エントリー登録を解除しました。").color(Colors.white))
      );

      if (current.unit.vehicle != null) {
        team.removePlayer(current.unit.vehicle);
        current.unit.vehicle.removePassenger(player);
        current.unit.vehicle.sendMessage(prefix
          .append(Component.text("騎士があなたから降りたため、エントリーが解除されました。").color(Colors.white))
        );
      }
    } else {
      current.unit.vehicle = null;

      if (player.removePassenger(current.unit.attacker)) {
        player.sendMessage(prefix
          .append(Component.text("エントリー登録を解除しました。").color(Colors.white))
        );
        retireLeader(current.unit.attacker);
        var inventory = current.unit.attacker.getInventory();
        inventory.setItem(0, CreateSaddle());
      } else {
        // https://youtu.be/D9vmP7Qj4TI?t=1217
        player.sendMessage(prefix
          .append(Component.text("騎士があなたから降りたため、エントリーが解除されました。").color(Colors.white))
        );
      }
    }
  }

  @Nullable
  Participation getParticipation(Player player) {
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

  void clearLeaderRegistrationBarrel() {
    var inventory = delegate.registrantsOpenLeaderRegistrationInventory();
    if (inventory == null) {
      return;
    }
    inventory.clear();

    if (!(inventory.getHolder(false) instanceof Barrel barrel)) {
      return;
    }
      // https://youtu.be/gp6ABH58SGA?t=2068
      barrel.customName(prefix.append(Component.text("大将").color(Colors.green)));
      barrel.update();

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

  private void updateLeaderRegistrationBarrel() {
    var inventory = delegate.registrantsOpenLeaderRegistrationInventory();
    if (inventory == null) {
      return;
    }
    for (var color : TeamColor.all) {
      MutableUnit leader = null;
      var units = registrants.get(color);
      if (units != null) {
        for (var unit : units) {
          if (unit.isLeader) {
            leader = unit;
            break;
          }
        }
      }
      var index = 10 + 3 * color.ordinal();
      if (leader == null) {
        inventory.setItem(index, CreateWool(color));
      } else {
        var attacker = leader.attacker;
        var name = color
          .component()
          .appendSpace()
          .append(Component.text("大将").color(Colors.yellow))
          .appendSpace()
          .append(Component.text(attacker.getName()).color(Colors.orange));
        var head = ItemBuilder.For(Material.PLAYER_HEAD)
          .displayName(name)
          .build();
        head.editMeta(SkullMeta.class, (skull) -> skull.setOwningPlayer(attacker));
        inventory.setItem(index, head);
      }
    }
  }
}
