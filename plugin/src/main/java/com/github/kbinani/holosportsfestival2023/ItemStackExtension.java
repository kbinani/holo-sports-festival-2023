package com.github.kbinani.holosportsfestival2023;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemStackExtension {
  public static void addCustomTag(ItemStack item, String tag) {
    item.editMeta(ItemMeta.class, it -> {
      var store = it.getPersistentDataContainer();
      store.set(NamespacedKey.minecraft(tag), PersistentDataType.BYTE, (byte) 1);
    });
  }

  public static boolean hasCustomTag(ItemStack item, String tag) {
    var meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    var store = meta.getPersistentDataContainer();
    return store.has(NamespacedKey.minecraft(tag), PersistentDataType.BYTE);
  }
}
