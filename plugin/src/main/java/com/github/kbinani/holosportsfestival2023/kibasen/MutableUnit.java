package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.EntityTracking;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class MutableUnit {
  final @Nonnull EntityTracking<Player> attacker;
  @Nullable
  EntityTracking<Player> vehicle;
  boolean isLeader = false;

  MutableUnit(@Nonnull Player attacker) {
    this.attacker = new EntityTracking<>(attacker);
  }
}
