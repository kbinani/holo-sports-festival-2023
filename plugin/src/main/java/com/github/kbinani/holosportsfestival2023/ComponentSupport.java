package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ComponentSupport {
  private ComponentSupport() {
  }

  public static Component Text(String s, TextColor color) {
    return Component.text(s).color(color);
  }

  public static Component Text(String s) {
    return Component.text(s).color(NamedTextColor.WHITE);
  }
}
