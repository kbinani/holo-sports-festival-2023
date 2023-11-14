package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class KibasenEventListener implements MiniGame {
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final Component title = Component.text("[Kibasen]").color(Colors.aqua);
  private static final Component prefix = title.append(Component.text(" ").color(Colors.white));
  private static final Point3i joinRedSign = pos(-30, 80, 50);
  private static final Point3i joinWhiteSign = pos(-30, 80, 51);
  private static final Point3i joinYellowSign = pos(-30, 80, 52);
  private static final Point3i startSign = pos(-30, 80, 54);
  private static final Point3i abortSign = pos(-30, 80, 55);
  private static final Point3i entryListSign = pos(-30, 80, 56);

  private final World world;
  private final JavaPlugin owner;

  public KibasenEventListener(World world, JavaPlugin owner) {
    this.owner = owner;
    this.world = world;
  }

  @Override
  public void miniGameReset() {
    reset();
  }

  @Override
  public void miniGameClearItem(Player player) {

  }

  private void reset() {
    Editor.StandingSign(
      world,
      joinRedSign,
      Material.OAK_SIGN,
      4,
      title,
      TeamColor.RED.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );
    Editor.StandingSign(
      world,
      joinWhiteSign,
      Material.OAK_SIGN,
      4,
      title,
      TeamColor.WHITE.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );
    Editor.StandingSign(
      world,
      joinYellowSign,
      Material.OAK_SIGN,
      4,
      title,
      TeamColor.YELLOW.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.lime)
    );

    Editor.StandingSign(
      world,
      startSign,
      Material.OAK_SIGN,
      4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームスタート").color(Colors.lime)
    );
    Editor.StandingSign(
      world,
      abortSign,
      Material.OAK_SIGN,
      4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームを中断する").color(Colors.red)
    );
    Editor.StandingSign(
      world,
      entryListSign,
      Material.OAK_SIGN,
      4,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("エントリーリスト").color(Colors.aqua)
    );
  }

  private static Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
