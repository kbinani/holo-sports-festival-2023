package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.TeamColor;
import org.jetbrains.annotations.NotNull;

class Participation {
  @NotNull
  final TeamColor color;
  @NotNull
  final Role role;
  @NotNull
  final Team team;

  Participation(@NotNull TeamColor color, @NotNull Role role, @NotNull Team team) {
    this.color = color;
    this.role = role;
    this.team = team;
  }
}
