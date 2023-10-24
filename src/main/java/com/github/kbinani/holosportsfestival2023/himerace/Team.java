package com.github.kbinani.holosportsfestival2023.himerace;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

class Team {
  private @Nullable Player princess;
  private final List<Player> knights = new LinkedList<>();
  private static final int kMaxKnightPlayers = 2;

  void add(Player player, Role role) {
    if (getCurrentRole(player) != null) {
      //TODO: エラーメッセージ
      return;
    }
    switch (role) {
      case PRINCESS -> {
        if (princess == null) {
          princess = player;
        } else {
          //TODO: エラーメッセージ
        }
      }
      case KNIGHT -> {
        if (kMaxKnightPlayers > knights.size()) {
          knights.add(player);
        } else {
          //TODO: エラーメッセージ
        }
      }
    }
  }

  List<Player> getKnights() {
    return new LinkedList<>(knights);
  }

  @Nullable Player getPrincess() {
    return princess;
  }

  @Nullable
  Role getCurrentRole(Player player) {
    if (princess != null && princess.getUniqueId().equals(player.getUniqueId())) {
      return Role.PRINCESS;
    }
    if (knights.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()))) {
      return Role.KNIGHT;
    }
    return null;
  }
}
