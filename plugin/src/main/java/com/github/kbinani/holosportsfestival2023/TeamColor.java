package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public enum TeamColor {
  RED("RED", Colors.red, Material.RED_WOOL, BossBar.Color.RED),
  WHITE("WHITE", Colors.white, Material.WHITE_WOOL, BossBar.Color.WHITE),
  YELLOW("YELLOW", Colors.yellow, Material.YELLOW_WOOL, BossBar.Color.YELLOW);

  public final String japanese;
  public final TextColor sign;
  public final Material quizConcealer;
  public final BossBar.Color barColor;

  TeamColor(String japanese, TextColor sign, Material quizConcealer, BossBar.Color barColor) {
    this.japanese = japanese;
    this.sign = sign;
    this.quizConcealer = quizConcealer;
    this.barColor = barColor;
  }

  public Component component() {
    return Component.text(japanese).color(sign);
  }

  public static final TeamColor[] all = new TeamColor[]{RED, WHITE, YELLOW};
}
