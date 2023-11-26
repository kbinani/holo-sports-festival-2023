package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;
import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.ProductPlaceholderItem;

class HotPlateKitchenware extends AbstractKitchenware {
  HotPlateKitchenware() {
    super(54, 29, 33, 14, 12);
  }

  @Override
  protected @Nonnull Inventory createInventory() {
    var inventory = Bukkit.createInventory(null, capacity, Text("鉄板", NamedTextColor.GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var fas = ItemBuilder.For(Material.FLINT_AND_STEEL).displayName(Text("材料を焼く！", NamedTextColor.GREEN)).build();
    final var ob = ProductPlaceholderItem();
    final var a = new ItemStack(Material.AIR);
    final var wsgp = ItemBuilder.For(Material.WHITE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var c = new ItemStack(Material.CAMPFIRE);
    c.editMeta(ItemMeta.class, it -> {
      //NOTE: 鉄板の焚き火は着火操作必要無いぽい: https://youtu.be/ZNGqqCothRc?t=9815
      it.displayName(Text("🔥🔥🔥", NamedTextColor.RED));
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
  protected @Nonnull CookingRecipe[] getRecipes() {
    return new CookingRecipe[]{
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.FLOUR, CookingTaskItem.EGG}, CookingTaskItem.PANCAKES),
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.RAW_GROUND_BEEF, CookingTaskItem.CUT_POTATO, CookingTaskItem.CUT_CARROT}, CookingTaskItem.MIO_HAMBURGER_STEAK),
    };
  }
}