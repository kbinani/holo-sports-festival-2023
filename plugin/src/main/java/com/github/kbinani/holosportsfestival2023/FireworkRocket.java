package com.github.kbinani.holosportsfestival2023;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class FireworkRocket {
  private FireworkRocket() {
  }

  public static void Launch(@Nonnull World world, Point3i p, Color[] colors, Color[] fadeColors, int lifeTime, int type, boolean flicker, boolean trail) {
    Launch(world, p.x + 0.5, p.y + 0.5, p.z + 0.5, colors, fadeColors, lifeTime, type, flicker, trail);
  }

  public static void Launch(@Nonnull World world, double x, double y, double z, Color[] colors, Color[] fadeColors, int lifeTime, int type, boolean flicker, boolean trail) {
    FireworkEffect.Type t = FireworkEffect.Type.BALL;
    switch (type) {
      case 0:
        t = FireworkEffect.Type.BALL;
        break;
      case 1:
        t = FireworkEffect.Type.BALL_LARGE;
        break;
      case 4:
        t = FireworkEffect.Type.BURST;
        break;
    }
    final FireworkEffect.Type effectType = t;
    var colorList = Arrays.stream(colors).toList();
    var fadeColorList = Arrays.stream(fadeColors).toList();
    world.spawn(new Location(world, x, y, z), Firework.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      var meta = it.getFireworkMeta();
      var effect = FireworkEffect.builder()
        .flicker(flicker)
        .trail(trail)
        .with(effectType)
        .withColor(colorList)
        .withFade(fadeColorList)
        .build();
      meta.addEffect(effect);
      it.setFireworkMeta(meta);
      it.setTicksToDetonate(lifeTime);
    });
  }
}
