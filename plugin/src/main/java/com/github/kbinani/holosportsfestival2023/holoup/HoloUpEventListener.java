package com.github.kbinani.holosportsfestival2023.holoup;

import com.github.kbinani.holosportsfestival2023.*;
import lombok.experimental.ExtensionMethod;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@ExtensionMethod({PlayerExtension.class, ItemStackExtension.class})
public class HoloUpEventListener implements MiniGame, Race.Delegate {
  // 位置をずらしたい場合はここでずらす
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final Component title = text("[Holoup]", AQUA);
  static final Component prefix = title.append(text(" ", WHITE));
  private static final Point3i joinSignRed = pos(-42, 100, -29);
  private static final Point3i joinSignWhite = pos(-41, 100, -29);
  private static final Point3i joinSignYellow = pos(-40, 100, -29);
  private static final Point3i abortSign = pos(-36, 100, -29);
  private static final Point3i startSign = pos(-37, 100, -29);
  private static final Point3i entryListSign = pos(-35, 100, -29);
  private static final BoundingBox announceBounds = new BoundingBox(x(-59), y(99), z(-63), x(12), 500, z(-19));
  private static final BoundingBox safeArea = new BoundingBox(x(-45), y(100) - 0.5, z(-34), x(-31), y(100) - 0.5, z(-23));
  static final String itemTag = "holo_sports_festival_holoup";
  static final String itemTagStrong = "holo_sports_festival_holoup_trident_strong";

  private final @Nonnull World world;
  private final @Nonnull JavaPlugin owner;
  private Status status = Status.IDLE;
  private final Map<TeamColor, EntityTracking<Player>> registrants = new HashMap<>();
  private @Nullable Cancellable countdownTask;
  private @Nullable Race race;
  private final Set<UUID> spectators = new HashSet<>();
  private final @Nonnull Announcer announcer;

  public HoloUpEventListener(@Nonnull World world, @Nonnull JavaPlugin owner, @Nonnull Announcer announcer) {
    this.world = world;
    this.owner = owner;
    this.announcer = announcer;
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
  public BoundingBox miniGameGetBoundingBox() {
    return announceBounds;
  }

  @Override
  public void raceDidFinish() {
    reset();
  }

  @Override
  public void raceDidDetectGoal(TeamColor color, Player player) {
    broadcast(prefix
      .append(text(player.getName(), color.textColor))
      .appendSpace()
      .append(text("がゴールしました！", GOLD)));
  }

  @Override
  public void raceDidDetectCheckpoint(TeamColor color, Player player, int index) {
    broadcast(prefix
      .append(text(player.getName(), color.textColor))
      .append(text(String.format(" が%d個目のチェックポイントに到達しました！", index), WHITE)));
  }

  private void reset() {
    Editor.StandingSign(world, joinSignRed, Material.OAK_SIGN, 0,
      title, TeamColor.RED.component(), Component.empty(), text("右クリでエントリー！", AQUA)
    );
    Editor.StandingSign(world, joinSignWhite, Material.OAK_SIGN, 0,
      title, TeamColor.WHITE.component(), Component.empty(), text("右クリでエントリー！", AQUA)
    );
    Editor.StandingSign(world, joinSignYellow, Material.OAK_SIGN, 0,
      title, TeamColor.YELLOW.component(), Component.empty(), text("右クリでエントリー！", AQUA)
    );

    Editor.StandingSign(world, startSign, Material.OAK_SIGN, 0,
      title, Component.empty(), Component.empty(), text("ゲームスタート", AQUA)
    );
    Editor.StandingSign(world, abortSign, Material.OAK_SIGN, 0,
      title, Component.empty(), Component.empty(), text("ゲームを中断する", RED)
    );
    Editor.StandingSign(world, entryListSign, Material.OAK_SIGN, 0,
      title, Component.empty(), Component.empty(), text("エントリー リスト", GREEN)
    );

    for (var playerTracking : registrants.values()) {
      Cloakroom.shared.restore(playerTracking.get());
    }
    registrants.clear();
    status = Status.IDLE;
    Kill.EntitiesByScoreboardTag(world, itemTag);
    Bukkit.getServer().getOnlinePlayers().forEach(this::clearItems);
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    if (race != null) {
      race.cleanup();
    }
    for (var id : spectators) {
      var player = Bukkit.getPlayer(id);
      if (player != null) {
        player.spread(safeArea);
        player.setGameMode(GameMode.ADVENTURE);
      }
    }
    spectators.clear();
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
              .append(text("参加者がいません", RED)));
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
            .append(text("ゲームを中断しました", RED)));
        } else if (location.equals(entryListSign)) {
          announceEntryList();
        } else if (location.equals(startSign)) {
          player.sendMessage(text("ゲームが既に開始しています。", RED));
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
            .append(text("ゲームを中断しました", RED)));
        } else if (location.equals(startSign)) {
          player.sendMessage(text("ゲームが既に開始しています。", RED));
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
    if (registrants.values().stream().anyMatch(it -> it.get() == player)) {
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
    if (registrants.values().stream().anyMatch(it -> it.get() == player)) {
      leave(player);
    }
    if (race != null) {
      race.onPlayerLeave(player);
    }
  }

  private void announceEntryList() {
    broadcast(text("-".repeat(10), GREEN)
      .appendSpace()
      .append(prefix)
      .appendSpace()
      .append(text("エントリー者 ", AQUA))
      .append(text("-".repeat(10), GREEN)));
    var first = true;
    for (var color : TeamColor.all) {
      if (!first) {
        broadcast(Component.empty());
      }
      first = false;
      var playerTracking = registrants.get(color);
      int count = playerTracking == null ? 0 : 1;
      broadcast(text(String.format(" %s (%d)", color.text, count), color.textColor));
      if (playerTracking != null) {
        broadcast(text(String.format("  - %s", playerTracking.get().getName()), color.textColor));
      }
    }
  }

  private void startCountdown() {
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }
    status = Status.COUNTDOWN;
    var titlePrefix = text("スタートまで", AQUA);
    var subtitle = text("上を目指せ!holoUp!", GREEN);
    countdownTask = new Countdown(
      owner,
      world,
      announceBounds,
      titlePrefix,
      AQUA,
      subtitle,
      10,
      this::start
    );
  }

  private void start() {
    boolean ok = true;
    for (var playerTracking : registrants.values()) {
      if (!prepare(playerTracking.get())) {
        ok = false;
      }
    }
    if (!ok) {
      for (var playerTracking : registrants.values()) {
        clearItems(playerTracking.get());
      }
      broadcast(prefix
        .append(text("準備が整っていない参加者がいたため中断しました。", RED)));
      status = Status.IDLE;
      return;
    }
    if (this.race != null) {
      this.race.cancel();
      this.race = null;
    }
    this.race = new Race(owner, world, announceBounds, registrants, announcer, this);
    status = Status.ACTIVE;
    broadcast(prefix
      .append(text("ゲームがスタートしました。", WHITE)));
  }

  private void onClickJoin(Player player, TeamColor color) {
    if (spectators.contains(player.getUniqueId())) {
      return;
    }
    var current = registrants.get(color);
    if (current == null) {
      if (registrants.values().stream().anyMatch(it -> it.get() == player)) {
        player.sendMessage(prefix
          .append(text("既に他のチームにエントリーしています。", RED))
        );
      } else {
        if (!Cloakroom.shared.store(player, prefix)) {
          return;
        }
        registrants.put(color, new EntityTracking<>(player));
        broadcast(prefix
          .append(text(player.getName(), color.textColor))
          .append(text("が", WHITE))
          .append(color.component())
          .append(text("にエントリーしました。", WHITE))
        );
      }
    } else if (current.get() == player) {
      leave(player);
    } else {
      player.sendMessage(prefix
        .append(text("既に", RED))
        .append(color.component())
        .append(text("のプレイヤーがエントリーしています。", RED))
      );
    }
  }

  private void leave(Player player) {
    @Nullable TeamColor color = null;
    for (var entry : registrants.entrySet()) {
      if (entry.getValue().get() == player) {
        color = entry.getKey();
        break;
      }
    }
    if (color != null) {
      Cloakroom.shared.restore(player);
      registrants.remove(color);
      broadcast(prefix
        .append(text(player.getName() + "が", WHITE))
        .append(color.component())
        .append(text("のエントリーを解除しました。", WHITE))
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
        .append(text("規定の位置より高い位置に居るためスタートできません。", RED))
      );
      return false;
    }
    var inventory = player.getInventory();
    ItemStack weak = ItemBuilder.For(Material.TRIDENT)
      .amount(1)
      .customTag(itemTag)
      .enchant(Enchantment.RIPTIDE, 1)
      .flags(ItemFlag.HIDE_ATTRIBUTES)
      .displayName(text("HoloUp用トライデント（弱）", AQUA))
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
      .customTag(itemTag)
      .displayName(text("リスポーン地点に戻る（右クリック）", AQUA))
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
      .customTag(itemTag)
      .customTag(itemTagStrong)
      .enchant(Enchantment.RIPTIDE, 2)
      .flags(ItemFlag.HIDE_ATTRIBUTES)
      .displayName(text("HoloUp用トライデント（強）", GREEN))
      .build();
  }

  static boolean IsStrongItem(ItemStack item) {
    return item.hasCustomTag(itemTagStrong);
  }

  private void warnNonEmptySlot(Player player, int index) {
    player.sendMessage(prefix
      .append(text(String.format("インベントリのスロット%dに既にアイテムがあるため競技用アイテムを渡せません", index), RED))
    );
  }

  private void clearItems(Player player) {
    PlayerInventory inventory = player.getInventory();
    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      if (item.hasCustomTag(itemTag)) {
        inventory.clear(i);
      }
    }
    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
  }

  private void broadcast(Component message) {
    announcer.announce(message, announceBounds);
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
