package com.github.kbinani.holosportsfestival2023.himerace;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.sProductPlaceholderMaterial;

record CookingRecipe(CookingTaskItem[] materials, CookingTaskItem product) {
  boolean consumeMaterialsIfPossible(Inventory inventory, int materialSlotFrom, int materialSlotTo, int productSlot) {
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

    record RecipeMatch(ItemStack item, int slot) {}
    var result = new ArrayList<RecipeMatch>();
    for (int j = 0; j < materials.length; j++) {
      if (matches[j] < 0) {
        return false;
      }
      var item = inventory.getItem(matches[j]);
      if (item == null) {
        return false;
      }
      result.add(new RecipeMatch(item, matches[j]));
    }
    for (var match : result) {
      inventory.setItem(match.slot, match.item.subtract());
    }
    var product = inventory.getItem(productSlot);
    if (product != null && product.isSimilar(this.product.toItem())) {
      inventory.setItem(productSlot, product.add());
    } else if (product == null || product.getType() == sProductPlaceholderMaterial) {
      inventory.setItem(productSlot, this.product.toItem());
    }
    return true;
  }
}
