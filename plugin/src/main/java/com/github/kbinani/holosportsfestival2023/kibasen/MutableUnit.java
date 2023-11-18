package com.github.kbinani.holosportsfestival2023.kibasen;

import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class MutableUnit {
  final @Nonnull Player attacker;
  @Nullable Player vehicle;
  boolean isLeader = false;

  MutableUnit(@Nonnull Player attacker) {
    this.attacker = attacker;
  }
}
