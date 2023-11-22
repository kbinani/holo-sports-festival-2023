package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Barrel;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

import static com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.*;
import static com.github.kbinani.holosportsfestival2023.kibasen.Session.maxHealthModifierName;
import static com.github.kbinani.holosportsfestival2023.kibasen.Session.maxHealthModifierUUID;

class Unit {
  private final JavaPlugin owner;
  final @Nonnull TeamColor color;
  final @Nonnull Player attacker;
  final @Nonnull Player vehicle;
  private @Nullable Entity healthDisplay;
  final boolean isLeader;
  private int kills = 0;
  private int health;
  private final int maxHealth;
  private @Nullable BukkitTask delayMountTask;

  Unit(JavaPlugin owner, @Nonnull TeamColor color, @Nonnull Player attacker, @Nonnull Player vehicle, boolean isLeader) {
    this.owner = owner;
    this.color = color;
    this.vehicle = vehicle;
    this.attacker = attacker;
    this.isLeader = isLeader;
    if (isLeader) {
      this.maxHealth = 5;
    } else {
      this.maxHealth = 3;
    }
    this.health = maxHealth;
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
      health = maxHealth;
      var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
      var title = Title.title(
        Component.empty(),
        enemy.teamDisplayName().append(Component.text("に倒されました...").color(Colors.orange)),
        times
      );
      attacker.showTitle(title);
      vehicle.showTitle(title);
    }
    var display = ensureHealthDisplayEntity();
    display.customName(createHealthDisplayComponent());
    return health == maxHealth;
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
    ensureHealthDisplayEntity();
  }

  void clean() {
    vehicle.removePassenger(attacker);
    if (delayMountTask != null) {
      delayMountTask.cancel();
      delayMountTask = null;
    }
    if (healthDisplay != null) {
      healthDisplay.remove();
    }
    attacker.removePotionEffect(PotionEffectType.GLOWING);
    vehicle.removePotionEffect(PotionEffectType.GLOWING);
    ClearItems(vehicle);
    ClearItems(attacker);
    deactivateHealthModifier(attacker);
    deactivateHealthModifier(vehicle);
  }

  void tick() {
    updateActionBar();
  }

  private Component createHealthDisplayComponent() {
    return Component.text("♥".repeat(health)).color(NamedTextColor.RED)
      .append(Component.text("♡".repeat(maxHealth - health)).color(NamedTextColor.WHITE));
  }

  private @Nonnull Entity ensureHealthDisplayEntity() {
    if (this.healthDisplay != null) {
      return this.healthDisplay;
    }
    if (this.delayMountTask != null) {
      this.delayMountTask.cancel();
    }
    var location = attacker.getLocation();
    location.setY(location.getBlockY() - 8);
    var display = attacker.getWorld().spawn(location, AreaEffectCloud.class, (it) -> {
      it.setCustomNameVisible(false);
      it.setInvulnerable(true);
      it.setDuration(365 * 24 * 60 * 60 * 20);
      it.setRadius(0);
      it.addScoreboardTag(healthDisplayScoreboardTag);
    });
    this.delayMountTask = Bukkit.getScheduler().runTaskLater(owner, () -> {
      // particle が出現する瞬間が見えないように ride を遅らせる
      attacker.addPassenger(display);
      display.customName(createHealthDisplayComponent());
      display.setCustomNameVisible(true);
      this.delayMountTask = null;
    }, 30);
    this.healthDisplay = display;
    return healthDisplay;
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
    activateHealthModifier(attacker);
    activateHealthModifier(vehicle);
    attacker.setFoodLevel(20);
    vehicle.setFoodLevel(20);
  }

  private void activateHealthModifier(Player player) {
    var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    if (attribute == null) {
      return;
    }
    player.setHealth(2 * maxHealth);
    attribute.addModifier(createHealthModifier());
  }

  private AttributeModifier createHealthModifier() {
    return new AttributeModifier(
      maxHealthModifierUUID, maxHealthModifierName, 2 * maxHealth - 20, AttributeModifier.Operation.ADD_NUMBER);
  }

  private void deactivateHealthModifier(Player player) {
    var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    if (attribute == null) {
      return;
    }
    attribute.removeModifier(createHealthModifier());
    player.setHealth(attribute.getValue());
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
