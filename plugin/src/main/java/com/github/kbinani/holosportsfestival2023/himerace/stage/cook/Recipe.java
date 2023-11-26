package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.sProductPlaceholderMaterial;

record Recipe(TaskItem[] materials, TaskItem product) {
  private record RecipeMatch(ItemStack item, int slot) {
  }

  boolean match(Inventory inventory, int materialSlotFrom, int materialSlotTo) {
    return getMatches(inventory, materialSlotFrom, materialSlotTo) != null;
  }

  @Nullable ItemStack consumeMaterialsIfPossible(Inventory inventory, int materialSlotFrom, int materialSlotTo, int productSlot) {
    var result = getMatches(inventory, materialSlotFrom, materialSlotTo);
    if (result == null) {
      return null;
    }
    for (var match : result) {
      inventory.setItem(match.slot, match.item.subtract());
    }
    var product = inventory.getItem(productSlot);
    if (product != null && product.isSimilar(this.product.toItem())) {
      return product.add();
    } else if (product == null || product.getType() == sProductPlaceholderMaterial) {
      return this.product.toItem();
    } else {
      return null;
    }
  }

  private ArrayList<RecipeMatch> getMatches(Inventory inventory, int materialSlotFrom, int materialSlotTo) {
    var matches = new int[materials.length];
    var expected = new ItemStack[materials.length];
    for (int j = 0; j < materials.length; j++) {
      matches[j] = -1;
      expected[j] = materials[j].toItem();
    }
    for (int i = materialSlotFrom; i <= materialSlotTo; i++) {
      var item = inventory.getItem(i);
      if (item == null) {
        continue;
      }
      for (int j = 0; j < materials.length; j++) {
        if (matches[j] < 0 && item.isSimilar(expected[j])) {
          matches[j] = i;
          break;
        }
      }
    }

    var result = new ArrayList<RecipeMatch>();
    for (int j = 0; j < materials.length; j++) {
      if (matches[j] < 0) {
        return null;
      }
      var item = inventory.getItem(matches[j]);
      if (item == null) {
        return null;
      }
      result.add(new RecipeMatch(item, matches[j]));
    }
    return result;
  }
}
