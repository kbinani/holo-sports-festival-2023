package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

class Team implements Level.Delegate {
  interface Delegate {
    void teamDidFinish(TeamColor color);
  }

  private final TeamColor color;
  private @Nullable Player princess;
  private final List<Player> knights = new LinkedList<>();
  private static final int kMaxKnightPlayers = 2;
  private final Level level;
  @Nullable Delegate delegate;

  Team(TeamColor color, Level level) {
    this.color = color;
    level.delegate.set(this);
    this.level = level;
  }

  @Override
  public void levelDidFinish() {
    if (delegate != null) {
      delegate.teamDidFinish(color);
    }
  }

  boolean add(Player player, Role role) {
    if (getCurrentRole(player) != null) {
      //TODO: エラーメッセージ
      return false;
    }
    return switch (role) {
      case PRINCESS -> {
        if (princess == null) {
          princess = player;
          yield true;
        } else {
          //TODO: エラーメッセージ
          yield false;
        }
      }
      case KNIGHT -> {
        if (kMaxKnightPlayers > knights.size()) {
          knights.add(player);
          yield true;
        } else {
          //TODO: エラーメッセージ
          yield false;
        }
      }
    };
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

  int size() {
    int i = knights.size();
    if (princess != null) {
      i++;
    }
    return i;
  }

  Component getBossBarName() {
    var stage = level.getActive();
    return color.component().append(Component.text(String.format(" %s", stage.description)).color(Colors.white));
  }

  float getProgress() {
    return level.getProgress();
  }

  void tick() {
    if (princess != null) {
      princess.sendActionBar(level.getActionBar(Role.PRINCESS));
    }
    for (var knight : knights) {
      knight.sendActionBar(level.getActionBar(Role.KNIGHT));
    }
  }
}
