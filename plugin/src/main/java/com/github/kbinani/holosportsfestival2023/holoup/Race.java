package com.github.kbinani.holosportsfestival2023.holoup;

import com.github.kbinani.holosportsfestival2023.TeamColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

class Race {
  private final Map<TeamColor, Player> participants;

  Race(Map<TeamColor, Player> registrants) {
    this.participants = new HashMap<>(registrants);
    registrants.clear();
  }
}
