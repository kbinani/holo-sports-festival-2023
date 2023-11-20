package com.github.kbinani.holosportsfestival2023.kibasen;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

class Unit {
  final @Nonnull Player attacker;
  final @Nonnull Player vehicle;
  final @Nonnull ArmorStand healthDisplay;
  final boolean isLeader;

  Unit(@Nonnull Player attacker, @Nonnull Player vehicle, @Nonnull ArmorStand healthDisplay, boolean isLeader) {
    this.vehicle = vehicle;
    this.attacker = attacker;
    this.healthDisplay = healthDisplay;
    this.isLeader = isLeader;
  }
}
