package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class TeamColor {
  public final String japanese;
  public final TextColor sign;

  private TeamColor(String japanese, TextColor sign) {
    this.japanese = japanese;
    this.sign = sign;
  }

  public Component component() {
    return Component.text(japanese).color(sign);
  }

  public static final TeamColor RED = new TeamColor("RED", Colors.red);
  public static final TeamColor WHITE = new TeamColor("WHITE", Colors.white);
  public static final TeamColor YELLOW = new TeamColor("YELLOW", Colors.yellow);

  public static final TeamColor[] all = new TeamColor[]{RED, WHITE, YELLOW};
}
