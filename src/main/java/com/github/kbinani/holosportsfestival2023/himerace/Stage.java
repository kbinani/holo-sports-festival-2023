package com.github.kbinani.holosportsfestival2023.himerace;

import org.bukkit.entity.Player;

interface Stage {
  void stageReset();
  void stageOnPlayerMove(Player player, Participation participation, Team team);
}
