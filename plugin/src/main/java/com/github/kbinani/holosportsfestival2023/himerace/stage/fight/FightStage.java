package  com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.ItemTag;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.Stage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.UUID;
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

    void fightStageDidFinish();
  }

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
  }

  @Override
  public void onEntityDeath(EntityDeathEvent e) {
    if (finished || !started) {
      return;
    }
    var entity = e.getEntity();
    if (!entity.getScoreboardTags().contains(Stage.FIGHT.tag)) {
      return;
    }
    if (!entity.getScoreboardTags().contains(stageEnemyTag)) {
      return;
    }
    e.setDroppedExp(0);

    updateWaveProgress();
  }

  @Override
  public void onEntitySpawn(EntitySpawnEvent e) {
    if (finished || !started) {
      return;
    }
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

  private void summonEnemy(Wave wave) {
    switch (wave) {
      case Wave1 -> {
        summonSpider(enemyPosLeft, wave);
        summonSpider(enemyPosRight, wave);
        summonZombie(enemyPosMiddleLeft, wave);
        summonZombie(enemyPosMiddleRight, wave);
        summonSkeleton(enemyPosCenterLow, wave);
      }
      case Wave2 -> {
        summonBlaze(enemyPosLeft, wave);
        summonHoglin(enemyPosMiddleLeft, wave);
        summonPiglinBrute(enemyPosCenterLow, wave);
        summonHoglin(enemyPosMiddleRight, wave);
        summonBlaze(enemyPosRight, wave);
      }
      case Wave3 -> {
        summonPillager(enemyPosLeft, wave);
        summonVindicator(enemyPosMiddleLeft, wave);
        summonIllusioner(enemyPosCenterHigh, wave);
        summonVindicator(enemyPosMiddleRight, wave);
        summonPillager(enemyPosRight, wave);
      }
    }
  }

  private void updateWaveProgress() {
    var remaining = world.getNearbyEntities(bounds).stream().filter(it -> !it.isDead() && it.getScoreboardTags().contains(this.wave.tag)).count();
    waveProgress = 5 - (int) remaining;
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
    delegate.fightStageRequestsTeleport(safeArea.toLocation(world).add(0.5, 0, 0.5), null);
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

  private void setEnableFence(boolean enable) {
    var material = enable ? "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]" : "air";
    Editor.Fill(world, pos(-98, 80, 56), pos(-90, 80, 56), material);
  }

  private void summonZombie(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Zombie.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

      it.setAdult();
      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
      equipment.setItemInMainHand(new ItemStack(Material.IRON_AXE));
    });
  }

  private void summonSpider(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Spider.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

      for (var passenger : it.getPassengers()) {
        passenger.remove();
      }
    });
  }

  private void summonSkeleton(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Skeleton.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

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

  private void summonBlaze(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Blaze.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);
    });
  }

  private void summonHoglin(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Hoglin.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

      it.setAdult();
      it.setImmuneToZombification(true);
    });
  }

  private void summonPiglinBrute(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), PiglinBrute.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

      it.setImmuneToZombification(true);
      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setItemInMainHand(new ItemStack(Material.GOLDEN_AXE));
    });
  }

  private void summonPillager(Point3i location, Wave w) {
    world.spawn(location.toLocation(world).add(0.5, 0, 0.5), Pillager.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setItemInMainHand(new ItemStack(Material.CROSSBOW));
    });
  }

  private void summonVindicator(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Vindicator.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

      var equipment = it.getEquipment();
      DisableDrop(equipment);
      equipment.clear();
      equipment.setItemInMainHand(new ItemStack(Material.IRON_AXE));
    });
  }

  private void summonIllusioner(Point3i location, Wave w) {
    world.spawn(location.toLocation(world, 0, 180).add(0.5, 0, 0.5), Illusioner.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      it.addScoreboardTag(itemTag);
      it.addScoreboardTag(Stage.FIGHT.tag);
      it.addScoreboardTag(w.tag);
      it.addScoreboardTag(stageEnemyTag);
      //NOTE: clearLootTable が効いていない
      it.clearLootTable();
      it.setPersistent(true);
      it.setCanPickupItems(false);

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
    summonEnemy(wave);
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
