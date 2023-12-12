package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.*;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.build.BuildStage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.carry.CarryStage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.fight.FightStage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.goal.GoalStage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.solve.SolveStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;

class Level implements CarryStage.Delegate, BuildStage.Delegate, CookStage.Delegate, SolveStage.Delegate, FightStage.Delegate, GoalStage.Delegate {
  private final World world;
  private final Point3i origin;
  private final TeamColor color;
  private final Map<Stage, AbstractStage> stages = new HashMap<>();
  private final CarryStage carryStage;
  private final BuildStage buildStage;
  private final CookStage cookStage;
  private final SolveStage solveStage;
  private final FightStage fightStage;
  private final GoalStage goalStage;
  private Stage active = Stage.CARRY;

  interface Delegate {
    void levelSignalActionBarUpdate();

    void levelSendTitle(Title title);

    void levelPlaySound(Sound sound);

    void levelRequestsTeleport(@Nonnull Function<Player, Location> predicate);

    void levelRequestsHealthRecovery();

    void levelRequestsEncouragingKnights(Set<UUID> excludeHealthRecovery);

    void levelRequestsClearGoatHornCooltime();

    @Nullable
    Player levelRequestsVisibleAlivePlayer(Mob enemy);

    void levelDidClearStage(Stage stage);
  }

  final WeakReference<Delegate> delegate = new WeakReference<>(null);

  /**
   * 入口の門向かって右下の reinforced_deepslate ブロックの座標を原点として初期化する
   */
  Level(World world, JavaPlugin owner, TeamColor color, Point3i origin, int mapId) {
    this.world = world;
    this.color = color;
    this.origin = origin;
    this.carryStage = new CarryStage(world, owner, origin, pos(-87, 80, -18), this);
    this.buildStage = new BuildStage(world, owner, pos(-100, 80, -18), pos(-87, 80, 1), this);
    this.cookStage = new CookStage(world, owner, pos(-100, 80, 1), pos(-87, 80, 22), this);
    this.solveStage = new SolveStage(world, owner, pos(-100, 80, 22), pos(-87, 80, 49), color.quizConcealer, mapId, this);
    this.fightStage = new FightStage(world, owner, pos(-100, 80, 49), pos(-87, 80, 84), this);
    this.goalStage = new GoalStage(world, owner, pos(-100, 80, 84), pos(-87, 80, 99), this);
    this.stages.put(Stage.CARRY, this.carryStage);
    this.stages.put(Stage.BUILD, this.buildStage);
    this.stages.put(Stage.COOK, this.cookStage);
    this.stages.put(Stage.SOLVE, this.solveStage);
    this.stages.put(Stage.FIGHT, this.fightStage);
    this.stages.put(Stage.GOAL, this.goalStage);
  }

  Stage getActive() {
    return active;
  }

  float getProgress() {
    var stage = this.stages.get(active);
    return stage.getProgress();
  }

  @Nonnull
  Component getActionBar(Role role) {
    var stage = stages.get(active);
    return stage.getActionBar(role);
  }

  void onPlayerMove(PlayerMoveEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.playerMove(e, participation);
    }
  }

  void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.playerInteract(e, participation);
    }
  }

  void onInventoryClick(InventoryClickEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.inventoryClick(e, participation);
    }
  }

  void onPlayerItemConsume(PlayerItemConsumeEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.playerItemConsume(e, participation);
    }
  }

  void onBlockDropItem(BlockDropItemEvent e) {
    for (var stage : stages.values()) {
      stage.blockDropItem(e);
    }
  }

  void onFurnaceSmelt(FurnaceSmeltEvent e) {
    for (var stage : stages.values()) {
      stage.furnaceSmelt(e);
    }
  }

  void onEntityDeath(EntityDeathEvent e) {
    for (var stage : stages.values()) {
      stage.entityDeath(e);
    }
  }

  void onEntitySpawn(EntitySpawnEvent e) {
    for (var stage : stages.values()) {
      stage.entitySpawn(e);
    }
  }

  void onPlayerDeath(PlayerDeathEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.playerDeath(e, participation);
    }
  }

  void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.entityTargetLivingEntity(e, participation);
    }
  }

  void onPlayerInteractEntity(PlayerInteractEntityEvent e, Role player, Role rightClicked) {
    for (var stage : stages.values()) {
      stage.playerInteractEntity(e, player, rightClicked);
    }
  }

  void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    for (var stage : stages.values()) {
      stage.entityDamageByEntity(e);
    }
  }

  void onEntityDismount(EntityDismountEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.entityDismount(e, participation);
    }
  }

  void onBlockBreak(BlockBreakEvent e, Participation participation) {
    var stage = stages.get(active);
    if (stage == null) {
      e.setCancelled(true);
    } else {
      stage.blockBreak(e, participation);
    }
  }

  void onBlockPlace(BlockPlaceEvent e, Participation participation) {
    var stage = stages.get(active);
    if (stage == null) {
      if (!e.getPlayer().isOp()) {
        e.setCancelled(true);
      }
    } else {
      stage.blockPlace(e, participation);
    }
  }

  void onEntityPlace(EntityPlaceEvent e, Participation participation) {
    var stage = stages.get(active);
    if (stage == null) {
      e.setCancelled(true);
    } else {
      stage.entityPlace(e, participation);
    }
  }

  void onEntityRegainHealth(EntityRegainHealthEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.entityRegainHealth(e, participation);
    }
  }

  void onPlayerItemDamage(PlayerItemDamageEvent e, Participation participation) {
    for (var stage : stages.values()) {
      stage.playerItemDamage(e, participation);
    }
  }

  void tick() {
    for (var stage : stages.values()) {
      stage.tick();
    }
  }

  void reset() {
    Editor.StandingSign(world, pos(-93, 80, -65), Material.OAK_SIGN, 8,
      HimeraceEventListener.title, color.component(), Role.PRINCESS.component(), text("右クリでエントリー！", AQUA)
    );
    Editor.StandingSign(world, pos(-95, 80, -65), Material.OAK_SIGN, 8,
      HimeraceEventListener.title, color.component(), Role.KNIGHT.component(), text("右クリでエントリー！", AQUA)
    );
    for (var stage : this.stages.values()) {
      stage.reset();
    }
    active = Stage.CARRY;
  }

  void start() {
    this.carryStage.start();
  }

  private Point3i pos(int x, int y, int z) {
    // [-100, 80, -61] は赤チーム用 Level の origin
    return new Point3i(x + 100 + origin.x, y - 80 + origin.y, z + 61 + origin.z);
  }

  @Override
  public void carryStageDidFinish() {
    this.active = Stage.BUILD;
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.CARRY);
    });
    this.buildStage.start();
    this.carryStage.closeGate();
  }

  @Override
  public void buildStageDidFinish() {
    this.active = Stage.COOK;
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.BUILD);
    });
    this.cookStage.start();
  }

  @Override
  public void buildStageSignalActionBarUpdate() {
    this.delegate.use(Delegate::levelSignalActionBarUpdate);
  }

  @Override
  public void buildStageSendTitle(Title title) {
    this.delegate.use(d -> {
      d.levelSendTitle(title);
    });
  }

  @Override
  public void buildStagePlaySound(Sound sound) {
    this.delegate.use(d -> {
      d.levelPlaySound(sound);
    });
  }

  @Override
  public void cookStageSignalActionBarUpdate() {
    this.delegate.use(Delegate::levelSignalActionBarUpdate);
  }

  @Override
  public void cookStageDidFinish() {
    this.active = Stage.SOLVE;
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.COOK);
    });
    this.solveStage.start();
  }

  @Override
  public void solveStageSendTitle(Title title) {
    this.delegate.use(d -> {
      d.levelSendTitle(title);
    });
  }

  @Override
  public void solveStagePlaySound(Sound sound) {
    this.delegate.use(d -> {
      d.levelPlaySound(sound);
    });
  }

  @Override
  public void solveStageDidFinish() {
    this.active = Stage.FIGHT;
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.SOLVE);
    });
    this.fightStage.start();
    this.goalStage.closeGate();
  }

  @Override
  public void fightStageSendTitle(Title title) {
    this.delegate.use(d -> {
      d.levelSendTitle(title);
    });
  }

  @Override
  public void fightStagePlaySound(Sound sound) {
    this.delegate.use(d -> {
      d.levelPlaySound(sound);
    });
  }

  @Override
  public void fightStageRequestsTeleport(@Nonnull Function<Player, Location> predicate) {
    this.delegate.use(d -> {
      d.levelRequestsTeleport(predicate);
    });
  }

  @Override
  public void fightStageRequestsHealthRecovery() {
    this.delegate.use(Delegate::levelRequestsHealthRecovery);
  }

  @Override
  public void fightStageRequestsEncouragingKnights(Set<UUID> excludeHealthRecovery) {
    this.delegate.use(it -> {
      it.levelRequestsEncouragingKnights(excludeHealthRecovery);
    });
  }

  @Override
  public void fightStageRequestsClearGoatHornCooltime() {
    this.delegate.use(Delegate::levelRequestsClearGoatHornCooltime);
  }

  @Override
  public @Nullable Player fightStageRequestsVisibleAlivePlayer(Mob enemy) {
    return delegate.use((delegate) -> {
      return delegate.levelRequestsVisibleAlivePlayer(enemy);
    });
  }

  @Override
  public void fightStageDidFinish() {
    this.active = Stage.GOAL;
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.FIGHT);
    });
    this.goalStage.start();
  }

  @Override
  public void goalStageDidFinish() {
    var right = pos(-99, 81, 95);
    var left = pos(-89, 81, 95);
    var c = color.fireworkColor;
    FireworkRocket.Launch(world, left, new Color[]{c}, new Color[]{c}, 20, 1, false, true);
    FireworkRocket.Launch(world, right, new Color[]{c}, new Color[]{c}, 20, 1, false, true);
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.GOAL);
    });
  }
}
