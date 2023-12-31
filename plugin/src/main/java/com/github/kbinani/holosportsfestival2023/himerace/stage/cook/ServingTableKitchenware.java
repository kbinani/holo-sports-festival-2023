package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.ProductPlaceholderItem;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

class ServingTableKitchenware extends AbstractKitchenware {
  ServingTableKitchenware() {
    super(54, 29, 33, 14, 12);
  }

  @Override
  protected @Nonnull Inventory createInventory() {
    var inventory = Bukkit.createInventory(null, capacity, text("盛り付け台", GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var a = new ItemStack(Material.AIR);
    final var b = ItemBuilder.For(Material.BOWL).displayName(text("盛り付ける！", GREEN)).build();
    final var ob = ProductPlaceholderItem();
    //NOTE: この orange_stained_glass_pane は displayName が設定されておらずアイテム名が見える: https://youtu.be/MKcNzz21P8g?t=9740
    final var osgp = ItemBuilder.For(Material.ORANGE_STAINED_GLASS_PANE).build();
    inventory.setContents(new ItemStack[]{
      bsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, bsgp,
      bsgp, gsgp, gsgp, b, gsgp, ob, gsgp, gsgp, bsgp,
      bsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, bsgp,
      bsgp, gsgp, a, a, a, a, a, gsgp, bsgp,
      bsgp, gsgp, osgp, osgp, osgp, osgp, osgp, gsgp, bsgp,
      bsgp, gsgp, osgp, osgp, osgp, osgp, osgp, gsgp, bsgp
    });
    return inventory;
  }

  @Override
  protected @Nonnull Recipe[] getRecipes() {
    return new Recipe[]{
      new Recipe(new TaskItem[]{TaskItem.PANCAKES, TaskItem.CUT_SWEET_BERRIES}, TaskItem.MIKO_PANCAKES),
    };
  }
}
