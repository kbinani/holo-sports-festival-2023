package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

enum Role {
  PRINCESS("姫", Colors.magenta),
  KNIGHT("騎士", Colors.orange);

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
