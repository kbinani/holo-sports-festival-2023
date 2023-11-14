package com.github.kbinani.holosportsfestival2023.kibasen;

import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

class Unit {
  final @Nonnull Player attacker;
  final @Nonnull Player vehicle;

  Unit(@Nonnull Player attacker, @Nonnull Player vehicle) {
    this.vehicle = vehicle;
    this.attacker = attacker;
  }
}
