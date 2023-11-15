package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.TeamColor;

class Participation {
  final TeamColor color;
  final MutableUnit unit;
  final boolean isAttacker;
  final boolean isVehicle;

  Participation(TeamColor color, MutableUnit unit, boolean isAttacker) {
    this.color = color;
    this.unit = unit;
    this.isAttacker = isAttacker;
    this.isVehicle = !isAttacker;
  }
}
