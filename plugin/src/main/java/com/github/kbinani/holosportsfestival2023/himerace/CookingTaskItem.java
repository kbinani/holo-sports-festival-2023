package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;
import static com.github.kbinani.holosportsfestival2023.himerace.CookStage.AddItemTag;

enum CookingTaskItem {
  EMERALD(Material.EMERALD),
  COAL(Material.COAL),

  POTATO(Material.POTATO),
  CHICKEN(Material.CHICKEN),
  BEEF(Material.BEEF),
  MUTTON(Material.MUTTON),
  RABBIT(Material.RABBIT),
  CARROT(Material.CARROT),
  WHEAT(Material.WHEAT),
  OIL(Material.POTION),
  EGG(Material.EGG),
  SWEET_BERRIES(Material.SWEET_BERRIES),

  BAKED_POTATO(Material.BAKED_POTATO),
  COOKED_CHICKEN(Material.COOKED_CHICKEN),
  COOKED_BEEF(Material.COOKED_BEEF),
  COOKED_MUTTON(Material.COOKED_MUTTON),
  COOKED_RABBIT(Material.COOKED_RABBIT),

  CUT_POTATO(Material.POTATO, Text("切ったジャガイモ / Cut Potato", NamedTextColor.WHITE), 1),
  CHOPPED_CHICKEN(Material.CHICKEN, Text("切った生の鶏肉 / Chopped Chicken", NamedTextColor.WHITE), 1),
  RAW_GROUND_BEEF(Material.BEEF, Text("生の牛ひき肉 / Raw Ground Beef", NamedTextColor.WHITE), 1),
  CUT_CARROT(Material.CARROT, Text("切ったニンジン / Cut Carrot", NamedTextColor.WHITE), 1),
  FLOUR(Material.WHEAT, Text("小麦粉 / Flour", NamedTextColor.WHITE), 1),
  CUT_SWEET_BERRIES(Material.SWEET_BERRIES, Text("切ったスイートベリー / Cut Sweet Berries", NamedTextColor.WHITE), 1),

  PANCAKES(Material.CAKE, Text("ただのパンケーキ / Pancakes", NamedTextColor.WHITE), 1),

  MIO_HAMBURGER_STEAK(Material.COOKED_BEEF, Text("ミオしゃ特製ハンバーグ♡ / Mio's Hamburger Steak", NamedTextColor.GOLD), 1),
  SUBARU_FRIED_CHICKEN(Material.COOKED_CHICKEN, Text("スバルの唐揚げ / Subaru's Fried Chicken", NamedTextColor.GOLD), 1),
  MIKO_PANCAKES(Material.CAKE, Text("えりぃとパンケーキ / Miko's Pancakes", NamedTextColor.GOLD), 2);

  final Material material;
  final @Nullable Component specialName;
  final @Nullable Integer customModelData;

  CookingTaskItem(Material material, @Nullable Component specialName, @Nullable Integer customModelData) {
    this.material = material;
    this.specialName = specialName;
    this.customModelData = customModelData;
  }

  CookingTaskItem(Material material) {
    this(material, null, null);
  }

  @Nonnull
  ItemStack toItem() {
    var item = switch (this) {
      case OIL -> AddItemTag(
        ItemBuilder.For(Material.POTION)
          .potion(PotionType.STRENGTH)
          .displayName(Text("油 / Oil"))
          .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
          .build()
      );
      default -> new ItemStack(this.material);
    };
    item.editMeta(ItemMeta.class, it -> {
      if (this.specialName != null) {
        it.displayName(this.specialName);
      }
      if (this.customModelData != null) {
        it.setCustomModelData(this.customModelData);
      }
    });
    return AddItemTag(item);
  }
}

