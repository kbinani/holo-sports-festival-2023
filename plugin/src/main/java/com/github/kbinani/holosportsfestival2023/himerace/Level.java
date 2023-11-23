package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

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
    void levelDidFinish(TeamColor color);
  }

  private Delegate delegate;

  /**
   * 入口の門向かって右下の reinforced_deepslate ブロックの座標を原点として初期化する
   *
   * @param origin
   */
  Level(World world, JavaPlugin owner, TeamColor color, Point3i origin, int mapId, Delegate delegate) {
    this.world = world;
    this.owner = owner;
    this.color = color;
    this.origin = origin;
    this.delegate = delegate;
    this.carryStage = new CarryStage(world, owner, origin, this);
    this.buildStage = new BuildStage(world, owner, pos(-100, 80, -18), this);
    this.cookStage = new CookStage(world, owner, pos(-100, 80, 1), this);
    this.solveStage = new SolveStage(world, owner, pos(-100, 80, 22), color.quizConcealer, mapId, this);
    this.fightStage = new FightStage(world, owner, pos(-100, 80, 49), this);
    this.goalStage = new GoalStage(world, owner, pos(-100, 80, 84), this);
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
    return active.progressOffset + active.progressWeight * stage.getProgress();
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

  void reset() {
    Editor.StandingSign(
      world,
      pos(-93, 80, -65),
      Material.OAK_SIGN,
      8,
      HimeraceEventListener.title,
      color.component(),
      Role.PRINCESS.component(),
      Component.text("右クリでエントリー！").color(Colors.aqua));

    Editor.StandingSign(
      world,
      pos(-95, 80, -65),
      Material.OAK_SIGN,
      8,
      HimeraceEventListener.title,
      color.component(),
      Role.KNIGHT.component(),
      Component.text("右クリでエントリー！").color(Colors.aqua));

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
    this.buildStage.start();
  }

  @Override
  public void buildStageDidFinish() {
    this.active = Stage.COOK;
    this.cookStage.start();
  }

  @Override
  public void cookStageDidFinish() {
    this.active = Stage.SOLVE;
    this.solveStage.start();
  }

  @Override
  public void solveStageDidFinish() {
    this.active = Stage.FIGHT;
    this.fightStage.start();
  }

  @Override
  public void fightStageDidFinish() {
    this.active = Stage.GOAL;
    this.goalStage.start();
  }

  @Override
  public void goalStageDidFinish() {
    if (delegate != null) {
      delegate.levelDidFinish(color);
    }
  }
}
