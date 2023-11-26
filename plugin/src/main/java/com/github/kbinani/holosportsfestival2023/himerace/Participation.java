package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.TeamColor;

import javax.annotation.Nonnull;

public class Participation {
  public final @Nonnull TeamColor color;
  public final @Nonnull Role role;
  public final @Nonnull Team team;

  Participation(@Nonnull TeamColor color, @Nonnull Role role, @Nonnull Team team) {
    this.color = color;
    this.role = role;
    this.team = team;
  }
}
