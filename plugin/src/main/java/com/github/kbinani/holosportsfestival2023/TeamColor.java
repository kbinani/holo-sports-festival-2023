package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public enum TeamColor {
  RED("RED", Colors.red, Material.RED_WOOL, BossBar.Color.RED, NamedTextColor.RED, Material.RED_WOOL),
  WHITE("WHITE", Colors.white, Material.WHITE_WOOL, BossBar.Color.WHITE, NamedTextColor.WHITE, Material.WHITE_WOOL),
  YELLOW("YELLOW", Colors.yellow, Material.YELLOW_WOOL, BossBar.Color.YELLOW, NamedTextColor.YELLOW, Material.YELLOW_WOOL);

  public final String japanese;
  public final TextColor sign;
  public final Material quizConcealer;
  public final BossBar.Color barColor;
  public final NamedTextColor namedTextColor;
  public final Material wool;

  TeamColor(String japanese, TextColor sign, Material quizConcealer, BossBar.Color barColor, NamedTextColor namedTextColor, Material wool) {
    this.japanese = japanese;
    this.sign = sign;
    this.quizConcealer = quizConcealer;
    this.barColor = barColor;
    this.namedTextColor = namedTextColor;
    this.wool = wool;
  }

  public Component component() {
    return Component.text(japanese).color(sign);
  }

  public static final TeamColor[] all = new TeamColor[]{RED, WHITE, YELLOW};
}
