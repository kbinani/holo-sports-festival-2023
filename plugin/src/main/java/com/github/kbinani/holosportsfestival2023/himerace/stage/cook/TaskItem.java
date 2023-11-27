package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import javax.annotation.Nullable;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public enum TaskItem {
  EMERALD(Material.EMERALD),
  COAL(Material.COAL),

  POTATO(Material.POTATO),
  CHICKEN(Material.CHICKEN),
  BEEF(Material.BEEF),
  MUTTON(Material.MUTTON),
  RABBIT(Material.RABBIT),
  CARROT(Material.CARROT),
  WHEAT(Material.WHEAT),
  OIL(Material.POTION, null, text("油 / Oil", WHITE), null),
  EGG(Material.EGG),
  SWEET_BERRIES(Material.SWEET_BERRIES),

  BAKED_POTATO(Material.BAKED_POTATO, null, text("ベイクドポテト / Baked Potato"), null),
  COOKED_CHICKEN(Material.COOKED_CHICKEN, null, text("焼き鳥 / Cooked Chicken"), null),
  COOKED_BEEF(Material.COOKED_BEEF, null, text("ステーキ / Cooked Beef"), null),
  COOKED_MUTTON(Material.COOKED_MUTTON, null, text("焼き羊肉 / Baked Mutton"), null),
  COOKED_RABBIT(Material.COOKED_RABBIT, null, text("焼き兎肉 / Cooked Rabbit"), null),

  CUT_POTATO(Material.POTATO, text("切ったジャガイモ / Cut Potato"), null, 1),
  CHOPPED_CHICKEN(Material.CHICKEN, text("切った生の鶏肉 / Chopped Chicken"), null, 1),
  RAW_GROUND_BEEF(Material.BEEF, text("生の牛ひき肉 / Raw Ground Beef"), null, 1),
  CUT_CARROT(Material.CARROT, text("切ったニンジン / Cut Carrot"), null, 1),
  FLOUR(Material.WHEAT, text("小麦粉 / Flour"), null, 1),
  CUT_SWEET_BERRIES(Material.SWEET_BERRIES, text("切ったスイートベリー / Cut Sweet Berries"), null, 1),

  PANCAKES(Material.PUMPKIN_PIE, text("ただのパンケーキ / Pancakes", WHITE), null, 1),

  MIO_HAMBURGER_STEAK(Material.COOKED_BEEF, text("ミオしゃ特製ハンバーグ♡ / Mio's Hamburger Steak", GOLD), null, 1),
  SUBARU_FRIED_CHICKEN(Material.COOKED_CHICKEN, text("スバルの唐揚げ / Subaru's Fried Chicken", GOLD), null, 1),
  MIKO_PANCAKES(Material.PUMPKIN_PIE, text("えりぃとパンケーキ / Miko's Pancakes", GOLD), null, 2);

  final Material material;
  final @Nullable Component specialItemName;
  final @Nullable Component description;
  final @Nullable Integer customModelData;

  TaskItem(Material material, @Nullable Component specialItemName, @Nullable Component description, @Nullable Integer customModelData) {
    this.material = material;
    this.specialItemName = specialItemName;
    this.description = description == null ? specialItemName : description;
    this.customModelData = customModelData;
  }

  TaskItem(Material material) {
    this(material, null, null, null);
  }

  Component getDescription() {
    if (description == null) {
      return Component.translatable(material.translationKey());
    } else {
      return description;
    }
  }
}
