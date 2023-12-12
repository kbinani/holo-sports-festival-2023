package com.github.kbinani.holosportsfestival2023.relay;

import com.github.kbinani.holosportsfestival2023.*;
import lombok.experimental.ExtensionMethod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@ExtensionMethod({WorldExtension.class, ItemStackExtension.class})
public class RelayEventListener implements MiniGame, Race.Delegate {
  public interface Delegate {
    Point3i relayGetJoinSignLocation(TeamColor color);

    Point3i relayGetAnnounceEntryListSignLocation();

    Point3i relayGetStartSignLocation();

    Point3i relayGetAbortSignLocation();

    BoundingBox relayGetAnnounceBounds();

    @Nullable
    TrackAndField relayTakeTrackAndFieldOwnership();

    void relayReleaseTrackAndFieldOwnership();
  }

  public static final Component title = text("[Relay]", AQUA);
  static final Component prefix = title.append(text(" ", WHITE));
  private static final Point3i offset = new Point3i(0, 0, 0);
  private static final String sBubbleScoreboardTag = "hololive_sports_festival2023_relay_bubble";
  private static final String sBreadHangerScoreboardTag = "hololive_sports_festival_2023_relay_bread";
  private static final String sItemTag = "hololive_sports_festival_2023_relay_item";

  private final @Nonnull World world;
  private final @Nonnull JavaPlugin owner;
  private final @Nonnull Map<TeamColor, Team> teams = new HashMap<>();
  private final Set<Point3i> pistonPositions;
  private final @Nonnull BukkitTask waveTimer;
  private final List<Wave> waves = new ArrayList<>();
  private boolean wavePrepared = false;
  private boolean breadHangerPrepared = false;
  private final List<EntityTracking<ArmorStand>> breadHangers = new ArrayList<>();
  private Status status = Status.IDLE;
  private final @Nonnull Delegate delegate;
  private @Nullable Race race;
  private @Nullable Inventory entryBookInventory;
  private @Nullable Countdown countdown;
  private @Nullable TrackAndField taf;
  private final @Nonnull Point3i safeSpot = pos(4, 80, 60);
  private final @Nonnull Teams scoreboardTeams = new Teams(Main.sScoreboardTeamPrefix + "relay", true);
  private final @Nonnull BoundingBox poseChangeDetectionArea;
  private final @Nonnull Announcer announcer;

  public RelayEventListener(@Nonnull World world, @Nonnull JavaPlugin owner, @Nonnull Announcer announcer, @Nonnull Delegate delegate) {
    this.world = world;
    this.owner = owner;
    this.announcer = announcer;
    this.delegate = delegate;
    pistonPositions = new HashSet<>();
    pistonPositions.add(pos(57, 79, 57));
    pistonPositions.add(pos(59, 79, 57));
    pistonPositions.add(pos(61, 79, 57));
    poseChangeDetectionArea = new BoundingBox(x(55), y(80), z(64), x(64), y(83), z(68));
    this.waveTimer = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 1, 1);
  }

  @Override
  public void miniGameReset() {
    reset();
  }

  @Override
  public void miniGameClearItem(Player player) {
    ClearItem(player);
  }

  @Override
  public BoundingBox miniGameGetBoundingBox() {
    return delegate.relayGetAnnounceBounds();
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onItemSpawn(ItemSpawnEvent e) {
    var location = e.getLocation();
    if (location.getWorld() != world) {
      return;
    }
    var item = e.getEntity();
    var stack = item.getItemStack();
    if (stack.getType() != Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
      return;
    }
    if (x(56) <= location.getX() && location.getX() <= x(63) &&
      y(80) <= location.getY() && location.getY() <= y(91) &&
      z(56) <= location.getZ() && location.getZ() <= z(59)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onBlockPistonRetract(BlockPistonRetractEvent e) {
    var block = e.getBlock();
    var location = new Point3i(block.getLocation());
    if (pistonPositions.contains(location)) {
      var pressurePlatePos = location.added(0, 2, 0);
      Bukkit.getScheduler().runTaskLater(owner, () -> {
        world.set(pressurePlatePos, Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
      }, 10);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent e) {
    var armorStand = e.getRightClicked();
    for (var stand : breadHangers) {
      if (stand.get() == armorStand) {
        e.setCancelled(true);
        return;
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    if (status == Status.ACTIVE && race != null) {
      race.onEntityDamageByEntity(e);
    }
    if (!(e.getDamager() instanceof Player player)) {
      return;
    }
    if (!(e.getEntity() instanceof ArmorStand armorStand)) {
      return;
    }
    if (breadHangers.stream().noneMatch(it -> it.get() == armorStand)) {
      return;
    }
    e.setCancelled(true);
    var inventory = player.getInventory();
    inventory.addItem(createBread());
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    var action = e.getAction();
    var player = e.getPlayer();
    var item = e.getItem();
    if (status == Status.IDLE) {
      if (action == Action.RIGHT_CLICK_BLOCK) {
        var block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        var location = new Point3i(block.getLocation());
        for (var color : TeamColor.all) {
          if (delegate.relayGetJoinSignLocation(color).equals(location)) {
            onClickJoin(player, color);
            e.setCancelled(true);
            return;
          }
        }
        if (delegate.relayGetStartSignLocation().equals(location)) {
          onClickStart();
          e.setCancelled(true);
          return;
        } else if (delegate.relayGetAnnounceEntryListSignLocation().equals(location)) {
          announceEntryList(teams);
          e.setCancelled(true);
          return;
        } else if (delegate.relayGetAbortSignLocation().equals(location)) {
          reset();
          e.setCancelled(true);
          return;
        }
      }
      if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
        if (item != null && item.getType() == Material.BOOK && item.hasCustomTag(sItemTag)) {
          for (var team : teams.values()) {
            if (team.players().contains(player)) {
              var inventory = ensureEntryBookInventory();
              player.openInventory(inventory);
              e.setCancelled(true);
              return;
            }
          }
        }
      }
    } else {
      if (action == Action.RIGHT_CLICK_BLOCK) {
        var block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        var location = new Point3i(block.getLocation());
        if (delegate.relayGetAbortSignLocation().equals(location)) {
          abort();
          e.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onInventoryClick(InventoryClickEvent e) {
    var view = e.getView();
    var inventory = ensureEntryBookInventory();
    if (view.getTopInventory() != inventory) {
      return;
    }
    var slot = e.getRawSlot();
    if (slot < 0 || 27 <= slot) {
      return;
    }
    e.setCancelled(true);
    if (!(e.getWhoClicked() instanceof Player player)) {
      return;
    }
    if (e.getAction() != InventoryAction.PICKUP_ALL) {
      return;
    }
    var color = getRegistration(player);
    if (color == null) {
      return;
    }
    int order = slot - color.ordinal() * 9;
    if (order < 0 || 9 <= order) {
      return;
    }
    var item = e.getCurrentItem();
    var team = ensureTeam(color);
    var current = team.getCurrentOrder(player);
    if (current == null) {
      if (team.assign(player, order)) {
        broadcast(prefix
          .append(color.component())
          .append(text(String.format("の第%d走者に", order + 1), WHITE))
          .append(text(player.getName(), GOLD))
          .append(text("がエントリーしました！", WHITE))
        );
        var head = ItemBuilder.For(Material.PLAYER_HEAD)
          .displayName(color.component().append(text(String.format(" 第%d走者 ", order + 1), WHITE).append(text(player.getName(), GOLD))))
          .meta(SkullMeta.class, it -> it.setOwningPlayer(player))
          .build();
        inventory.setItem(slot, head);
      } else {
        // https://youtu.be/las27v3TLW8?t=5334
        player.sendMessage(prefix.append(text("他のプレイヤーが選択しています。", RED)));
      }
    } else if (current == order) {
      // メッセージは出ない. https://youtu.be/uEpmE5WJPW8?t=5234
      team.unassign(player);
      inventory.setItem(slot, createPlaceholder(color, order));
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerMove(PlayerMoveEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    if (race == null) {
      return;
    }
    race.onPlayerMove(e);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onEntityPoseChange(EntityPoseChangeEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    if (!(e.getEntity() instanceof Player player)) {
      return;
    }
    if (!poseChangeDetectionArea.contains(player.getLocation().toVector())) {
      return;
    }
    var before = player.getPose();
    var after = e.getPose();

    if (before == Pose.SWIMMING && after != Pose.SWIMMING) {
      // https://youtu.be/ls3kb0qhT4E?t=11047
      player.setFoodLevel(2);
      player.sendMessage(prefix.append(text("パンを左クリックで取って食べよう！", WHITE)));
      player.sendMessage(prefix.append(text("Left click to get bread and eat it!", WHITE)));
      var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
      var title = Title.title(
        text("お腹が空いてしまった！", RED),
        text("I'm hungry!", GOLD),
        times
      );
      player.showTitle(title);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
    if (status != Status.ACTIVE) {
      return;
    }
    if (race == null) {
      return;
    }
    var item = e.getItem();
    var player = e.getPlayer();
    if (item.getType() == Material.BREAD && item.hasCustomTag(race.getBreadId())) {
      player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
      player.setFoodLevel(20);
    }
  }

  private @Nullable TeamColor getRegistration(Player player) {
    if (status == Status.IDLE) {
      for (var entry : teams.entrySet()) {
        var team = entry.getValue();
        if (team.players().contains(player)) {
          return entry.getKey();
        }
      }
    }
    return null;
  }

  @Override
  public void raceDidFinish() {
    var taf = this.taf;
    if (taf != null) {
      Players.Within(world, taf.photoSpotBounds, p -> p.teleport(safeSpot.toLocation(world)));
    }
    reset();
  }

  private @Nonnull Inventory ensureEntryBookInventory() {
    if (entryBookInventory == null) {
      var inventory = Bukkit.createInventory(null, 27, prefix.append(text("エントリーリスト", GREEN)));
      for (var i = 0; i < TeamColor.all.length; i++) {
        var color = TeamColor.all[i];
        for (var j = 0; j < 9; j++) {
          var index = i * 9 + j;
          var item = createPlaceholder(color, j);
          inventory.setItem(index, item);
        }
      }
      entryBookInventory = inventory;
      return inventory;
    } else {
      return entryBookInventory;
    }
  }

  private ItemStack createPlaceholder(TeamColor color, int order) {
    return ItemBuilder.For(color.quizConcealer)
      .displayName(color.component().append(text(String.format(" 第%d走者", order + 1), WHITE)))
      .build();
  }

  private void abort() {
    if (countdown != null) {
      countdown.cancel();
      countdown = null;
    }
    for (var team : this.teams.values()) {
      team.dispose();
    }
    this.teams.clear();
    if (race != null) {
      race.teleportAll(safeSpot.toLocation(world));
      var teams = race.abort();
      this.teams.putAll(teams);
    }
    for (var team : this.teams.values()) {
      for (var player : team.players()) {
        ClearItem(player);
        var inventory = player.getInventory();
        inventory.setItem(0, createEntryBook());
      }
    }
    updateEntryBookContents();
    if (status != Status.IDLE) {
      broadcast(prefix.append(text("ゲームを中断しました", RED)));
    }
    if (taf != null) {
      Players.Within(world, taf.photoSpotBounds, p -> p.teleport(safeSpot.toLocation(world)));
    }
    releaseTrackAndFieldOwnership();
    status = Status.IDLE;
  }

  private void updateEntryBookContents() {
    var inventory = ensureEntryBookInventory();
    for (var color : TeamColor.all) {
      var team = teams.get(color);
      for (var i = 0; i < 9; i++) {
        var player = team == null ? null : team.getAssignedPlayer(i);
        var index = color.ordinal() * 9 + i;
        if (player == null) {
          inventory.setItem(index, createPlaceholder(color, i));
        } else {
          var head = ItemBuilder.For(Material.PLAYER_HEAD)
            .displayName(color.component().append(text(String.format(" 第%d走者 ", i + 1), WHITE).append(text(player.getName(), GOLD))))
            .meta(SkullMeta.class, it -> it.setOwningPlayer(player))
            .build();
          inventory.setItem(index, head);
        }
      }
    }
  }

  private void announceEntryList(Map<TeamColor, Team> teams) {
    broadcast(
      text("-".repeat(10), GREEN)
        .appendSpace()
        .append(prefix)
        .appendSpace()
        .append(text("エントリー者 ", AQUA))
        .append(text("-".repeat(10), GREEN))
    );
    var first = true;
    for (var color : TeamColor.all) {
      if (!first) {
        broadcast(Component.empty());
      }
      first = false;
      var team = teams.get(color);
      if (team == null) {
        broadcast(text(String.format(" %s (0)", color.text), color.textColor));
        continue;
      }
      broadcast(text(String.format(" %s (%d)", color.text, team.getOrderLength()), color.textColor));
      for (var i = 0; i < team.getOrderLength(); i++) {
        var player = team.getAssignedPlayer(i);
        if (player != null) {
          broadcast(text(String.format(" - [%d] %s", i + 1, player.getName()), color.textColor));
        }
      }
    }
  }

  private void onClickJoin(Player player, TeamColor color) {
    if (player.getGameMode() == GameMode.SPECTATOR) {
      return;
    }
    for (var entry : teams.entrySet()) {
      var team = entry.getValue();
      var c = entry.getKey();
      if (team.players().contains(player)) {
        var order = team.getCurrentOrder(player);
        team.remove(player);
        if (order != null) {
          var inventory = ensureEntryBookInventory();
          inventory.setItem(c.ordinal() * 9 + order, createPlaceholder(c, order));
        }
        player.sendMessage(prefix.append(text("エントリー登録を解除しました。", WHITE)));
        Cloakroom.shared.restore(player);
        return;
      }
    }
    if (!Cloakroom.shared.store(player, prefix)) {
      return;
    }
    var team = ensureTeam(color);
    team.add(player);
    broadcast(prefix
      .append(text(String.format("%sが", player.getName()), WHITE))
      .append(color.component())
      .append(text("にエントリーしました。", WHITE)));
    player.sendMessage(prefix
      .append(text("エントリーブックを使用して走順を選択してください！", WHITE)));
    player.sendMessage(prefix
      .append(text("Right-click while having the Entry Book equipped to select your relay position!", WHITE)));
    var inventory = player.getInventory();
    inventory.setItem(0, createEntryBook());
  }

  private @Nonnull ItemStack createEntryBook() {
    return ItemBuilder.For(Material.BOOK)
      .displayName(text("エントリーブック (右クリックで使用) / Entry Book (Right click to open)", GOLD))
      .customTag(sItemTag)
      .build();
  }

  static @Nonnull ItemStack CreateBaton(TeamColor color) {
    return ItemBuilder.For(Material.BLAZE_ROD)
      .meta(ItemMeta.class, it -> {
        it.setCustomModelData(color.ordinal() + 1);
      })
      .displayName(color.component().append(text("チームバトン", WHITE)))
      .customTag(sItemTag)
      .build();
  }

  private @Nonnull Team ensureTeam(TeamColor color) {
    var team = teams.get(color);
    if (team == null) {
      var t = new Team(color, scoreboardTeams.ensure(color));
      teams.put(color, t);
      return t;
    } else {
      return team;
    }
  }

  private void onClickStart() {
    if (status != Status.IDLE) {
      return;
    }
    var taf = takeTrackAndFieldOwnership();
    if (taf == null) {
      broadcast(prefix.append(text("他の競技が進行中です。ゲームを開始できません。", RED)));
      return;
    }
    var result = Race.From(owner, world, this.teams, offset, delegate.relayGetAnnounceBounds(), safeSpot, announcer, this);
    if (result.reason != null) {
      broadcast(prefix.append(result.reason));
      return;
    }
    if (result.value == null) {
      broadcast(prefix.append(text("不明なエラー", RED)));
      return;
    }
    if (race != null) {
      race.dispose();
    }
    taf.setRelayStartGateEnabled(true);
    taf.setEnablePhotoSpot(false);
    announceEntryList(result.value.teams);
    race = result.value;
    race.teleportAll(safeSpot.toLocation(world));
    Players.Within(world, taf.photoSpotBounds, p -> p.teleport(safeSpot.toLocation(world)));
    race.prepare();
    if (entryBookInventory != null) {
      entryBookInventory.close();
    }
    status = Status.COUNTDOWN;
    var titlePrefix = text("スタートまで", AQUA);
    var subtitle = text("春夏秋冬リレー", GREEN);
    countdown = new Countdown(
      owner, world, delegate.relayGetAnnounceBounds(),
      titlePrefix, AQUA, subtitle,
      10, this::start
    );
  }

  private @Nullable TrackAndField takeTrackAndFieldOwnership() {
    if (this.taf != null) {
      return this.taf;
    }
    var taf = delegate.relayTakeTrackAndFieldOwnership();
    if (taf == null) {
      return null;
    }
    taf.setMode(TrackAndField.Mode.TRACK);
    this.taf = taf;
    return taf;
  }

  private void releaseTrackAndFieldOwnership() {
    if (this.taf == null) {
      return;
    }
    this.taf.setMode(TrackAndField.Mode.IDLE);
    this.taf = null;
    delegate.relayReleaseTrackAndFieldOwnership();
  }

  private void start() {
    if (status != Status.COUNTDOWN) {
      return;
    }
    var taf = takeTrackAndFieldOwnership();
    if (taf == null) {
      broadcast(prefix.append(text("他の競技が進行中です。ゲームを開始できません。", RED)));
      abort();
      return;
    }
    if (race == null) {
      return;
    }
    taf.setRelayStartGateEnabled(false);
    status = Status.ACTIVE;
    //NOTE: 本家では号砲は鳴っていないぽい
    world.playSound(new Location(world, x(3) + 0.5, y(82), z(71) + 0.5), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
    race.start();
  }

  private void reset() {
    for (var team : teams.values()) {
      team.dispose();
    }
    teams.clear();
    if (race != null) {
      race.dispose();
      race = null;
    }
    if (entryBookInventory != null) {
      entryBookInventory.close();
      entryBookInventory = null;
    }
    if (countdown != null) {
      countdown.cancel();
      countdown = null;
    }
    releaseTrackAndFieldOwnership();
    Bukkit.getServer().getOnlinePlayers().forEach(RelayEventListener::ClearItem);

    // NOTE: 逆走防止
    world.fill(pos(55, 82, 67), pos(63, 82, 67), Material.BARRIER);

    status = Status.IDLE;
  }

  static void ClearItem(Player player) {
    var inventory = player.getInventory();
    for (int i = 0; i < inventory.getSize(); i++) {
      var item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      if (item.hasCustomTag(sItemTag)) {
        inventory.clear(i);
      }
    }
  }

  private void tick() {
    tickWaves();
    tickBreadHanger();
  }

  private void tickBreadHanger() {
    if (breadHangerPrepared) {
      return;
    }
    var minX = x(55) / 16;
    var maxX = x(63) / 16;
    var cz = z(55) / 16;
    for (int cx = minX; cx <= maxX; cx++) {
      if (!world.isChunkLoaded(cx, cz)) {
        return;
      }
    }
    breadHangerPrepared = true;
    Kill.EntitiesByScoreboardTag(world, sBreadHangerScoreboardTag);
    for (var i = 0; i < 3; i++) {
      var stand = world.spawn(new Location(world, x(57) + 0.5 + i * 2, y(86) + 0.6, z(55) + 0.8), ArmorStand.class, it -> {
        it.addScoreboardTag(sBreadHangerScoreboardTag);
        it.setGravity(false);
        it.setBasePlate(false);
        it.setVisible(false);
        var e = it.getEquipment();
        e.setHelmet(new ItemStack(Material.BREAD));
      });
      breadHangers.add(new EntityTracking<>(stand));
    }
  }

  private @Nonnull ItemStack createBread() {
    if (status == Status.ACTIVE && race != null) {
      return ItemBuilder.For(Material.BREAD)
        .displayName(text("元気が出るパン", GOLD))
        .customTag(sItemTag)
        .customTag(race.getBreadId())
        .build();
    } else {
      return new ItemStack(Material.BREAD);
    }
  }

  private void tickWaves() {
    var minX = x(-48) / 16;
    var minZ = z(64) / 16;
    var maxX = x(-16) / 16;
    var maxZ = z(80) / 16;
    for (int cx = minX; cx <= maxX; cx++) {
      for (int cz = minZ; cz <= maxZ; cz++) {
        if (!world.isChunkLoaded(cx, cz)) {
          return;
        }
      }
    }
    if (!wavePrepared) {
      wavePrepared = true;
      Kill.EntitiesByScoreboardTag(world, sBubbleScoreboardTag);
      var yCandidates = new double[]{
        y(83) + 0.5,
        y(82) + 0.5,
        y(81) + 0.5
      };
      var startX = x(-12) + 0.5;
      var endX = x(-39) + 0.5;
      waves.add(new Wave(
        world, sBubbleScoreboardTag,
        startX, endX, x(-16),
        yCandidates, y(79) + 0.5
      ));
      waves.add(new Wave(
        world, sBubbleScoreboardTag,
        startX, endX, x(-25),
        yCandidates, y(79) + 0.5
      ));
      waves.add(new Wave(
        world, sBubbleScoreboardTag,
        startX, endX, x(-34),
        yCandidates, y(79) + 0.5
      ));
    }
    for (var wave : waves) {
      wave.tick();
    }
  }

  private void broadcast(Component message) {
    announcer.announce(message, delegate.relayGetAnnounceBounds());
  }

  private int x(int x) {
    return x + offset.x;
  }

  private int y(int y) {
    return y + offset.y;
  }

  private int z(int z) {
    return z + offset.z;
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x + offset.x, y + offset.y, z + offset.z);
  }
}
