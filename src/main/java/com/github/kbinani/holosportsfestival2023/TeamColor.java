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

  public static final TeamColor RED = new TeamColor("赤チーム", Colors.red);
  public static final TeamColor WHITE = new TeamColor("白チーム", Colors.white);
  public static final TeamColor YELLOW = new TeamColor("黄チーム", Colors.yellow);
}
