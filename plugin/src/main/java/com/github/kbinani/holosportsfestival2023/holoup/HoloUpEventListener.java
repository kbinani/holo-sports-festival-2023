package com.github.kbinani.holosportsfestival2023.holoup;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;

public class HoloUpEventListener implements MiniGame {
  // 位置をずらしたい場合はここでずらす
  private static final Point3i offset = new Point3i(0, 0, 0);
  private final World world;
  private static final Component title = Component.text("[Holoup]").color(Colors.aqua);

  public HoloUpEventListener(World world) {
    this.world = world;
  }

  @Override
  public void miniGameReset() {
    reset();
  }

  private void reset() {
    Editor.StandingSign(
      world,
      pos(-42, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.RED.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      pos(-41, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.WHITE.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      pos(-40, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.YELLOW.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      pos(-39, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      Component.text("観戦者").color(Colors.purple),
      Component.text("Spectator").color(Colors.purple),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );

    Editor.StandingSign(
      world,
      pos(-37, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームスタート").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      pos(-36, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームを中断する").color(Colors.red)
    );
    Editor.StandingSign(
      world,
      pos(-35, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("エントリー リスト").color(Colors.lime)
    );
  }

  private static Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
