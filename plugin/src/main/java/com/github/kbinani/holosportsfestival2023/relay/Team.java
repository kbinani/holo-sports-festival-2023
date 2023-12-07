package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.Cloakroom;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.github.kbinani.holosportsfestival2023.relay.RelayEventListener.ClearItem;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

class Team {
  private final @Nonnull TeamColor color;
  private final @Nonnull Player[] order = new Player[9];
  private final @Nonnull Set<Player> players = new HashSet<>();
  int currentRunningOrder = 0;
  private final @Nonnull org.bukkit.scoreboard.Team scoreboardTeam;

  Team(@Nonnull TeamColor color, @Nonnull org.bukkit.scoreboard.Team team) {
    this.color = color;
    this.scoreboardTeam = team;
  }

  Component getBossBarName() {
    var orderLength = getOrderLength();
    var player = getAssignedPlayer(Math.min(currentRunningOrder, orderLength - 1));
    if (player == null) {
      return Component.empty();
    }
    return color.component()
      .appendSpace()
      .append(text(currentRunningOrder < orderLength - 1 ? String.format("第%d走者: ", currentRunningOrder + 1) : "アンカー: ", WHITE))
      .append(text(player.getName(), GOLD));
  }

  float getBossBarProgress() {
    return currentRunningOrder / (float) getOrderLength();
  }

  void dispose() {
    Arrays.fill(order, null);
    players.forEach(player -> {
      Cloakroom.shared.restore(player);
      ClearItem(player);
    });
    players.clear();
    scoreboardTeam.unregister();
  }

  int getOrderLength() {
    int count = 0;
    for (Player player : order) {
      if (player == null) {
        break;
      } else {
        count++;
      }
    }
    return count;
  }

  int getParticipantsCount() {
    return players.size();
  }

  boolean assign(Player player, int order) {
    if (order < 0 || this.order.length <= order) {
      return false;
    }
    if (!players.contains(player)) {
      return false;
    }
    if (this.order[order] == null) {
      this.order[order] = player;
      players.add(player);
      return true;
    } else {
      return false;
    }
  }

  void unassign(Player player) {
    for (var i = 0; i < order.length; i++) {
      if (order[i] == player) {
        order[i] = null;
        break;
      }
    }
  }

  void add(@Nonnull Player player) {
    players.add(player);
    scoreboardTeam.addPlayer(player);
  }

  void remove(@Nonnull Player player) {
    for (var i = 0; i < order.length; i++) {
      if (order[i] == player) {
        order[i] = null;
      }
    }
    players.remove(player);
    scoreboardTeam.removePlayer(player);
  }

  List<Player> players() {
    return new ArrayList<>(players);
  }

  boolean contains(Player player) {
    return getCurrentOrder(player) != null;
  }

  @Nullable
  Integer getCurrentOrder(Player player) {
    for (var i = 0; i < order.length; i++) {
      if (order[i] == player) {
        return i;
      }
    }
    return null;
  }

  @Nullable
  Player getAssignedPlayer(int order) {
    if (order < 0 || this.order.length <= order) {
      return null;
    }
    return this.order[order];
  }
}
