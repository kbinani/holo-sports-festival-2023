package com.github.kbinani.holosportsfestival2023.himerace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;

public enum Role {
  PRINCESS("姫", LIGHT_PURPLE),
  KNIGHT("騎士", GOLD);

  final String text;
  final TextColor color;

  Role(String text, TextColor color) {
    this.text = text;
    this.color = color;
  }

  Component component() {
    return Component.text(text).color(color);
  }
}
