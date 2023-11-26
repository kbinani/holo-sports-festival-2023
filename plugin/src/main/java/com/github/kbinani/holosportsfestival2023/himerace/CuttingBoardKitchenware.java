package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;
import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.ProductPlaceholderItem;

class CuttingBoardKitchenware extends AbstractKitchenware {
  CuttingBoardKitchenware() {
    super(36, 11, 11, 15, 13);
  }

  @Override
  @Nonnull Inventory createInventory() {
    var inventory = Bukkit.createInventory(null, capacity, Text("まな板", NamedTextColor.GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var a = new ItemStack(Material.AIR);
    final var ia = ItemBuilder.For(Material.IRON_AXE).displayName(Text("材料を切る！", NamedTextColor.GREEN)).build();
    final var ob = ProductPlaceholderItem();
    final var osgp = ItemBuilder.For(Material.ORANGE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    inventory.setContents(new ItemStack[]{
      bsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, bsgp,
      bsgp, gsgp, a, gsgp, ia, gsgp, ob, gsgp, bsgp,
      bsgp, gsgp, osgp, osgp, osgp, osgp, osgp, gsgp, bsgp,
      bsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, gsgp, bsgp,
    });
    return inventory;
  }

  @Override
  @Nonnull CookingRecipe[] getRecipes() {
    return new CookingRecipe[]{
      // https://youtu.be/ZNGqqCothRc?t=9807
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.POTATO}, CookingTaskItem.CUT_POTATO),
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.CARROT}, CookingTaskItem.CUT_CARROT),
      // https://youtu.be/ls3kb0qhT4E?t=9813
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.BEEF}, CookingTaskItem.RAW_GROUND_BEEF),
      // https://youtu.be/yMpj50YZHec?t=9817
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.WHEAT}, CookingTaskItem.FLOUR),
      // https://youtu.be/MKcNzz21P8g?t=9738
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.CHICKEN}, CookingTaskItem.CHOPPED_CHICKEN),
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.SWEET_BERRIES}, CookingTaskItem.CUT_SWEET_BERRIES),
    };
  }
}
