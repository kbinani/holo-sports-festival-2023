package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public enum TeamColor {
  RED("RED", Colors.red, Material.RED_WOOL),
  WHITE("WHITE", Colors.white, Material.WHITE_WOOL),
  YELLOW("YELLOW", Colors.yellow, Material.YELLOW_WOOL);

  public final String japanese;
  public final TextColor sign;
  public final Material quizConcealer;

  TeamColor(String japanese, TextColor sign, Material quizConcealer) {
    this.japanese = japanese;
    this.sign = sign;
    this.quizConcealer = quizConcealer;
  }

  public Component component() {
    return Component.text(japanese).color(sign);
  }

  public static final TeamColor[] all = new TeamColor[]{RED, WHITE, YELLOW};
}
