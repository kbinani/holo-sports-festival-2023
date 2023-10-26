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

class Level implements BlockHeadStage.Delegate {
  private final World world;
  private final JavaPlugin owner;
  private final Point3i origin;
  private final TeamColor color;
  private final Stage[] stages;
  private final BlockHeadStage blockHeadStage;

  /**
   * 入口の門向かって右下の reinforced_deepslate ブロックの座標を原点として初期化する
   *
   * @param origin
   */
  Level(World world, JavaPlugin owner, TeamColor color, Point3i origin) {
    this.world = world;
    this.owner = owner;
    this.color = color;
    this.origin = origin;
    this.blockHeadStage = new BlockHeadStage(world, owner, origin);
    this.blockHeadStage.delegate = this;
    this.stages = new Stage[]{
      this.blockHeadStage
    };
  }

  void onPlayerMove(PlayerMoveEvent e, Participation participation, Team team) {
    for (var stage : stages) {
      stage.stageOnPlayerMove(e, participation, team);
    }
  }

  void onPlayerInteract(PlayerInteractEvent e, Participation participation, Team team) {
    for (var stage : stages) {
      stage.stageOnPlayerInteract(e, participation, team);
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

    for (var stage : this.stages) {
      stage.stageReset();
    }
  }

  void openGateBlockHead() {
    this.blockHeadStage.stageOpenGate();
  }

  private Point3i pos(int x, int y, int z) {
    // [-100, 80, -61] は赤チーム用 Level の origin
    return new Point3i(x + 100 + origin.x, y - 80 + origin.y, z + 61 + origin.z);
  }

  @Override
  public void blockHeadStageDidFinish() {
    //TODO:
    Stage.OpenGate(owner, world, pos(-96, 80, -18));
  }
}
