package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

class Level {
  private final World world;
  private final Point3i origin;
  private final TeamColor color;
  private final Stage[] stages;

  /**
   * 入口の門向かって右下の reinforced_deepslate ブロックの座標を原点として初期化する
   * @param origin
   */
  Level(World world, TeamColor color, Point3i origin) {
    this.world = world;
    this.color = color;
    this.origin = origin;
    this.stages = new Stage[]{
        new BlockHeadStage(world, origin)
    };
  }

  void onPlayerMove(Player player, Participation participation, Team team) {
    for (var stage : stages) {
      stage.stageOnPlayerMove(player, participation, team);
    }
  }

  void reset() {
    Editor.StandingSign(
        world,
        pos(-16, -60, -20),
        Material.OAK_SIGN,
        8,
        Component.text("[Himerace]").color(Colors.aqua),
        Component.text(color.japanese).color(color.sign),
        Component.text("姫").color(Colors.magenta),
        Component.text("右クリでエントリー！").color(Colors.aqua));

    Editor.StandingSign(
        world,
        pos(-18, -60, -20),
        Material.OAK_SIGN,
        8,
        Component.text("[Himerace]").color(Colors.aqua),
        Component.text(color.japanese).color(color.sign),
        Component.text("騎士").color(Colors.orange),
        Component.text("右クリでエントリー！").color(Colors.aqua));

    for (var stage : this.stages) {
      stage.stageReset();
    }
  }

  private Point3i pos(int x, int y, int z) {
    // [-23, -60, -16] はステージを仮の座標で再現した時の、赤チーム用 Level の origin
    return new Point3i(x + 23 + origin.x, y + 60 + origin.y, z + 16 + origin.z);
  }
}