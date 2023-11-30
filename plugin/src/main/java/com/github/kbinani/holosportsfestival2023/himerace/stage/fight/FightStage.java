package  com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.ItemTag;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.Stage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.itemTag;
import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.title;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class FightStage extends AbstractStage {
  // wave1
  //   クモx2
  //   ゾンビx2
  //   スケルトン
  // wave2
  //   ブレイズx2
  //   ホグリンx2
  //   ピグリンブルート
  // wave3
  //   ヴィンディケーターx2
  //   ピリジャーx2
  //   イリュージョナー
  public interface Delegate {
    void fightStagePlaySound(Sound sound);

    void fightStageSendTitle(Title title);

    void fightStageRequestsTeleport(Location location, @Nullable Function<Player, Boolean> predicate);

    void fightStageRequestsHealthRecovery();

    void fightStageRequestsEncouragingKnights();

    void fightStageRequestsClearGoatHornCooltime();

    void fightStageDidFinish();
  }

  private static final @Nonnull UUID attackDamageModifierUUID = UUID.fromString("8968EEE4-6CA0-4AE9-93DA-108A1278B2B5");
  private static final @Nonnull String attackDamageModifierName = "hololive_sports_festival_2023_himerace_fight_stage";

  final @Nonnull Delegate delegate;
  private Wave wave = Wave.Wave1;
  private int waveProgress = 0;
  private int waveRound = 0;
  private final Point3i signPos = pos(-94, 80, 55);
  private final Point3i enemyPosLeft = pos(-90, 82, 82);
  private final Point3i enemyPosMiddleLeft = pos(-91, 80, 79);
  private final Point3i enemyPosMiddleRight = pos(-97, 80, 79);
  private final Point3i enemyPosRight = pos(-98, 82, 82);
  private final Point3i enemyPosCenterLow = pos(-94, 81, 81);
  private final Point3i enemyPosCenterHigh = pos(-94, 89, 81);
  private final Point3i safeArea = pos(-94, 80, 53);
  private final String stageEnemyTag;
  private final Map<UUID, Entity> deadPlayerSeats = new HashMap<>();
  private final List<Mob> enemies = new ArrayList<>();

  public FightStage(
    @Nonnull World world,
    @Nonnull JavaPlugin owner,
    @Nonnull Point3i origin,
    @Nonnull Point3i southEast,
    @Nonnull Delegate delegate) //
  {
    super(world, owner, origin, southEast.x - origin.x, southEast.z - origin.z);
    this.delegate = delegate;
    this.stageEnemyTag = UUID.randomUUID().toString();
  }

  @Override
  protected void onStart() {
    updateStandingSign(Wave.Wave1);
  }

  @Override
  protected void onFinish() {
    delegate.fightStageDidFinish();
  }

  @Override
  protected void onReset() {
    closeGate();
    wave = Wave.Wave1;
    waveProgress = 0;
    waveRound = 0;
    updateStandingSign(Wave.Wave1);
    Kill.EntitiesByScoreboardTag(world, Stage.FIGHT.tag);
    setEnableFence(true);
    for (var seat : deadPlayerSeats.values()) {
      seat.remove();
    }
    deadPlayerSeats.clear();
    for (var enemy : enemies) {
      enemy.remove();
    }
    enemies.clear();
  }

  @Override
  protected void onEntityDeath(EntityDeathEvent e) {
    var entity = e.getEntity();
    if (!enemies.remove(entity)) {
      return;
    }
    e.setDroppedExp(0);

    updateWaveProgress();
  }

  @Override
  protected void onEntitySpawn(EntitySpawnEvent e) {
    if (!(e.getEntity() instanceof Item item)) {
      return;
    }
    if (!bounds.contains(e.getLocation().toVector())) {
      return;
    }
    var stack = item.getItemStack();
    if (ItemTag.HasByte(stack, itemTag)) {
      return;
    }
    //NOTE: Entity#clearLootTable が効いていないのでここで削除する
    item.remove();
  }

  @Override
  protected void onPlayerDeath(PlayerDeathEvent e, Participation participation) {
    e.setCancelled(true);

    switch (participation.role) {
      case PRINCESS -> {
        waveRound++;
        waveProgress = 0;

        // https://youtu.be/dDv4L4rHwGU?t=726
        var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
        var title = Title.title(
          text("姫が倒されました・・・", RED),
          Component.empty(),
          times
        );
        delegate.fightStageSendTitle(title);

        enemies.forEach(Mob::remove);
        enemies.clear();
        clearDeadPlayerSeats();
        setEnableFence(true);
        updateStandingSign(wave);
        Bukkit.getScheduler().runTaskLater(owner, () -> {
          delegate.fightStageRequestsTeleport(
            safeArea.toLocation(world).add(0.5, 0, 0.5),
            this::playerNeedsEvacuation
          );
        }, 0);
        delegate.fightStageRequestsHealthRecovery();
      }
      case KNIGHT -> {
        var player = e.getPlayer();
        var location = player.getLocation();
        location.setY(y(78));
        var seat = world.spawn(location, ArmorStand.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
          it.addScoreboardTag(itemTag);
          it.addScoreboardTag(Stage.FIGHT.tag);
          it.setVisible(false);
          it.setGravity(false);
        });
        seat.addPassenger(player);
        var id = player.getUniqueId();
        var current = deadPlayerSeats.get(id);
        if (current != null) {
          current.remove();
        }
        deadPlayerSeats.put(id, seat);

        for (var enemy : enemies) {
          if (enemy.getTarget() == player) {
            enemy.setTarget(null);
          }
        }
      }
    }
  }

  @Override
  protected void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent e, Participation participation) {
    if (!(e.getTarget() instanceof Player player)) {
      return;
    }
    if (deadPlayerSeats.containsKey(player.getUniqueId())) {
      e.setCancelled(true);
    }
  }

  @Override
  protected void onPlayerInteractEntity(PlayerInteractEntityEvent e, Role actor, Role rightClicked) {
    if (actor != Role.PRINCESS || rightClicked != Role.KNIGHT) {
      return;
    }
    var hand = e.getHand();
    var princess = e.getPlayer();
    var inventory = princess.getInventory();
    var usedItem = inventory.getItem(hand);
    if (usedItem.getType() == Material.RED_BED && ItemTag.HasByte(usedItem, Stage.FIGHT.tag) && ItemTag.HasByte(usedItem, itemTag)) {
      if (e.getRightClicked() instanceof Player knight) {
        var seat = deadPlayerSeats.get(knight.getUniqueId());
        if (seat != null) {
          seat.removePassenger(knight);
          seat.remove();
          deadPlayerSeats.remove(knight.getUniqueId());
          Recover(knight);
          var location = knight.getLocation();
          location.setY(y(80));
          Bukkit.getScheduler().runTaskLater(owner, () -> {
            knight.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND, TeleportFlag.EntityState.RETAIN_PASSENGERS);
          }, 0);
        }
      }
    }
  }

  @Override
  protected void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof LivingEntity defender)) {
      return;
    }
    var isMobDefender = enemies.stream().anyMatch(it -> it.getUniqueId().equals(defender.getUniqueId()));
    LivingEntity attacker;
    if (e.getDamager() instanceof LivingEntity damager) {
      attacker = damager;
    } else if (e.getDamager() instanceof Projectile projectile) {
      if (projectile.getShooter() instanceof LivingEntity damager) {
        attacker = damager;
      } else {
        return;
      }
    } else {
      return;
    }
    var isMobAttacker = enemies.stream().anyMatch(it -> it.getUniqueId().equals(attacker.getUniqueId()));
    if (isMobAttacker && isMobDefender) {
      e.setCancelled(true);
      return;
    }
    if (defender instanceof Player defendingPlayer && isMobAttacker) {
      if (deadPlayerSeats.containsKey(defendingPlayer.getUniqueId())) {
        e.setCancelled(true);
        if (attacker instanceof Hoglin) {
          unlinkHostileHoglins();
        } else if (attacker instanceof PiglinBrute) {
          unlinkHostilePiglinBrute();
        }
        return;
      }
    }
    if (attacker instanceof Player attackingPlayer) {
      if (deadPlayerSeats.containsKey(attackingPlayer.getUniqueId())) {
        e.setCancelled(true);
        return;
      }
    }
  }

  @Override
  protected void onEntityDismount(EntityDismountEvent e, Participation participation) {
    if (!(e.getEntity() instanceof Player player)) {
      return;
    }
    if (deadPlayerSeats.containsKey(player.getUniqueId())) {
      e.setCancelled(true);
    }
  }

  @Override
  protected void onTick() {
    var enemies = new ArrayList<Mob>(this.enemies);
    for (var enemy : enemies) {
      var target = enemy.getTarget();
      if (target != null) {
        if (target instanceof Player player) {
          if (deadPlayerSeats.containsKey(player.getUniqueId())) {
            enemy.setTarget(null);
          }
        } else {
          enemy.setTarget(null);
        }
      }
    }
  }

  private void unlinkHostileHoglins() {
    //NOTE: hoglin は setTarget(null) としても敵対状態が解除されないので, いったん kill して別個体を同じ位置にスポーンさせる.
    var locations = new ArrayList<Location>();
    for (var enemy : new ArrayList<Mob>(enemies)) {
      if (enemy instanceof Hoglin) {
        locations.add(enemy.getLocation());
        enemy.remove();
        this.enemies.remove(enemy);
      }
    }
    for (var location : locations) {
      this.enemies.add(summonHoglin(location, wave, waveRound));
    }
  }

  private void unlinkHostilePiglinBrute() {
    //NOTE: piglin_brute は setTarget(null) としても敵対状態が解除されないので, いったん kill して別個体を同じ位置にスポーンさせる.
    var locations = new ArrayList<Location>();
    for (var enemy : new ArrayList<Mob>(enemies)) {
      if (enemy instanceof PiglinBrute) {
        locations.add(enemy.getLocation());
        enemy.remove();
        this.enemies.remove(enemy);
      }
    }
    for (var location : locations) {
      this.enemies.add(summonPiglinBrute(location, wave, waveRound));
    }
  }

  private static void Recover(Player player) {
    var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    if (maxHealth != null) {
      player.setHealth(maxHealth.getValue());
    }
    player.setFoodLevel(20);
  }

  private void summonEnemies(Wave wave, int round) {
    switch (wave) {
      case Wave1 -> {
        enemies.add(summonSpider(enemyPosLeft, wave, round));
        enemies.add(summonSpider(enemyPosRight, wave, round));
        enemies.add(summonZombie(enemyPosMiddleLeft, wave, round));
        enemies.add(summonZombie(enemyPosMiddleRight, wave, round));
        enemies.add(summonSkeleton(enemyPosCenterLow, wave, round));
      }
      case Wave2 -> {
        enemies.add(summonBlaze(enemyPosLeft, wave, round));
        enemies.add(summonHoglin(enemyPosMiddleLeft.toLocation(world, 0, 180).add(0.5, 0, 0.5), wave, round));
        enemies.add(summonPiglinBrute(enemyPosCenterLow.toLocation(world, 0, 180).add(0.5, 0, 0.5), wave, round));
        enemies.add(summonHoglin(enemyPosMiddleRight.toLocation(world, 0, 180).add(0.5, 0, 0.5), wave, round));
        enemies.add(summonBlaze(enemyPosRight, wave, round));
      }
      case Wave3 -> {
        enemies.add(summonPillager(enemyPosLeft, wave, round));
        enemies.add(summonVindicator(enemyPosMiddleLeft, wave, round));
        enemies.add(summonIllusioner(enemyPosCenterHigh, wave, round));
        enemies.add(summonVindicator(enemyPosMiddleRight, wave, round));
        enemies.add(summonPillager(enemyPosRight, wave, round));
      }
    }
  }

  private void updateWaveProgress() {
    var remaining = enemies.size();
    waveProgress = 5 - remaining;
    if (remaining > 0) {
      return;
    }
    var next = this.wave.next();
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    if (next == null) {
      var title = Title.title(
        text("Wave.3 ", GOLD).append(text("クリア！", GREEN)),
        text("姫が感圧板を踏んだらゴール！", AQUA),
        times
      );
      clearDeadPlayerSeats();
      delegate.fightStageSendTitle(title);
      delegate.fightStagePlaySound(Sound.ENTITY_PLAYER_LEVELUP);
      delegate.fightStageRequestsHealthRecovery();

      delegate.fightStageDidFinish();
      return;
    }
    this.wave = next;
    this.waveProgress = 0;
    this.waveRound = 0;
    clearDeadPlayerSeats();
    delegate.fightStageRequestsTeleport(
      safeArea.toLocation(world).add(0.5, 0, 0.5),
      this::playerNeedsEvacuation
    );
    setEnableFence(true);
    updateStandingSign(next);
    var title = Title.title(
      text(String.format("Wave.%d", next.ordinal()), GOLD).append(text("クリア！", GREEN)),
      text("次のウェーブに備えましょう！", AQUA),
      times
    );
    delegate.fightStageSendTitle(title);
    delegate.fightStagePlaySound(Sound.ENTITY_PLAYER_LEVELUP);
    delegate.fightStageRequestsHealthRecovery();
    delegate.fightStageRequestsClearGoatHornCooltime();
  }

  private void clearDeadPlayerSeats() {
    for (var entry : deadPlayerSeats.entrySet()) {
      var seat = entry.getValue();
      seat.remove();
    }
    deadPlayerSeats.clear();
  }

  private boolean playerNeedsEvacuation(Player player) {
    var z = player.getZ();
    return z < z(51) || z(56) < z;
  }

  private void setEnableFence(boolean enable) {
    var material = enable ? "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]" : "air";
    Editor.Fill(world, pos(-98, 80, 56), pos(-90, 80, 56), material);
  }

  private void setupEnemy(Mob mob, Wave wave, int round) {
    mob.addScoreboardTag(itemTag);
    mob.addScoreboardTag(Stage.FIGHT.tag);
    mob.addScoreboardTag(wave.tag);
    mob.addScoreboardTag(stageEnemyTag);
    //NOTE: clearLootTable が効いていない
    mob.clearLootTable();
    mob.setPersistent(true);
    mob.setCanPickupItems(false);
    mob.setRemoveWhenFarAway(false);

    var attackDamage = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
    if (attackDamage != null) {
      var modifier = new AttributeModifier(
        attackDamageModifierUUID, attackDamageModifierName,
        Math.pow(0.8, round) - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1
      );
      attackDamage.addModifier(modifier);
    }
  }

  private Mob summonZombie(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Zombie.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      it.setAdult();
      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
      equipment.setItemInMainHand(new ItemStack(Material.IRON_AXE));
    });
  }

  private Mob summonSpider(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Spider.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      for (var passenger : it.getPassengers()) {
        passenger.remove();
      }
    });
  }

  private Mob summonSkeleton(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Skeleton.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
      equipment.setItemInMainHand(new ItemStack(Material.BOW));
      equipment.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
      equipment.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
      equipment.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    });
  }

  private Mob summonBlaze(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Blaze.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);
    });
  }

  private Mob summonHoglin(Location location, Wave w, int round) {
    return world.spawn(location, Hoglin.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      it.setAdult();
      it.setImmuneToZombification(true);
    });
  }

  private Mob summonPiglinBrute(Location location, Wave w, int round) {
    return world.spawn(location, PiglinBrute.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      it.setImmuneToZombification(true);
      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setItemInMainHand(new ItemStack(Material.GOLDEN_AXE));
    });
  }

  private Mob summonPillager(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world).add(0.5, 0, 0.5), Pillager.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setItemInMainHand(new ItemStack(Material.CROSSBOW));
    });
  }

  private Mob summonVindicator(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Vindicator.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setItemInMainHand(new ItemStack(Material.IRON_AXE));
    });
  }

  private Mob summonIllusioner(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Illusioner.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      it.setAI(false);
      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
    });
  }

  private void updateStandingSign(Wave wave) {
    Editor.StandingSign(world, signPos, Material.CRIMSON_SIGN, 8,
      title,
      text("右クリでスタート！", GOLD),
      text(String.format("Wave.%d", wave.ordinal() + 1), GOLD),
      text("姫専用", LIGHT_PURPLE));
  }

  @Override
  public float getProgress() {
    if (finished) {
      return 1.0f;
    } else {
      return Math.max(0.0f, (wave.ordinal() + waveProgress / 5.0f) / 3.0f);
    }
  }

  @Override
  public @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case PRINCESS -> text("騎士と一緒にモンスターを倒そう！", GREEN);
      case KNIGHT -> text("姫と一緒にモンスターを倒そう！", GREEN);
    };
  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    if (participation.role != Role.PRINCESS) {
      return;
    }
    switch (e.getAction()) {
      case RIGHT_CLICK_BLOCK -> {
        var block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        var item = e.getItem();
        if (item != null && ItemTag.HasByte(item, itemTag) && ItemTag.HasByte(item, Stage.FIGHT.tag)) {
          switch (item.getType()) {
            case RED_BED -> e.setCancelled(true);
            case GOAT_HORN -> {
              delegate.fightStageRequestsHealthRecovery();
              delegate.fightStageRequestsEncouragingKnights();
            }
          }
        }
        var location = new Point3i(block.getLocation());
        if (!location.equals(signPos)) {
          return;
        }
        if (wave == Wave.Wave1 && waveRound == 0) {
          delegate.fightStageRequestsTeleport(safeArea.toLocation(world).add(0.5, 0, 0.5), (p) -> {
            var z = p.getLocation().getZ();
            return z < z(51);
          });
          closeGate();
        }
        summonEnemies(wave, waveRound);
        setEnableFence(false);
        Editor.Set(world, signPos, Material.AIR);
      }
      case RIGHT_CLICK_AIR -> {
        var item = e.getItem();
        if (item != null && item.getType() == Material.GOAT_HORN && ItemTag.HasByte(item, itemTag) && ItemTag.HasByte(item, Stage.FIGHT.tag)) {
          delegate.fightStageRequestsEncouragingKnights();
        }
      }
    }
  }

  private int x(int x) {
    return x + 100 + origin.x;
  }

  private int y(int y) {
    return y - 80 + origin.y;
  }

  private int z(int z) {
    return z - 49 + origin.z;
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x(x), y(y), z(z));
  }

  private static void DisableDrop(EntityEquipment e) {
    e.setHelmetDropChance(0);
    e.setItemInMainHandDropChance(0);
    e.setItemInOffHandDropChance(0);
    e.setChestplateDropChance(0);
    e.setLeggingsDropChance(0);
    e.setBootsDropChance(0);
  }
}
