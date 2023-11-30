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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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
        deadPlayerSeats.values().forEach(it -> {
          it.getPassengers().forEach(it::removePassenger);
        });
        deadPlayerSeats.clear();
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
        enemies.add(summonHoglin(enemyPosMiddleLeft, wave, round));
        enemies.add(summonPiglinBrute(enemyPosCenterLow, wave, round));
        enemies.add(summonHoglin(enemyPosMiddleRight, wave, round));
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
      delegate.fightStageSendTitle(title);
      delegate.fightStagePlaySound(Sound.ENTITY_PLAYER_LEVELUP);
      delegate.fightStageDidFinish();
      return;
    }
    this.wave = next;
    this.waveProgress = 0;
    this.waveRound = 0;
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

  private Mob summonHoglin(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Hoglin.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      setupEnemy(it, w, round);

      it.setAdult();
      it.setImmuneToZombification(true);
    });
  }

  private Mob summonPiglinBrute(Point3i location, Wave w, int round) {
    return world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), PiglinBrute.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
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
    var action = e.getAction();
    if (action != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    var block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    var item = e.getItem();
    if (item != null && item.getType() == Material.RED_BED && ItemTag.HasByte(item, itemTag) && ItemTag.HasByte(item, Stage.FIGHT.tag)) {
      e.setCancelled(true);
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
