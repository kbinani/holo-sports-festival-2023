package com.github.kbinani.holosportsfestival2023;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public interface MiniGame extends Listener {
  void miniGameReset();
  void miniGameClearItem(Player player);
}
