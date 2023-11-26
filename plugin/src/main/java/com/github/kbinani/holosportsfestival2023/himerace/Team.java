package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;
import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.CreateRecipeBook0;
import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.CreateRecipeBook1;
import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.ClearItems;
import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.itemTag;

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
  public void levelDidClearStage(Stage stage) {
    if (princess != null) {
      ClearItems(princess, stage.tag);
    }
    for (var knight : knights) {
      ClearItems(knight, stage.tag);
    }
    switch (stage) {
      case CARRY -> {
        if (princess != null) {
          var book = ItemBuilder.For(Material.BOOK)
            .customByteTag(itemTag)
            .customByteTag(Stage.BUILD.tag)
            .displayName(Text("回答する！(右クリックで開く) / Answer Book (Right click to open)", NamedTextColor.AQUA))
            .build();
          var inventory = princess.getInventory();
          inventory.setItem(0, book);
        }
      }
      case BUILD -> {
        if (princess != null) {
          princess.chat("お腹が空いてきちゃった・・・(I'm so hungry...)");
          var inventory = princess.getInventory();
          inventory.setItem(0, CreateRecipeBook0());
          inventory.setItem(1, CreateRecipeBook1());
        }
        for (var knight : knights) {
          var emerald = ItemBuilder.For(Material.EMERALD)
            .amount(20)
            .customByteTag(Stage.COOK.tag)
            .build();
          knight.getInventory().setItem(0, emerald);
        }
      }
      case GOAL -> {
        if (delegate != null) {
          delegate.teamDidFinish(color);
        }
      }
    }
  }

  @Override
  public void levelSignalActionBarUpdate() {
    updateActionBar();
  }

  @Override
  public void levelSendTitle(Title title) {
    if (princess != null) {
      princess.showTitle(title);
    }
    for (var knight : knights) {
      knight.showTitle(title);
    }
  }

  @Override
  public void levelPlaySound(Sound sound) {
    if (princess != null) {
      princess.playSound(princess.getLocation(), sound, 1, 1);
    }
    for (var knight : knights) {
      knight.playSound(knight.getLocation(), sound, 1, 1);
    }
  }

  void dispose() {
    if (princess != null) {
      ClearItems(princess, itemTag);
    }
    for (var knight : knights) {
      ClearItems(knight, itemTag);
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
    return color.component().append(Text(String.format(" %s", stage.description), NamedTextColor.WHITE));
  }

  float getProgress() {
    return level.getProgress();
  }

  void tick() {
    level.tick();
    updateActionBar();
  }

  private void updateActionBar() {
    if (princess != null) {
      princess.sendActionBar(level.getActionBar(Role.PRINCESS));
    }
    for (var knight : knights) {
      knight.sendActionBar(level.getActionBar(Role.KNIGHT));
    }
  }
}
