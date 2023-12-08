package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.kbinani.holosportsfestival2023.kibasen.Session.maxHealthModifierUUID;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class KibasenEventListener implements MiniGame, Registrants.Delegate, Session.Delegate {
  public interface Delegate {
    @Nullable
    TrackAndField kibasenTakeTrackAndFieldOwnership();

    void kibasenReleaseTrackAndFieldOwnership();

    Point3i kibasenGetJoinSignLocation(TeamColor color);

    Point3i kibasenGetAnnounceEntryListSignLocation();

    Point3i kibasenGetStartSignLocation();

    BoundingBox kibasenGetAnnounceBounds();
  }

  private static final Point3i offset = new Point3i(0, 0, 0);
  public static final Component title = text("[Kibasen]", AQUA);
  static final Component prefix = title.append(text(" ", WHITE));
  static final String itemTag = "hololive_sports_festival_2023_kibasen";
  static final Point3i leaderRegistrationBarrel = pos(-30, 63, 53);
  static final String healthDisplayScoreboardTag = "hololive_sports_festival_2023_kibasen_health_display";
  private static final BoundingBox safeRespawnBounds = new BoundingBox(x(-41), y(80), z(44), x(-24), y(80), z(64));

  private final @Nonnull World world;
  private final @Nonnull JavaPlugin owner;
  private Status status = Status.IDLE;
  private final Teams teams = new Teams(Main.sScoreboardTeamPrefix + "kibasen", false);
  private final Registrants registrants = new Registrants(teams, this);
  private @Nullable Cancellable countdown;
  private @Nullable Session session;
  private final @Nonnull Delegate delegate;
  private @Nullable TrackAndField taf;

  public KibasenEventListener(@Nonnull World world, @Nonnull JavaPlugin owner, @Nonnull Delegate delegate) {
    this.owner = owner;
    this.world = world;
    this.delegate = delegate;
  }

  @Override
  public void miniGameReset() {
    reset();
  }

  @Override
  public void miniGameClearItem(Player player) {
    ClearItems(player);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    switch (status) {
      case IDLE -> {
        var action = e.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK) {
          Block block = e.getClickedBlock();
          if (block != null) {
            Point3i location = new Point3i(block.getLocation());
            for (var color : TeamColor.all) {
              if (location.equals(delegate.kibasenGetJoinSignLocation(color))) {
                onClickJoin(player, color);
                e.setCancelled(true);
                return;
              }
            }
            if (location.equals(delegate.kibasenGetAnnounceEntryListSignLocation())) {
              announceEntryList();
              e.setCancelled(true);
              return;
            } else if (location.equals(delegate.kibasenGetStartSignLocation())) {
              startCountdown();
              e.setCancelled(true);
              return;
            }
          }
        }
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
          var item = e.getItem();
          if (item != null) {
            if (item.getType() == Material.BOOK) {
              if (ItemTag.HasByte(item, itemTag)) {
                var inventory = openLeaderRegistrationInventory();
                if (inventory != null) {
                  player.openInventory(inventory);
                  e.setCancelled(true);
                  return;
                }
              }
            }
          }
        }
      }
      case COUNTDOWN, ACTIVE -> {
        var action = e.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK) {
          Block block = e.getClickedBlock();
          var taf = this.taf;
          if (block != null && taf != null) {
            Point3i location = new Point3i(block.getLocation());
            if (location.equals(taf.kibasenAbortSign)) {
              abort();
              e.setCancelled(true);
              return;
            }
          }
        }
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
    if (status == Status.IDLE) {
      var attacker = e.getPlayer();
      var hand = e.getHand();
      var inventory = attacker.getInventory();
      var item = inventory.getItem(hand);
      if (item.getType() != Material.SADDLE) {
        return;
      }
      if (!(e.getRightClicked() instanceof Player vehicle)) {
        return;
      }
      if (!ItemTag.HasByte(item, itemTag)) {
        return;
      }
      registrants.addPassenger(attacker, vehicle);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityDismount(EntityDismountEvent e) {
    if (status == Status.IDLE) {
      if (!(e.getDismounted() instanceof Player vehicle)) {
        return;
      }
      if (!(e.getEntity() instanceof Player attacker)) {
        return;
      }
      var participation = registrants.getParticipation(vehicle);
      if (participation == null) {
        return;
      }
      registrants.dismount(attacker);
      if (registrants.isEmpty()) {
        releaseTrackAndFieldOwnership();
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerQuit(PlayerQuitEvent e) {
    var player = e.getPlayer();
    registrants.remove(player);
    if (registrants.isEmpty()) {
      releaseTrackAndFieldOwnership();
    }
    //TODO: session.onPlayerQuit
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onInventoryClick(InventoryClickEvent e) {
    if (status == Status.IDLE) {
      var inventory = e.getInventory();
      if (!(inventory.getHolder(false) instanceof Barrel barrel)) {
        return;
      }
      var location = barrel.getLocation();
      if (location.getWorld() != world) {
        return;
      }
      if (!leaderRegistrationBarrel.equals(new Point3i(location))) {
        return;
      }
      if (!(e.getWhoClicked() instanceof Player player)) {
        return;
      }
      e.setCancelled(true);
      var current = registrants.getParticipation(player);
      if (current == null) {
        return;
      }
      if (e.getAction() != InventoryAction.PICKUP_ALL || !e.isLeftClick()) {
        return;
      }
      var slot = e.getSlot();
      if (slot != 10 && slot != 13 && slot != 16) {
        return;
      }
      var index = (slot - 10) / 3;
      var color = TeamColor.all[index];
      var item = inventory.getItem(slot);
      if (item == null) {
        return;
      }
      if (color != current.color) {
        return;
      }
      registrants.onClickBecomeLeader(player);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    if (session != null) {
      session.onEntityDamageByEntity(e);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityRegainHealth(EntityRegainHealthEvent e) {
    if (session != null) {
      session.onEntityRegainHealth(e);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityExhaustion(EntityExhaustionEvent e) {
    if (session != null) {
      session.onEntityExhaustion(e);
    }
  }

  @Override
  public void registrantsBroadcast(Component message) {
    broadcast(message);
  }

  @Override
  public @Nullable Inventory registrantsOpenLeaderRegistrationInventory() {
    return openLeaderRegistrationInventory();
  }

  @Override
  public void sessionDidFinish() {
    //NOTE: 本家はゲーム終了しても「ゲームを中断する」をクリックするまでは会場はリセットされない.
    reset();
  }

  private @Nullable TrackAndField takeTrackAndFieldOwnership() {
    if (this.taf != null) {
      return this.taf;
    }
    var taf = delegate.kibasenTakeTrackAndFieldOwnership();
    if (taf == null) {
      return null;
    }
    taf.setMode(TrackAndField.Mode.FIELD);
    this.taf = taf;
    return taf;
  }

  private void releaseTrackAndFieldOwnership() {
    if (this.taf == null) {
      return;
    }
    this.taf.setMode(TrackAndField.Mode.IDLE);
    this.taf = null;
    delegate.kibasenReleaseTrackAndFieldOwnership();
  }

  static void ClearItems(Player player) {
    var inventory = player.getInventory();
    for (int i = 0; i < inventory.getSize(); i++) {
      var item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      if (ItemTag.HasByte(item, itemTag)) {
        inventory.clear(i);
      }
    }
    var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
    if (maxHealth != null) {
      if (maxHealth.getModifier(maxHealthModifierUUID) != null) {
        maxHealth.removeModifier(maxHealthModifierUUID);
        player.setHealth(maxHealth.getValue());
      }
    }
  }

  private void startCountdown() {
    if (this.countdown != null) {
      this.countdown.cancel();
    }
    if (!registrants.validate()) {
      return;
    }
    if (takeTrackAndFieldOwnership() == null) {
      broadcast(prefix.append(text("他の競技が進行中です。ゲームを開始できません。", RED)));
      return;
    }
    announceEntryList();
    var titlePrefix = text("スタートまで", AQUA);
    var subtitle = text("騎馬戦", GREEN);
    this.countdown = new Countdown(
      owner, world, delegate.kibasenGetAnnounceBounds(),
      titlePrefix, AQUA, subtitle,
      10, this::start);
    this.status = Status.COUNTDOWN;
  }

  private void start() {
    this.countdown = null;
    var session = this.registrants.promote(owner, world, delegate.kibasenGetAnnounceBounds(), this);
    if (session == null) {
      abort();
      return;
    }
    this.session = session;
    this.registrants.clear();
    broadcast(prefix
      .append(text("ゲームがスタートしました。", WHITE))
    );
    this.status = Status.ACTIVE;
  }

  private void abort() {
    broadcast(prefix.append(text("ゲームを中断しました。", RED)));
    releaseTrackAndFieldOwnership();
    if (this.session != null) {
      this.session.abort();
      this.session = null;
    }
    if (this.countdown != null) {
      this.countdown.cancel();
      this.countdown = null;
    }
    this.status = Status.IDLE;
  }

  private void announceEntryList() {
    registrants.announceEntryList();
  }

  private void onClickJoin(Player player, TeamColor color) {
    var current = registrants.getParticipation(player);
    if (current == null) {
      registrants.addAttacker(player, color);
    } else {
      registrants.remove(player);
      if (registrants.isEmpty()) {
        releaseTrackAndFieldOwnership();
      }
    }
  }

  static ItemStack CreateSaddle() {
    return ItemBuilder.For(Material.SADDLE)
      .displayName(text("自分の馬を右クリックしてください！", GOLD))
      .customByteTag(itemTag)
      .build();
  }

  static ItemStack CreateBook() {
    return ItemBuilder.For(Material.BOOK)
      .displayName(text("大将にエントリー (右クリックで使用)", GOLD))
      .customByteTag(itemTag)
      .build();
  }

  static ItemStack CreateWool(TeamColor color) {
    return ItemBuilder.For(color.wool)
      .displayName(color.component().append(text(" の大将になる！", WHITE)))
      .build();
  }

  private void broadcast(Component message) {
    Players.Within(world, delegate.kibasenGetAnnounceBounds(), player -> player.sendMessage(message));
  }

  private void reset() {
    Editor.Set(world, leaderRegistrationBarrel, Material.BARREL.createBlockData());
    registrants.clearLeaderRegistrationBarrel();

    Kill.EntitiesByScoreboardTag(world, healthDisplayScoreboardTag);
    Bukkit.getServer().getOnlinePlayers().forEach(KibasenEventListener::ClearItems);

    registrants.clear();
    if (session != null) {
      session.clear(safeRespawnBounds);
      session = null;
    }
    releaseTrackAndFieldOwnership();
    status = Status.IDLE;
  }

  private @Nullable Inventory openLeaderRegistrationInventory() {
    var block = world.getBlockAt(leaderRegistrationBarrel.toLocation(world));
    var state = block.getState(false);
    if (state instanceof Barrel barrel) {
      return barrel.getInventory();
    } else {
      return null;
    }
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

  static Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
