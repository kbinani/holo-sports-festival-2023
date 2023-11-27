package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.ProductPlaceholderItem;
import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.sProductPlaceholderMaterial;

abstract class AbstractKitchenware {
  protected final int capacity;
  protected final int materialSlotFrom;
  protected final int materialSlotTo;
  protected final int productSlot;
  protected final int toolSlot;
  final @Nonnull Inventory inventory;
  private @Nullable BukkitTask cooldownTimer;

  AbstractKitchenware(int capacity, int materialSlotFrom, int materialSlotTo, int productSlot, int toolSlot) {
    this.capacity = capacity;
    this.materialSlotFrom = materialSlotFrom;
    this.materialSlotTo = materialSlotTo;
    this.productSlot = productSlot;
    this.toolSlot = toolSlot;

    this.inventory = createInventory();
  }

  protected abstract @Nonnull Inventory createInventory();

  protected abstract @Nonnull Recipe[] getRecipes();

  protected @Nullable Integer getCooldownSeconds() {
    return null;
  }

  protected void onCountdown(int count) {
  }

  protected void onAllProductPickedUp() {}

  final void onInventoryClick(InventoryClickEvent e, JavaPlugin owner) {
    if (e.isCancelled()) {
      return;
    }
    var view = e.getView();
    if (view.getTopInventory() != inventory) {
      return;
    }
    var slot = e.getRawSlot();
    var item = e.getCurrentItem();
    var action = e.getAction();
    var skip = onClickProdctSlot(e, productSlot);
    if (skip != null) {
      if (skip == SkipReason.ALL_PRODUCT_PICKEDUP) {
        onAllProductPickedUp();
      }
      return;
    }
    if (item == null) {
      return;
    }
    if (materialSlotFrom <= slot && slot <= materialSlotTo) {
      // nop
    } else if (toolSlot == slot) {
      e.setCancelled(true);
      if (action != InventoryAction.PICKUP_ALL || cooldownTimer != null) {
        return;
      }
      Recipe match = null;
      for (var recipe : getRecipes()) {
        if (recipe.match(inventory, materialSlotFrom, materialSlotTo)) {
          match = recipe;
          break;
        }
      }
      if (match == null) {
        return;
      }
      var cooldown = getCooldownSeconds();
      final var product = match.consumeMaterialsIfPossible(inventory, materialSlotFrom, materialSlotTo, productSlot);
      if (cooldown == null) {
        if (product != null) {
          inventory.setItem(productSlot, product);
        }
      } else {
        final var count = new AtomicInteger(cooldown);
        this.onCountdown(cooldown);
        this.cooldownTimer = Bukkit.getScheduler().runTaskTimer(owner, () -> {
          var c = count.decrementAndGet();
          this.onCountdown(c);
          if (c == 0) {
            this.inventory.setItem(productSlot, product);
            this.cooldownTimer.cancel();
            this.cooldownTimer = null;
          }
        }, 20, 20);
      }
    } else {
      e.setCancelled(true);
    }
  }

  final void dispose() {
    this.inventory.close();
    if (cooldownTimer != null) {
      cooldownTimer.cancel();
      cooldownTimer = null;
    }
  }

  protected enum SkipReason {
    ALL_PRODUCT_PICKEDUP,
    SYSTEM,
  }

  protected final SkipReason onClickProdctSlot(InventoryClickEvent e, int productSlot) {
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
      return SkipReason.SYSTEM;
    } else if (item != null) {
      if (slot == productSlot) {
        if (item.getType() == sProductPlaceholderMaterial) {
          e.setCancelled(true);
          return SkipReason.SYSTEM;
        } else {
          if (cooldownTimer != null) {
            e.setCancelled(true);
            return SkipReason.SYSTEM;
          } else if (action == InventoryAction.PICKUP_ALL) {
            view.setCursor(item);
            view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            e.setCancelled(true);
            return SkipReason.ALL_PRODUCT_PICKEDUP;
          } else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            bottom.addItem(item);
            view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            e.setCancelled(true);
            return SkipReason.ALL_PRODUCT_PICKEDUP;
          } else if (action == InventoryAction.PICKUP_HALF) {
            var remain = item.getAmount() / 2;
            var amount = item.getAmount() - remain;
            view.setCursor(item.clone().subtract(remain));
            e.setCancelled(true);
            if (remain == 0) {
              view.setItem(e.getRawSlot(), ProductPlaceholderItem());
              return SkipReason.ALL_PRODUCT_PICKEDUP;
            } else {
              view.setItem(e.getRawSlot(), item.clone().subtract(amount));
              return SkipReason.SYSTEM;
            }
          } else {
            e.setCancelled(true);
          }
        }
        return SkipReason.SYSTEM;
      } else {
        return null;
      }
    } else {
      return SkipReason.SYSTEM;
    }
  }
}
