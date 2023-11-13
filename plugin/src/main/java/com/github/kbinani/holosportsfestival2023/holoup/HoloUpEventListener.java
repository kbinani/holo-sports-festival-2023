package com.github.kbinani.holosportsfestival2023.holoup;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class HoloUpEventListener implements MiniGame, Race.Delegate {
  // 位置をずらしたい場合はここでずらす
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final Component title = Component.text("[Holoup]").color(Colors.aqua);
  private static final Component prefix = title.append(Component.text(" ").color(Colors.white));
  private static final Point3i joinSignRed = pos(-42, 100, -29);
  private static final Point3i joinSignWhite = pos(-41, 100, -29);
  private static final Point3i joinSignYellow = pos(-40, 100, -29);
  private static final Point3i abortSign = pos(-36, 100, -29);
  private static final Point3i startSign = pos(-37, 100, -29);
  private static final BoundingBox announceBounds = new BoundingBox(x(-59), y(99), z(-63), x(12), 500, z(-19));
  private static final String itemTag = "holo_sports_festival_holoup";

  private final World world;
  private final JavaPlugin owner;
  private Status status = Status.IDLE;
  private final Map<TeamColor, Player> registrants = new HashMap<>();
  private @Nullable Cancellable countdownTask;
  private @Nullable Race race;

  public HoloUpEventListener(World world, JavaPlugin owner) {
    this.world = world;
    this.owner = owner;
  }

  @Override
  public void miniGameReset() {
    reset();
  }

  @Override
  public void miniGameClearItem(Player player) {
    clearItems(player);
  }

  @Override
  public void raceDidFinish() {
    reset();
  }

  @Override
  public void raceDidDetectGoal(TeamColor color, Player player) {
    broadcast(prefix
      .append(Component.text(player.getName()).color(color.sign))
      .appendSpace()
      .append(Component.text("がゴールしました！").color(Colors.orange)));
  }

  @Override
  public void raceDidDetectCheckpoint(TeamColor color, Player player, int index) {
    broadcast(prefix
      .append(Component.text(player.getName()).color(color.sign))
      .append(Component.text(String.format(" が%d個目のチェックポイントに到達しました！", index)).color(Colors.white)));
  }

  private void reset() {
    Editor.StandingSign(
      world,
      joinSignRed,
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.RED.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      joinSignWhite,
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.WHITE.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      joinSignYellow,
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.YELLOW.component(),
      Component.empty(),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      pos(-39, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      Component.text("観戦者").color(Colors.purple),
      Component.text("Spectator").color(Colors.purple),
      Component.text("右クリでエントリー！").color(Colors.aqua)
    );

    Editor.StandingSign(
      world,
      startSign,
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームスタート").color(Colors.aqua)
    );
    Editor.StandingSign(
      world,
      abortSign,
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("ゲームを中断する").color(Colors.red)
    );
    Editor.StandingSign(
      world,
      pos(-35, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Component.text("エントリー リスト").color(Colors.lime)
    );

    registrants.clear();
    status = Status.IDLE;
    world.getPlayers().forEach(this::clearItems);
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    if (race != null) {
      race.cleanup();
    }
    race = null;
  }

  private void abort() {
    status = Status.IDLE;
    world.getPlayers().forEach(this::clearItems);
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    if (race != null) {
      race.cancel();
    }
    race = null;
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    switch (status) {
      case IDLE -> {
        Block block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
          return;
        }
        Point3i location = new Point3i(block.getLocation());
        if (location.equals(joinSignRed)) {
          onClickJoin(player, TeamColor.RED);
        } else if (location.equals(joinSignWhite)) {
          onClickJoin(player, TeamColor.WHITE);
        } else if (location.equals(joinSignYellow)) {
          onClickJoin(player, TeamColor.YELLOW);
        } else if (location.equals(startSign)) {
          if (registrants.isEmpty()) {
            player.sendMessage(prefix
              .append(Component.text("参加者がいません").color(Colors.red)));
          } else {
            startCountdown();
          }
        }
      }
      case COUNTDOWN, ACTIVE -> {
        if (race != null) {
          race.onPlayerInteract(e);
        }
        Block block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
          return;
        }
        Point3i location = new Point3i(block.getLocation());
        if (location.equals(abortSign)) {
          abort();
          broadcast(prefix
            .append(Component.text("ゲームを中断しました").color(Colors.red)));
        }
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerMove(PlayerMoveEvent e) {
    if (race == null) {
      return;
    }
    race.onPlayerMove(e.getPlayer());
  }

  private void startCountdown() {
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    status = Status.COUNTDOWN;
    var titlePrefix = Component.text("スタートまで").color(Colors.aqua);
    var subtitle = Component.text("上を目指せ!holoUp!").color(Colors.lime);
    countdownTask = new Countdown(
      owner,
      world,
      announceBounds,
      titlePrefix,
      Colors.aqua,
      subtitle,
      10,
      this::start
    );
  }

  private void start() {
    boolean ok = true;
    for (var player : registrants.values()) {
      if (!giveItems(player)) {
        ok = false;
      }
    }
    if (!ok) {
      for (var player : registrants.values()) {
        clearItems(player);
      }
      broadcast(prefix
        .append(Component.text("インベントリのスロットが空いておらず競技用アイテムが渡せない参加者がいたため中断しました。").color(Colors.red)));
      status = Status.IDLE;
      return;
    }
    if (this.race != null) {
      this.race.cancel();
      this.race = null;
    }
    this.race = new Race(owner, world, announceBounds, registrants, this);
    status = Status.ACTIVE;
    broadcast(prefix
      .append(Component.text("ゲームがスタートしました。").color(Colors.white)));
  }

  private void onClickJoin(Player player, TeamColor color) {
    var current = registrants.get(color);
    if (current == null) {
      if (registrants.containsValue(player)) {
        player.sendMessage(prefix
          .append(Component.text("既に他のチームにエントリーしています。").color(Colors.red))
        );
      } else {
        registrants.put(color, player);
        broadcast(prefix
          .append(Component.text(player.getName()).color(color.sign))
          .append(Component.text("が").color(Colors.white))
          .append(color.component())
          .append(Component.text("にエントリーしました。").color(Colors.white))
        );
      }
    } else if (current == player) {
      registrants.remove(color);
      broadcast(prefix
        .append(Component.text(player.getName() + "が").color(Colors.white))
        .append(color.component())
        .append(Component.text("のエントリーを解除しました。").color(Colors.white))
      );
    } else {
      player.sendMessage(prefix
        .append(Component.text("既に").color(Colors.red))
        .append(color.component())
        .append(Component.text("のプレイヤーがエントリーしています。").color(Colors.red))
      );
    }
  }

  private boolean giveItems(Player player) {
    var inventory = player.getInventory();
    ItemStack weak = ItemBuilder.For(Material.TRIDENT)
      .amount(1)
      .customByteTag(itemTag, (byte) 1)
      .enchant(Enchantment.RIPTIDE, 1)
      .flags(ItemFlag.HIDE_ATTRIBUTES)
      .displayName(Component.text("HoloUp用トライデント（弱）").color(Colors.aqua))
      .build();
    if (inventory.getItem(0) != null) {
      warnNonEmptySlot(player, 0);
      return false;
    }
    inventory.setItem(0, weak);

    ItemStack strong = ItemBuilder.For(Material.TRIDENT)
      .amount(1)
      .customByteTag(itemTag, (byte) 1)
      .enchant(Enchantment.RIPTIDE, 2)
      .flags(ItemFlag.HIDE_ATTRIBUTES)
      .displayName(Component.text("HoloUp用トライデント（強）").color(Colors.aqua))
      .build();
    if (inventory.getItem(1) != null) {
      warnNonEmptySlot(player, 1);
      return false;
    }
    inventory.setItem(1, strong);

    ItemStack bed = ItemBuilder.For(Material.RED_BED)
      .amount(1)
      .customByteTag(itemTag, (byte) 1)
      .displayName(Component.text("リスポーン地点に戻る（右クリック）").color(Colors.aqua))
      .build();
    if (inventory.getItem(2) != null) {
      warnNonEmptySlot(player, 2);
      return false;
    }
    inventory.setItem(2, bed);

    return true;
  }

  private void warnNonEmptySlot(Player player, int index) {
    player.sendMessage(prefix
      .append(Component.text(String.format("インベントリのスロット%dに既にアイテムがあるため競技用アイテムを渡せません", index)).color(Colors.red))
    );
    clearItems(player);
  }

  private void clearItems(Player player) {
    PlayerInventory inventory = player.getInventory();
    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
        continue;
      }
      PersistentDataContainer container = meta.getPersistentDataContainer();
      if (container.has(NamespacedKey.minecraft(itemTag), PersistentDataType.BYTE)) {
        inventory.clear(i);
      }
    }
  }

  private void broadcast(Component message) {
    Players.Within(world, announceBounds, player -> player.sendMessage(message));
  }

  private static int x(int x) {
    return x + offset.x;
  }

  static int y(int y) {
    return y + offset.y;
  }

  private static int z(int z) {
    return z + offset.z;
  }

  private static Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
