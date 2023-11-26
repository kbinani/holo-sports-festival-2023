package com.github.kbinani.holosportsfestival2023.himerace;

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
import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.ProductPlaceholderItem;

class CauldronKitchenware extends AbstractKitchenware {
  CauldronKitchenware() {
    super(54, 30, 32, 14, 12);
  }

  @Override
  @Nonnull Inventory createInventory() {
    var inventory = Bukkit.createInventory(null, capacity, Text("鍋", NamedTextColor.GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var fas = ItemBuilder.For(Material.FLINT_AND_STEEL).displayName(Text("調理する！", NamedTextColor.GREEN)).build();
    final var ob = ProductPlaceholderItem();
    final var wsgp = ItemBuilder.For(Material.WHITE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var a = new ItemStack(Material.AIR);
    final var c = new ItemStack(Material.CAMPFIRE);
    c.editMeta(ItemMeta.class, it -> {
      it.setCustomModelData(1);
      //TODO: 着火した時は NamedTextColor.RED
      it.displayName(Text("🔥🔥🔥", NamedTextColor.DARK_GRAY));
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
  @Nonnull CookingRecipe[] getRecipes() {
    return new CookingRecipe[]{
      // https://youtu.be/TEqf-g0WlKY?t=9918
      // 7秒: "油" + "切った生の鶏肉" + "小麦粉" -> Text("スバルの唐揚げ / Subaru's Fried Chicken", NamedTextColor.GOLD)
      new CookingRecipe(new CookingTaskItem[]{CookingTaskItem.OIL, CookingTaskItem.CHOPPED_CHICKEN, CookingTaskItem.FLOUR}, CookingTaskItem.SUBARU_FRIED_CHICKEN),
    };
  }
}
