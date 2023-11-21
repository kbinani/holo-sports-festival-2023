package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Barrel;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import java.time.Duration;

import static com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.*;

class Unit {
  final @Nonnull TeamColor color;
  final @Nonnull Player attacker;
  final @Nonnull Player vehicle;
  final @Nonnull Entity healthDisplay;
  final boolean isLeader;
  private int kills = 0;
  private int health = 3;

  Unit(@Nonnull TeamColor color, @Nonnull Player attacker, @Nonnull Player vehicle, @Nonnull Entity healthDisplay, boolean isLeader) {
    this.color = color;
    this.vehicle = vehicle;
    this.attacker = attacker;
    this.healthDisplay = healthDisplay;
    this.isLeader = isLeader;
  }

  int getKills() {
    return kills;
  }

  void kill(Player enemy) {
    kills++;
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    var title = Title.title(
      Component.empty(),
      enemy.teamDisplayName().append(Component.text("を倒しました！").color(Colors.orange)),
      times
    );
    attacker.showTitle(title);
    vehicle.showTitle(title);
    updateActionBar();
  }

  // health が 0 になったら true を返す
  boolean damagedBy(Player enemy) {
    health -= 1;
    if (health < 1) {
      health = 3;
      var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
      var title = Title.title(
        Component.empty(),
        enemy.teamDisplayName().append(Component.text("に倒されました...").color(Colors.orange)),
        times
      );
      attacker.showTitle(title);
      vehicle.showTitle(title);
    }
    healthDisplay.customName(
      Component.text("♥".repeat(health)).color(NamedTextColor.RED)
        .append(Component.text("♡".repeat(3 - health)).color(NamedTextColor.WHITE))
    );
    return health == 3;
  }

  void teleport(Location location) {
    vehicle.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
  }

  void prepare() {
    closeLeaderRegistrationInventory();
    setupAttackerItems();
    setupHealth();
    setupGameMode();
    updateActionBar();
  }

  void clean() {
    vehicle.removePassenger(attacker);
    healthDisplay.remove();
    attacker.removePotionEffect(PotionEffectType.GLOWING);
    vehicle.removePotionEffect(PotionEffectType.GLOWING);
    ClearItems(vehicle);
    ClearItems(attacker);
    ApplyMaxHealth(attacker);
    ApplyMaxHealth(vehicle);
  }

  void tick() {
    updateActionBar();
  }

  private void updateActionBar() {
    var actionBar = Component.text(String.format("現在のキル数: %d", kills)).color(Colors.lime);
    attacker.sendActionBar(actionBar);
    vehicle.sendActionBar(actionBar);
  }

  private void setupGameMode() {
    attacker.setGameMode(GameMode.ADVENTURE);
    vehicle.setGameMode(GameMode.ADVENTURE);
  }

  private void setupHealth() {
    ApplyMaxHealth(attacker);
    ApplyMaxHealth(vehicle);
    attacker.setFoodLevel(20);
    vehicle.setFoodLevel(20);
  }

  private static void ApplyMaxHealth(Player player) {
    var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    if (maxHealth != null) {
      player.setHealth(maxHealth.getValue());
    }
  }

  private void setupAttackerItems() {
    ClearItems(attacker);
    var inventory = attacker.getInventory();
    var sword = ItemBuilder.For(Material.WOODEN_SWORD)
      .customByteTag(itemTag, (byte) 1)
      .build();
    inventory.setItem(0, sword);
    var shield = ItemBuilder.For(Material.SHIELD)
      .customByteTag(itemTag, (byte) 1)
      .build();
    inventory.setItemInOffHand(shield);
    inventory.setHeldItemSlot(0);
  }

  private void closeLeaderRegistrationInventory() {
    var view = attacker.getOpenInventory();
    for (var index = 0; index < view.countSlots(); index++) {
      var inventory = view.getInventory(index);
      if (inventory == null) {
        continue;
      }
      var holder = inventory.getHolder();
      if (holder == null) {
        continue;
      }
      if (holder instanceof Barrel barrel) {
        var pos = new Point3i(barrel.getLocation());
        if (pos.equals(leaderRegistrationBarrel)) {
          view.close();
          break;
        }
      }
    }
  }
}
