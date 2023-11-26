package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import com.github.kbinani.holosportsfestival2023.WeakReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;

class Level implements CarryStage.Delegate, BuildStage.Delegate, CookStage.Delegate, SolveStage.Delegate, FightStage.Delegate, GoalStage.Delegate {
  private final World world;
  private final JavaPlugin owner;
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
    void levelDidClearStage(Stage stage);
  }

  final WeakReference<Delegate> delegate = new WeakReference<>(null);

  /**
   * 入口の門向かって右下の reinforced_deepslate ブロックの座標を原点として初期化する
   */
  Level(World world, JavaPlugin owner, TeamColor color, Point3i origin, int mapId) {
    this.world = world;
    this.owner = owner;
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

  @Nonnull Component getActionBar(Role role) {
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
      stage.onInventoryClick(e, participation);
    }
  }

  void tick() {
    for (var stage : stages.values()) {
      stage.tick();
    }
  }

  void reset() {
    Editor.StandingSign(
      world,
      pos(-93, 80, -65),
      Material.OAK_SIGN,
      8,
      HimeraceEventListener.title,
      color.component(),
      Role.PRINCESS.component(),
      Text("右クリでエントリー！", NamedTextColor.AQUA));

    Editor.StandingSign(
      world,
      pos(-95, 80, -65),
      Material.OAK_SIGN,
      8,
      HimeraceEventListener.title,
      color.component(),
      Role.KNIGHT.component(),
      Text("右クリでエントリー！", NamedTextColor.AQUA));

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
  public void cookStageDidFinish() {
    this.active = Stage.SOLVE;
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.COOK);
    });
    this.solveStage.start();
  }

  @Override
  public void solveStageDidFinish() {
    this.active = Stage.FIGHT;
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.SOLVE);
    });
    this.fightStage.start();
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
    this.delegate.use((delegate) -> {
      delegate.levelDidClearStage(Stage.GOAL);
    });
  }
}
