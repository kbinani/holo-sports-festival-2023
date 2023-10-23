package com.github.kbinani.holosportsfestival2023.himerace;

import org.bukkit.event.player.PlayerMoveEvent;

interface Stage {
  void stageReset();
  void debugOnPlayerMove(PlayerMoveEvent e);
}
