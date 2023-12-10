package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;

import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.ProductPlaceholderItem;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

class HotPlateKitchenware extends AbstractKitchenware {
  HotPlateKitchenware() {
    super(54, 29, 33, 14, 12);
  }

  @Override
  protected @Nonnull Inventory createInventory() {
    var inventory = Bukkit.createInventory(null, capacity, text("鉄板", GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var fas = ItemBuilder.For(Material.FLINT_AND_STEEL).displayName(text("材料を焼く！", GREEN)).build();
    final var ob = ProductPlaceholderItem();
    final var a = new ItemStack(Material.AIR);
    final var wsgp = ItemBuilder.For(Material.WHITE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var c = new ItemStack(Material.CAMPFIRE);
    c.editMeta(ItemMeta.class, it -> {
      //NOTE: 鉄板の焚き火は着火操作必要無いぽい: https://youtu.be/ZNGqqCothRc?t=9815
      it.displayName(text("🔥🔥🔥", RED));
      it.setCustomModelData(2);
    });
    inventory.setContents(new ItemStack[]{
      bsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, bsgp,
      bsgp, gsgp, gsgp, fas, gsgp, ob, gsgp, gsgp, bsgp,
      bsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, bsgp,
      bsgp, gsgp, a, a, a, a, a, gsgp, bsgp,
      bsgp, gsgp, wsgp, wsgp, wsgp, wsgp, wsgp, gsgp, bsgp,
      bsgp, gsgp, c, c, c, c, c, gsgp, bsgp
    });
    return inventory;
  }

  @Override
  protected @Nonnull Recipe[] getRecipes() {
    return new Recipe[]{
      new Recipe(new TaskItem[]{TaskItem.FLOUR, TaskItem.EGG}, TaskItem.PANCAKES),
      new Recipe(new TaskItem[]{TaskItem.RAW_GROUND_BEEF, TaskItem.CUT_POTATO, TaskItem.CUT_CARROT}, TaskItem.MIO_HAMBURGER_STEAK),
    };
  }
}
