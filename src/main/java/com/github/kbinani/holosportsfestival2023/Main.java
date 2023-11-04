package com.github.kbinani.holosportsfestival2023;

import com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class Main extends JavaPlugin implements Listener {
  private World world;
  private List<MiniGame> miniGames;
  private boolean isLevelsReady = false;

  @Override
  public void onEnable() {
    Optional<World> overworld = getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
    if (overworld.isEmpty()) {
      getLogger().log(java.util.logging.Level.SEVERE, "server should have at least one overworld dimension");
      setEnabled(false);
      return;
    }
    world = overworld.get();

    List<String> reasons = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Boolean mobGriefing = world.getGameRuleValue(GameRule.MOB_GRIEFING);
    Boolean keepInventory = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
    Boolean showDeathMessages = world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
    Boolean announceAdvancements = world.getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS);
    if (mobGriefing != null && mobGriefing) {
      reasons.add("mobGriefing gamerule is set to true");
    }
    if (keepInventory != null && !keepInventory) {
      reasons.add("keepInventory gamerule is set to false");
    }
    if (showDeathMessages != null && showDeathMessages) {
      warnings.add("showDeathMessages gamerule is set to true");
    }
    if (announceAdvancements != null && announceAdvancements) {
      warnings.add("announceAdvancements gamerule is set to true");
    }
    if (world.getDifficulty() == Difficulty.PEACEFUL) {
      reasons.add("the \"himerace\" mini game is not playable as the difficulty is set to peaceful");
    }
    if (!world.getPVP()) {
      reasons.add("pvp is set to false");
    }
    if (!reasons.isEmpty()) {
      getLogger().log(java.util.logging.Level.SEVERE, "Disabling the plugin because:");
      for (String reason : reasons) {
        getLogger().log(java.util.logging.Level.SEVERE, "  " + reason);
      }
      setEnabled(false);
      return;
    }
    if (!warnings.isEmpty()) {
      for (String warning : warnings) {
        getLogger().warning(warning);
      }
    }

    PluginManager pluginManager = getServer().getPluginManager();
    pluginManager.registerEvents(this, this);

    miniGames = new ArrayList<>();
    miniGames.add(new HimeraceEventListener(world, this, new int[]{0, 1, 2}));
    for (var miniGame : miniGames) {
      pluginManager.registerEvents(miniGame, this);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    if (!isLevelsReady) {
      isLevelsReady = true;
      for (var miniGame : miniGames) {
        miniGame.miniGameReset();
      }
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onCreatureSpawn(CreatureSpawnEvent e) {
    if (e.isCancelled()) {
      return;
    }
    if (e.getEntity().getWorld() != world) {
      return;
    }
    switch (e.getSpawnReason()) {
      case NATURAL:
      case VILLAGE_INVASION:
      case BUILD_WITHER:
      case BUILD_IRONGOLEM:
      case BUILD_SNOWMAN:
      case SPAWNER_EGG:
      case SPAWNER:
        e.setCancelled(true);
        break;
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onBlockForm(BlockFormEvent e) {
    if (e.isCancelled()) {
      return;
    }
    var from = e.getBlock();
    if (from.getType() != Material.WHITE_CONCRETE_POWDER) {
      return;
    }
    e.setCancelled(true);
  }
}
