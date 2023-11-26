package com.github.kbinani.holosportsfestival2023.himerace;

import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nonnull;

import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.ProductPlaceholderItem;
import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.sProductPlaceholderMaterial;

abstract class AbstractKitchenware {
  protected final int capacity;
  protected final int materialSlotFrom;
  protected final int materialSlotTo;
  protected final int productSlot;
  protected final int toolSlot;
  final @Nonnull Inventory inventory;

  AbstractKitchenware(int capacity, int materialSlotFrom, int materialSlotTo, int productSlot, int toolSlot) {
    this.capacity = capacity;
    this.materialSlotFrom = materialSlotFrom;
    this.materialSlotTo = materialSlotTo;
    this.productSlot = productSlot;
    this.toolSlot = toolSlot;

    this.inventory = createInventory();
  }

  abstract @Nonnull Inventory createInventory();

  abstract @Nonnull CookingRecipe[] getRecipes();

  final void onInventoryClick(InventoryClickEvent e) {
    if (e.isCancelled()) {
      return;
    }
    var view = e.getView();
    if (view.getTopInventory() != inventory) {
      return;
    }
    var slot = e.getRawSlot();
    var item = e.getCurrentItem();
    if (!onClickProdctSlot(e, productSlot)) {
      return;
    }
    if (item == null) {
      return;
    }
    if (materialSlotFrom <= slot && slot <= materialSlotTo) {
      // nop
    } else if (toolSlot == slot) {
      //TODO: cauldron は着火するだけ, product の生成は 7 秒後
      e.setCancelled(true);
      for (var recipe : getRecipes()) {
        if (recipe.consumeMaterialsIfPossible(inventory, materialSlotFrom, materialSlotTo, productSlot)) {
          break;
        }
      }
    } else {
      e.setCancelled(true);
    }
  }

  final void dispose() {
    this.inventory.close();
  }

  protected final boolean onClickProdctSlot(InventoryClickEvent e, int productSlot) {
    var view = e.getView();
    var top = view.getTopInventory();
    var bottom = view.getBottomInventory();
    var clicked = e.getClickedInventory();
    var slot = e.getRawSlot();
    var item = e.getCurrentItem();
    var action = e.getAction();
    if (clicked == bottom) {
      if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || action == InventoryAction.PLACE_SOME) {
        var product = top.getItem(productSlot);
        if (product == null) {
          top.setItem(productSlot, ProductPlaceholderItem());
        }
      }
      return false;
    } else if (item != null) {
      if (slot == productSlot) {
        if (item.getType() == sProductPlaceholderMaterial) {
          e.setCancelled(true);
        } else {
          if (action == InventoryAction.PICKUP_ALL) {
            view.setCursor(item);
            view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            e.setCancelled(true);
          } else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            bottom.addItem(item);
            view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            e.setCancelled(true);
          } else if (action == InventoryAction.PICKUP_HALF) {
            var remain = item.getAmount() / 2;
            var amount = item.getAmount() - remain;
            view.setCursor(item.clone().subtract(remain));
            if (remain == 0) {
              view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            } else {
              view.setItem(e.getRawSlot(), item.clone().subtract(amount));
            }
            e.setCancelled(true);
          } else {
            e.setCancelled(true);
          }
        }
        return false;
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
}
