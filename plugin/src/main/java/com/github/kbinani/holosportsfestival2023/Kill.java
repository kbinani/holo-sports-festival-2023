package com.github.kbinani.holosportsfestival2023;

import lombok.experimental.ExtensionMethod;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

@ExtensionMethod({ItemStackExtension.class})
public class Kill {
  private Kill() {

  }

  public static void EntitiesByType(World world, BoundingBox box, EntityType type) {
    world.getNearbyEntities(box, it -> it.getType() == type).forEach(Kill::Do);
  }

  public static void EntitiesByScoreboardTag(World world, BoundingBox box, String scoreboardTag) {
    world.getNearbyEntities(box, it -> ShouldKill(it, scoreboardTag)).forEach(Kill::Do);
  }

  public static void EntitiesByScoreboardTag(World world, String scoreboardTag) {
    world.getEntities().stream().filter(it -> ShouldKill(it, scoreboardTag)).forEach(Kill::Do);
  }

  private static boolean ShouldKill(Entity entity, String scoreboardTag) {
    if (entity.getScoreboardTags().contains(scoreboardTag)) {
      return true;
    }
    if (entity instanceof Item item) {
      var stack = item.getItemStack();
      return stack.hasCustomTag(scoreboardTag);
    } else {
      return false;
    }
  }

  private static void Do(Entity entity) {
    if (entity instanceof Player player) {
      player.setHealth(0);
    } else {
      entity.remove();
    }
  }
}
