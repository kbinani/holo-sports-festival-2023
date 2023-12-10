package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import org.bukkit.util.BoundingBox;

public interface Announcer {
  void announce(Component message, BoundingBox bounds);
}
