package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.Cloakroom;
import com.github.kbinani.holosportsfestival2023.EntityTracking;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.kbinani.holosportsfestival2023.relay.RelayEventListener.ClearItem;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

class Team {
  private final @Nonnull TeamColor color;
  private final @Nonnull ArrayList<EntityTracking<Player>> order = new ArrayList<>(9);
  private final @Nonnull Set<EntityTracking<Player>> players = new HashSet<>();
  int currentRunningOrder = 0;
  private final @Nonnull org.bukkit.scoreboard.Team scoreboardTeam;

  Team(@Nonnull TeamColor color, @Nonnull org.bukkit.scoreboard.Team team) {
    this.color = color;
    this.scoreboardTeam = team;
    for (var i = 0; i < 9; i++) {
      order.add(null);
    }
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
    order.replaceAll(it -> null);
    players.forEach(playerTracking -> {
      var player = playerTracking.get();
      Cloakroom.shared.restore(player);
      ClearItem(player);
    });
    players.clear();
    scoreboardTeam.unregister();
  }

  int getOrderLength() {
    int count = 0;
    for (var playerTracking : order) {
      if (playerTracking == null) {
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
    if (order < 0 || this.order.size() <= order) {
      return false;
    }
    var tracking = players.stream().filter(it -> it.get() == player).findFirst().orElse(null);
    if (tracking == null) {
      return false;
    }
    if (this.order.get(order) == null) {
      this.order.set(order, tracking);
      return true;
    } else {
      return false;
    }
  }

  void unassign(Player player) {
    for (var i = 0; i < order.size(); i++) {
      var tracking = order.get(i);
      if (tracking != null && tracking.get() == player) {
        order.set(i, null);
        break;
      }
    }
  }

  void add(@Nonnull Player player) {
    players.add(new EntityTracking<>(player));
    scoreboardTeam.addPlayer(player);
  }

  void remove(@Nonnull Player player) {
    for (var i = 0; i < order.size(); i++) {
      var tracking = order.get(i);
      if (tracking != null && tracking.get() == player) {
        order.set(i, null);
      }
    }
    players.removeIf(it -> it.get() == player);
    scoreboardTeam.removePlayer(player);
  }

  List<EntityTracking<Player>> players() {
    return new ArrayList<>(players);
  }

  boolean contains(Player player) {
    return players.stream().anyMatch(it -> it.get() == player);
  }

  @Nullable
  Integer getCurrentOrder(Player player) {
    for (var i = 0; i < order.size(); i++) {
      var tracking = order.get(i);
      if (tracking != null && tracking.get() == player) {
        return i;
      }
    }
    return null;
  }

  @Nullable
  Player getAssignedPlayer(int order) {
    if (order < 0 || this.order.size() <= order) {
      return null;
    }
    var tracking = this.order.get(order);
    if (tracking == null) {
      return null;
    } else {
      return tracking.get();
    }
  }
}
