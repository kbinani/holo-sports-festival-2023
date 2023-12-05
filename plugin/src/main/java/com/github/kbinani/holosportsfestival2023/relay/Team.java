package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.TeamColor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

class Team {
  private final @Nonnull TeamColor color;
  private final @Nonnull Player[] order = new Player[9];
  private final @Nonnull Set<Player> players = new HashSet<>();

  Team(@Nonnull TeamColor color) {
    this.color = color;
  }

  void dispose() {
    Arrays.fill(order, null);
    players.clear();
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
  }

  void remove(@Nonnull Player player) {
    for (var i = 0; i < order.length; i++) {
      if (order[i] == player) {
        order[i] = null;
      }
    }
    players.remove(player);
  }

  List<Player> players() {
    return new ArrayList<>(players);
  }

  boolean contains(Player player) {
    return getCurrentOrder(player) != null;
  }

  @Nullable Integer getCurrentOrder(Player player) {
    for (var i = 0; i < order.length; i++) {
      if (order[i] == player) {
        return i;
      }
    }
    return null;
  }

  @Nullable Player getAssignedPlayer(int order) {
    if (order < 0 || this.order.length <= order) {
      return null;
    }
    return this.order[order];
  }
}
