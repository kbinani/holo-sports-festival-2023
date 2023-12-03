package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.MiniGame;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class RelayEventListener implements MiniGame {
  public static final Component title = text("[Relay]", AQUA);
  static final Component prefix = title.append(text(" ", WHITE));

  @Override
  public void miniGameReset() {

  }

  @Override
  public void miniGameClearItem(Player player) {

  }
}
