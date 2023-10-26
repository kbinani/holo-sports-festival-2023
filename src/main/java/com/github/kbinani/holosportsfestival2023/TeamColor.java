package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public enum TeamColor {
  RED("RED", Colors.red),
  WHITE("WHITE", Colors.white),
  YELLOW("YELLOW", Colors.yellow);

  public final String japanese;
  public final TextColor sign;

  TeamColor(String japanese, TextColor sign) {
    this.japanese = japanese;
    this.sign = sign;
  }

  public Component component() {
    return Component.text(japanese).color(sign);
  }

  public static final TeamColor[] all = new TeamColor[]{RED, WHITE, YELLOW};
}
