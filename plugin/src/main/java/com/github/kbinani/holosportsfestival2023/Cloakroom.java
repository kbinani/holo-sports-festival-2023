package com.github.kbinani.holosportsfestival2023;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cloakroom {
  public static final @Nonnull Cloakroom shared = new Cloakroom();

  private final Map<UUID, ItemStack[]> storage = new HashMap<>();

  private Cloakroom() {
  }

  public boolean store(Player player) {
    var id = player.getUniqueId();
    if (storage.containsKey(id)) {
      return false;
    }
    var inventory = player.getInventory();
    storage.put(id, inventory.getContents());
    inventory.clear();
    return true;
  }

  public boolean restore(Player player) {
    var id = player.getUniqueId();
    var stored = storage.get(id);
    if (stored == null) {
      return false;
    }
    storage.remove(id);
    var inventory = player.getInventory();
    inventory.setContents(stored);
    return true;
  }
}
