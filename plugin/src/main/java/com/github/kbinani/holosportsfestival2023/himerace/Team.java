package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.*;
import com.github.kbinani.holosportsfestival2023.himerace.stage.cook.Task;
import com.github.kbinani.holosportsfestival2023.himerace.stage.cook.TaskItem;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.*;
import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.CreateRecipeBook0;
import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.CreateRecipeBook1;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class Team implements Level.Delegate {
  interface Delegate {
    void teamDidFinish(TeamColor color);
  }

  static final @Nonnull UUID maxHealthModifierUUID = UUID.fromString("CA4218FF-B7D7-4E16-8ECD-942513E88E22");
  private static final @Nonnull String maxHealthModifierName = "hololive_sports_festival_2023_himerace";

  private final JavaPlugin owner;
  private final TeamColor color;
  private @Nullable Player princess;
  private final List<Player> knights = new LinkedList<>();
  private final Map<UUID, HealthDisplay> healthDisplays = new HashMap<>();
  private static final int kMaxKnightPlayers = 2;
  private final Level level;
  private final Teams teams;
  @Nullable
  Delegate delegate;

  Team(JavaPlugin owner, TeamColor color, Level level, Teams teams) {
    this.owner = owner;
    this.color = color;
    level.delegate.set(this);
    this.level = level;
    this.teams = teams;
  }

  void onStart() {
    var team = this.teams.ensure(color);
    if (princess != null) {
      activateHealthModifier(princess);
      princess.setGameMode(GameMode.ADVENTURE);
      team.addEntity(princess);
    }
    for (var knight : knights) {
      knight.setGameMode(GameMode.ADVENTURE);
      team.addEntity(knight);
    }
  }

  private void clearInventoryExceptEquipments(Player player) {
    var equipment = player.getEquipment();
    var helmet = equipment.getHelmet();
    var chestplate = equipment.getChestplate();
    var leggings = equipment.getLeggings();
    var boots = equipment.getBoots();
    player.getInventory().clear();
    equipment.setHelmet(helmet);
    equipment.setChestplate(chestplate);
    equipment.setLeggings(leggings);
    equipment.setBoots(boots);
  }

  @Override
  public void levelDidClearStage(Stage stage) {
    if (princess != null) {
      clearInventoryExceptEquipments(princess);
    }
    for (var knight : knights) {
      clearInventoryExceptEquipments(knight);
    }
    switch (stage) {
      case CARRY -> {
        if (princess != null) {
          var book = ItemBuilder.For(Material.BOOK)
            .customByteTag(itemTag)
            .customByteTag(Stage.BUILD.tag)
            .displayName(text("回答する！(右クリックで開く) / Answer Book (Right click to open)", AQUA))
            .build();
          var inventory = princess.getInventory();
          inventory.setItem(0, book);
          princess.setGameMode(GameMode.ADVENTURE);
        }
        for (var knight : knights) {
          knight.setGameMode(GameMode.CREATIVE);
        }
      }
      case BUILD -> {
        if (princess != null) {
          princess.setFoodLevel(2);
          princess.chat("お腹が空いてきちゃった・・・(I'm so hungry...)");
          var inventory = princess.getInventory();
          inventory.setItem(0, CreateRecipeBook0());
          inventory.setItem(1, CreateRecipeBook1());
          princess.setGameMode(GameMode.ADVENTURE);
        }
        for (var knight : knights) {
          var inventory = knight.getInventory();
          var emerald = Task.ToItem(TaskItem.EMERALD, 20);
          inventory.setItem(0, emerald);
          knight.setGameMode(GameMode.SURVIVAL);
        }
      }
      case COOK -> {
        if (princess != null) {
          princess.setFoodLevel(20);
          princess.setGameMode(GameMode.ADVENTURE);
        }
        for (var knight : knights) {
          knight.setGameMode(GameMode.ADVENTURE);
        }
      }
      case SOLVE -> {
        activateHealthDisplays();
        if (princess != null) {
          var inventory = princess.getInventory();
          inventory.setItem(0, ItemBuilder.For(Material.GOLDEN_SHOVEL)
            .customByteTag(itemTag)
            .customByteTag(Stage.FIGHT.tag)
            .displayName(text("回復の杖 (右クリックで使用) / Healing Wand (Right click to use)", GOLD))
            .build());
          inventory.setItem(1, ItemBuilder.For(Material.GOAT_HORN)
            .customByteTag(itemTag)
            .customByteTag(Stage.FIGHT.tag)
            .displayName(text("ヤギの角笛 (右クリックで使用) / Goat Horn (Right click to use)", GOLD))
            .meta(MusicInstrumentMeta.class, (it) -> {
              it.setInstrument(MusicInstrument.SING);
            })
            .build());
          inventory.setItem(2, ItemBuilder.For(Material.RED_BED)
            .customByteTag(itemTag)
            .customByteTag(Stage.FIGHT.tag)
            .displayName(text("ベッド (右クリックで使用) / Bed (Right click to use)", GOLD))
            .build());
          princess.setGameMode(GameMode.ADVENTURE);
        }
        for (var knight : knights) {
          var inventory = knight.getInventory();
          var sword = ItemBuilder.For(Material.IRON_SWORD)
            .customByteTag(itemTag)
            .customByteTag(Stage.FIGHT.tag)
            .build();
          inventory.setItem(0, sword);
          inventory.setHeldItemSlot(0);
          var shield = ItemBuilder.For(Material.SHIELD)
            .customByteTag(itemTag)
            .customByteTag(Stage.FIGHT.tag)
            .build();
          inventory.setItemInOffHand(shield);
          var bow = ItemBuilder.For(Material.BOW)
            .customByteTag(itemTag)
            .customByteTag(Stage.FIGHT.tag)
            .enchant(Enchantment.ARROW_INFINITE, 1)
            .build();
          inventory.setItem(1, bow);
          var arrow = ItemBuilder.For(Material.ARROW)
            .customByteTag(itemTag)
            .customByteTag(Stage.FIGHT.tag)
            .build();
          inventory.setItem(2, arrow);
          knight.setGameMode(GameMode.ADVENTURE);
        }
      }
      case FIGHT -> {
        deactivateHealthDisplays();
        if (princess != null) {
          deactivateHealthModifier(princess);
          princess.setGameMode(GameMode.ADVENTURE);
        }
        for (var knight : knights) {
          knight.setGameMode(GameMode.ADVENTURE);
        }
      }
      case GOAL -> {
        if (princess != null) {
          princess.setGameMode(GameMode.ADVENTURE);
        }
        for (var knight : knights) {
          knight.setGameMode(GameMode.ADVENTURE);
        }
        if (delegate != null) {
          delegate.teamDidFinish(color);
        }
      }
    }
  }

  private void activateHealthDisplays() {
    for (var display : healthDisplays.values()) {
      display.dispose();
    }
    healthDisplays.clear();
    if (princess != null) {
      var display = new HealthDisplay(owner, princess, itemTag);
      healthDisplays.put(princess.getUniqueId(), display);
    }
    for (var knight : knights) {
      var display = new HealthDisplay(owner, knight, itemTag);
      healthDisplays.put(knight.getUniqueId(), display);
    }
  }

  private void deactivateHealthDisplays() {
    for (var display : healthDisplays.values()) {
      display.dispose();
    }
    healthDisplays.clear();
  }

  private void activateHealthModifier(Player player) {
    var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    if (attribute == null) {
      return;
    }
    player.setHealth(6);
    if (attribute.getModifier(maxHealthModifierUUID) != null) {
      attribute.removeModifier(maxHealthModifierUUID);
    }
    attribute.addModifier(createHealthModifier());
  }

  private AttributeModifier createHealthModifier() {
    return new AttributeModifier(
      maxHealthModifierUUID, maxHealthModifierName, 6 - 20, AttributeModifier.Operation.ADD_NUMBER);
  }

  private void deactivateHealthModifier(Player player) {
    var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    if (attribute == null) {
      return;
    }
    attribute.removeModifier(maxHealthModifierUUID);
    player.setHealth(attribute.getValue());
  }

  @Override
  public void levelSignalActionBarUpdate() {
    updateActionBar();
  }

  @Override
  public void levelSendTitle(Title title) {
    if (princess != null) {
      princess.showTitle(title);
    }
    for (var knight : knights) {
      knight.showTitle(title);
    }
  }

  @Override
  public void levelPlaySound(Sound sound) {
    if (princess != null) {
      princess.playSound(princess.getLocation(), sound, 1, 1);
    }
    for (var knight : knights) {
      knight.playSound(knight.getLocation(), sound, 1, 1);
    }
  }

  @Override
  public void levelRequestsTeleport(Location location, @Nullable Function<Player, Boolean> predicate) {
    if (princess != null) {
      if (predicate == null || predicate.apply(princess)) {
        princess.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND, TeleportFlag.EntityState.RETAIN_PASSENGERS);
      }
    }
    for (var knight : knights) {
      if (predicate == null || predicate.apply(knight)) {
        knight.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND, TeleportFlag.EntityState.RETAIN_PASSENGERS);
      }
    }
  }

  @Override
  public void levelRequestsHealthRecovery() {
    if (princess != null) {
      var maxHealth = princess.getAttribute(Attribute.GENERIC_MAX_HEALTH);
      if (maxHealth != null) {
        princess.setHealth(maxHealth.getValue());
      }
    }
    for (var knight : knights) {
      var maxHealth = knight.getAttribute(Attribute.GENERIC_MAX_HEALTH);
      if (maxHealth != null) {
        knight.setHealth(maxHealth.getValue());
      }
    }
  }

  @Override
  public void levelRequestsEncouragingKnights(Set<UUID> excludeHealthRecovery) {
    if (princess != null) {
      princess.sendMessage(prefix.append(text("みんなを鼓舞しました！", WHITE)));
    }
    for (var knight : knights) {
      knight.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 10 * 20, 0));
      knight.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 0));
      knight.sendMessage(prefix.append(text("姫がみんなを鼓舞しています！", WHITE)));
      if (!excludeHealthRecovery.contains(knight.getUniqueId())) {
        var maxHealth = knight.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
          knight.setHealth(maxHealth.getValue());
        }
      }
    }
  }

  @Override
  public @Nullable Player levelRequestsVisibleAlivePlayer(Mob enemy) {
    record Candidate(Player player, double distance) {
    }
    var candidates = new ArrayList<Candidate>();
    for (var knight : knights) {
      var seat = knight.getVehicle();
      if (seat != null) {
        continue;
      }
      if (enemy.getPathfinder().findPath(knight) != null) {
        candidates.add(new Candidate(knight, knight.getLocation().toVector().distanceSquared(enemy.getLocation().toVector())));
      }
    }
    if (princess != null) {
      if (enemy.getPathfinder().findPath(princess) != null) {
        candidates.add(new Candidate(princess, princess.getLocation().toVector().distanceSquared(enemy.getLocation().toVector())));
      }
    }
    if (candidates.isEmpty()) {
      return null;
    }
    candidates.sort(Comparator.comparingDouble(it -> it.distance));
    var target = candidates.stream().findFirst();
    return target.map(candidate -> candidate.player).orElse(null);
  }

  @Override
  public void levelRequestsClearGoatHornCooltime() {
    if (princess != null) {
      princess.setCooldown(Material.GOAT_HORN, 0);
    }
  }

  void dispose() {
    var team = teams.ensure(color);
    if (princess != null) {
      ClearItems(princess, itemTag);
      team.removeEntity(princess);
      Cloakroom.shared.restore(princess);
    }
    for (var knight : knights) {
      ClearItems(knight, itemTag);
      team.removeEntity(knight);
      Cloakroom.shared.restore(knight);
    }
  }

  boolean add(Player player, Role role) {
    if (getCurrentRole(player) != null) {
      //TODO: エラーメッセージ
      return false;
    }
    return switch (role) {
      case PRINCESS -> {
        if (princess == null) {
          if (!Cloakroom.shared.store(player, prefix)) {
            yield false;
          }
          princess = player;
          yield true;
        } else {
          //TODO: エラーメッセージ
          yield false;
        }
      }
      case KNIGHT -> {
        if (kMaxKnightPlayers > knights.size()) {
          if (!Cloakroom.shared.store(player, prefix)) {
            yield false;
          }
          knights.add(player);
          yield true;
        } else {
          //TODO: エラーメッセージ
          yield false;
        }
      }
    };
  }

  void remove(Player player) {
    if (player == princess) {
      princess = null;
    }
    knights.remove(player);
    Cloakroom.shared.restore(player);
  }

  public List<Player> getKnights() {
    return new LinkedList<>(knights);
  }

  public @Nullable Player getPrincess() {
    return princess;
  }

  @Nullable
  Role getCurrentRole(Player player) {
    if (princess != null && princess.getUniqueId().equals(player.getUniqueId())) {
      return Role.PRINCESS;
    }
    if (knights.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()))) {
      return Role.KNIGHT;
    }
    return null;
  }

  int size() {
    int i = knights.size();
    if (princess != null) {
      i++;
    }
    return i;
  }

  Component getBossBarName() {
    var stage = level.getActive();
    return color.component().append(text(String.format(" %s", stage.description), WHITE));
  }

  float getProgress() {
    return level.getProgress();
  }

  void tick() {
    level.tick();
    updateActionBar();
    healthDisplays.values().forEach(HealthDisplay::update);
  }

  private void updateActionBar() {
    if (princess != null) {
      princess.sendActionBar(level.getActionBar(Role.PRINCESS));
    }
    for (var knight : knights) {
      knight.sendActionBar(level.getActionBar(Role.KNIGHT));
    }
  }
}
