package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.HealthDisplay;
import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

import static com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.*;
import static com.github.kbinani.holosportsfestival2023.kibasen.Session.maxHealthModifierName;
import static com.github.kbinani.holosportsfestival2023.kibasen.Session.maxHealthModifierUUID;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

class Unit {
  private final JavaPlugin owner;
  final @Nonnull TeamColor color;
  final @Nonnull Player attacker;
  final @Nonnull Player vehicle;
  private @Nullable HealthDisplay healthDisplay;
  final boolean isLeader;
  private int kills = 0;
  private int health;
  private final int maxHealth;

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
      enemy.teamDisplayName().append(text("を倒しました！", GOLD)),
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
        enemy.teamDisplayName().append(text("に倒されました...", GOLD)),
        times
      );
      attacker.showTitle(title);
      vehicle.showTitle(title);
    }
    attacker.setHealth(2 * health);
    vehicle.setHealth(2 * health);
    var display = ensureHealthDisplay();
    display.update();
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
    ensureHealthDisplay();
  }

  void clean() {
    vehicle.removePassenger(attacker);
    if (healthDisplay != null) {
      healthDisplay.dispose();
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

  private @Nonnull HealthDisplay ensureHealthDisplay() {
    if (this.healthDisplay != null) {
      return this.healthDisplay;
    }
    var display = new HealthDisplay(owner, attacker, healthDisplayScoreboardTag);
    this.healthDisplay = display;
    return display;
  }

  private void updateActionBar() {
    var actionBar = text(String.format("現在のキル数: %d", kills), GREEN);
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
      .customTag(itemTag)
      .build();
    inventory.setItem(0, sword);
    var shield = ItemBuilder.For(Material.SHIELD)
      .customTag(itemTag)
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
