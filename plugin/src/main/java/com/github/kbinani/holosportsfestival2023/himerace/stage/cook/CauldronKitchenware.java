package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.ProductPlaceholderItem;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

class CauldronKitchenware extends AbstractKitchenware {
  CauldronKitchenware() {
    super(54, 30, 32, 14, 12);
  }

  @Override
  protected @Nonnull Inventory createInventory() {
    var inventory = Bukkit.createInventory(null, capacity, text("鍋", GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var fas = ItemBuilder.For(Material.FLINT_AND_STEEL).displayName(text("調理する！", GREEN)).build();
    final var ob = ProductPlaceholderItem();
    final var wsgp = ItemBuilder.For(Material.WHITE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var a = new ItemStack(Material.AIR);
    final var c = new ItemStack(Material.CAMPFIRE);
    c.editMeta(ItemMeta.class, it -> {
      it.setCustomModelData(1);
      it.displayName(text("🔥🔥🔥", DARK_GRAY));
    });
    inventory.setContents(new ItemStack[]{
      bsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, bsgp,
      bsgp, gsgp, gsgp, fas, gsgp, ob, gsgp, gsgp, bsgp,
      bsgp, gsgp, wsgp, gsgp, gsgp, gsgp, wsgp, gsgp, bsgp,
      bsgp, gsgp, wsgp, a, a, a, wsgp, gsgp, bsgp,
      bsgp, gsgp, wsgp, wsgp, wsgp, wsgp, wsgp, gsgp, bsgp,
      bsgp, gsgp, c, c, c, c, c, gsgp, bsgp
    });
    return inventory;
  }

  @Override
  protected @Nonnull Recipe[] getRecipes() {
    return new Recipe[]{
      // https://youtu.be/TEqf-g0WlKY?t=9918
      // 7秒: "油" + "切った生の鶏肉" + "小麦粉" -> Text("スバルの唐揚げ / Subaru's Fried Chicken", NamedTextColor.GOLD)
      new Recipe(new TaskItem[]{TaskItem.OIL, TaskItem.CHOPPED_CHICKEN, TaskItem.FLOUR}, TaskItem.SUBARU_FRIED_CHICKEN),
    };
  }

  @Override
  protected @Nullable Integer getCooldownSeconds() {
    return 7;
  }

  @Override
  protected void onCountdown(int count) {
    var red = ItemBuilder.For(Material.RED_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    var orange = ItemBuilder.For(Material.ORANGE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    for (var i = 0; i < Math.min(count - 1, 6); i++) {
      inventory.setItem(i * 9, orange);
      inventory.setItem(i * 9 + 8, orange);
    }
    for (var i = Math.max(0, count - 1); i < 6; i++) {
      inventory.setItem(i * 9, red);
      inventory.setItem(i * 9 + 8, red);
    }
    var campfire = new ItemStack(Material.CAMPFIRE);
    campfire.editMeta(ItemMeta.class, it -> {
      it.setCustomModelData(count == 0 ? 1 : 2);
      it.displayName(text("🔥🔥🔥", count == 0 ? DARK_GRAY : RED));
    });
    for (var i = 47; i <= 51; i++) {
      inventory.setItem(i, campfire);
    }
  }

  @Override
  protected void onAllProductPickedUp() {
    var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    for (var i = 0; i < 6; i++) {
      inventory.setItem(i * 9, bsgp);
      inventory.setItem(i * 9 + 8, bsgp);
    }
  }
}
