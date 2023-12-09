package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;

public enum TeamColor {
  RED("RED", NamedTextColor.RED, Material.RED_WOOL, BossBar.Color.RED, Material.RED_WOOL, Color.RED, NamedTextColor.RED),
  WHITE("WHITE", NamedTextColor.WHITE, Material.WHITE_WOOL, BossBar.Color.WHITE, Material.WHITE_WOOL, Color.WHITE, NamedTextColor.GRAY),
  YELLOW("YELLOW", NamedTextColor.YELLOW, Material.YELLOW_WOOL, BossBar.Color.YELLOW, Material.YELLOW_WOOL, Color.YELLOW, NamedTextColor.YELLOW);

  public final String text;
  public final NamedTextColor textColor;
  public final Material quizConcealer;
  public final BossBar.Color barColor;
  public final Material wool;
  public final Color fireworkColor;
  public final NamedTextColor teamColor;

  TeamColor(String text, NamedTextColor textColor, Material quizConcealer, BossBar.Color barColor, Material wool, Color fireworkColor, NamedTextColor teamColor) {
    this.text = text;
    this.textColor = textColor;
    this.quizConcealer = quizConcealer;
    this.barColor = barColor;
    this.wool = wool;
    this.fireworkColor = fireworkColor;
    this.teamColor = teamColor;
  }

  public Component component() {
    return Component.text(text).color(textColor);
  }

  public static final TeamColor[] all = new TeamColor[]{RED, WHITE, YELLOW};
}
