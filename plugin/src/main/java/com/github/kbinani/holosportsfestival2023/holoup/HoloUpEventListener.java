package com.github.kbinani.holosportsfestival2023.holoup;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;

public class HoloUpEventListener implements MiniGame, Race.Delegate {
  // 位置をずらしたい場合はここでずらす
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final Component title = Text("[Holoup]", NamedTextColor.AQUA);
  static final Component prefix = title.append(Text(" "));
  private static final Point3i joinSignRed = pos(-42, 100, -29);
  private static final Point3i joinSignWhite = pos(-41, 100, -29);
  private static final Point3i joinSignYellow = pos(-40, 100, -29);
  private static final Point3i abortSign = pos(-36, 100, -29);
  private static final Point3i startSign = pos(-37, 100, -29);
  private static final Point3i entryListSign = pos(-35, 100, -29);
  private static final BoundingBox announceBounds = new BoundingBox(x(-59), y(99), z(-63), x(12), 500, z(-19));
  static final String itemTag = "holo_sports_festival_holoup";
  static final String itemTagStrong ="holo_sports_festival_holoup_trident_strong";

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
      .append(Text(player.getName(), color.textColor))
      .appendSpace()
      .append(Text("がゴールしました！", NamedTextColor.GOLD)));
  }

  @Override
  public void raceDidDetectCheckpoint(TeamColor color, Player player, int index) {
    broadcast(prefix
      .append(Text(player.getName(), color.textColor))
      .append(Text(String.format(" が%d個目のチェックポイントに到達しました！", index))));
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
      Text("右クリでエントリー！", NamedTextColor.AQUA)
    );
    Editor.StandingSign(
      world,
      joinSignWhite,
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.WHITE.component(),
      Component.empty(),
      Text("右クリでエントリー！", NamedTextColor.AQUA)
    );
    Editor.StandingSign(
      world,
      joinSignYellow,
      Material.OAK_SIGN,
      0,
      title,
      TeamColor.YELLOW.component(),
      Component.empty(),
      Text("右クリでエントリー！", NamedTextColor.AQUA)
    );
    Editor.StandingSign(
      world,
      pos(-39, 100, -29),
      Material.OAK_SIGN,
      0,
      title,
      Text("観戦者", NamedTextColor.DARK_PURPLE),
      Text("Spectator", NamedTextColor.DARK_PURPLE),
      Text("右クリでエントリー！", NamedTextColor.AQUA)
    );

    Editor.StandingSign(
      world,
      startSign,
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Text("ゲームスタート", NamedTextColor.AQUA)
    );
    Editor.StandingSign(
      world,
      abortSign,
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Text("ゲームを中断する", NamedTextColor.RED)
    );
    Editor.StandingSign(
      world,
      entryListSign,
      Material.OAK_SIGN,
      0,
      title,
      Component.empty(),
      Component.empty(),
      Text("エントリー リスト", NamedTextColor.GREEN)
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
              .append(Text("参加者がいません", NamedTextColor.RED)));
          } else {
            announceEntryList();
            startCountdown();
          }
        } else if (location.equals(entryListSign)) {
          announceEntryList();
        }
      }
      case COUNTDOWN -> {
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
            .append(Text("ゲームを中断しました", NamedTextColor.RED)));
        } else if (location.equals(entryListSign)) {
          announceEntryList();
        } else if (location.equals(startSign)) {
          player.sendMessage(Text("ゲームが既に開始しています。", NamedTextColor.RED));
        }
      }
      case ACTIVE -> {
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
            .append(Text("ゲームを中断しました", NamedTextColor.RED)));
        } else if (location.equals(startSign)) {
          player.sendMessage(Text("ゲームが既に開始しています。", NamedTextColor.RED));
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

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityDamage(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player player)) {
      return;
    }
    if (e.getCause() != EntityDamageEvent.DamageCause.FALL) {
      return;
    }
    if (announceBounds.contains(player.getLocation().toVector())) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
    if (race == null) {
      return;
    }
    race.onPlayerToggleSneak(e);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerRiptide(PlayerRiptideEvent e) {
    if (race == null) {
      return;
    }
    race.onPlayerRiptide(e);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerQuit(PlayerQuitEvent e) {
    var player = e.getPlayer();
    if (registrants.containsValue(player)) {
      leave(player);
    }
    if (race != null) {
      race.onPlayerLeave(player);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
    var player = e.getPlayer();
    if (registrants.containsValue(player)) {
      leave(player);
    }
    if (race != null) {
      race.onPlayerLeave(player);
    }
  }

  private void announceEntryList() {
    broadcast(Text("-".repeat(10), NamedTextColor.GREEN)
      .appendSpace()
      .append(prefix)
      .appendSpace()
      .append(Text("エントリー者 ", NamedTextColor.AQUA))
      .append(Text("-".repeat(10), NamedTextColor.GREEN)));
    var first = true;
    for (var color : TeamColor.all) {
      if (!first) {
        broadcast(Component.empty());
      }
      first = false;
      var player = registrants.get(color);
      int count = player == null ? 0 : 1;
      broadcast(Text(String.format(" %s (%d)", color.text, count), color.textColor));
      if (player != null) {
        broadcast(Text(String.format("  - %s", player.getName()), color.textColor));
      }
    }
  }

  private void startCountdown() {
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    status = Status.COUNTDOWN;
    var titlePrefix = Text("スタートまで", NamedTextColor.AQUA);
    var subtitle = Text("上を目指せ!holoUp!", NamedTextColor.GREEN);
    countdownTask = new Countdown(
      owner,
      world,
      announceBounds,
      titlePrefix,
      NamedTextColor.AQUA,
      subtitle,
      10,
      this::start
    );
  }

  private void start() {
    boolean ok = true;
    for (var player : registrants.values()) {
      if (!prepare(player)) {
        ok = false;
      }
    }
    if (!ok) {
      for (var player : registrants.values()) {
        clearItems(player);
      }
      broadcast(prefix
        .append(Text("準備が整っていない参加者がいたため中断しました。", NamedTextColor.RED)));
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
      .append(Text("ゲームがスタートしました。")));
  }

  private void onClickJoin(Player player, TeamColor color) {
    var current = registrants.get(color);
    if (current == null) {
      if (registrants.containsValue(player)) {
        player.sendMessage(prefix
          .append(Text("既に他のチームにエントリーしています。", NamedTextColor.RED))
        );
      } else {
        registrants.put(color, player);
        broadcast(prefix
          .append(Text(player.getName(), color.textColor))
          .append(Text("が"))
          .append(color.component())
          .append(Text("にエントリーしました。"))
        );
      }
    } else if (current == player) {
      leave(player);
    } else {
      player.sendMessage(prefix
        .append(Text("既に", NamedTextColor.RED))
        .append(color.component())
        .append(Text("のプレイヤーがエントリーしています。", NamedTextColor.RED))
      );
    }
  }

  private void leave(Player player) {
    @Nullable TeamColor color = null;
    for (var entry : registrants.entrySet()) {
      if (entry.getValue().getUniqueId().equals(player.getUniqueId())) {
        color = entry.getKey();
        break;
      }
    }
    if (color != null) {
      registrants.remove(color);
      broadcast(prefix
        .append(Text(player.getName() + "が"))
        .append(color.component())
        .append(Text("のエントリーを解除しました。"))
      );
    }
    if (registrants.isEmpty() && countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
      status = Status.IDLE;
    }
  }

  private boolean prepare(Player player) {
    var y = player.getLocation().getBlockY();
    if (y > Race.groundLevel) {
      player.sendMessage(prefix
        .append(Text("規定の位置より高い位置に居るためスタートできません。", NamedTextColor.RED))
      );
      return false;
    }
    var inventory = player.getInventory();
    ItemStack weak = ItemBuilder.For(Material.TRIDENT)
      .amount(1)
      .customByteTag(itemTag, (byte) 1)
      .enchant(Enchantment.RIPTIDE, 1)
      .flags(ItemFlag.HIDE_ATTRIBUTES)
      .displayName(Text("HoloUp用トライデント（弱）", NamedTextColor.AQUA))
      .build();
    if (inventory.getItem(0) != null) {
      warnNonEmptySlot(player, 0);
      return false;
    }
    inventory.setItem(0, weak);

    var strong = CreateStrongTrident();
    if (inventory.getItem(1) != null) {
      warnNonEmptySlot(player, 1);
      return false;
    }
    inventory.setItem(1, strong);

    ItemStack bed = ItemBuilder.For(Material.RED_BED)
      .amount(1)
      .customByteTag(itemTag, (byte) 1)
      .displayName(Text("リスポーン地点に戻る（右クリック）", NamedTextColor.AQUA))
      .build();
    if (inventory.getItem(2) != null) {
      warnNonEmptySlot(player, 2);
      return false;
    }
    inventory.setItem(2, bed);

    return true;
  }

  static ItemStack CreateStrongTrident() {
    return ItemBuilder.For(Material.TRIDENT)
      .amount(1)
      .customByteTag(itemTag, (byte) 1)
      .customByteTag(itemTagStrong, (byte) 1)
      .enchant(Enchantment.RIPTIDE, 2)
      .flags(ItemFlag.HIDE_ATTRIBUTES)
      .displayName(Text("HoloUp用トライデント（強）", NamedTextColor.GREEN))
      .build();
  }

  static boolean IsStrongItem(ItemStack item) {
    var meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    PersistentDataContainer container = meta.getPersistentDataContainer();
    return container.has(NamespacedKey.minecraft(itemTagStrong), PersistentDataType.BYTE);
  }

  private void warnNonEmptySlot(Player player, int index) {
    player.sendMessage(prefix
      .append(Text(String.format("インベントリのスロット%dに既にアイテムがあるため競技用アイテムを渡せません", index), NamedTextColor.RED))
    );
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
    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
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
