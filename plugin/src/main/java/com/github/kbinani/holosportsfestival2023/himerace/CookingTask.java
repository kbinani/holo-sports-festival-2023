package com.github.kbinani.holosportsfestival2023.himerace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;

enum CookingTask {
  BAKED_POTATO(Material.BAKED_POTATO, "hololive_sports_festival_2023_himerace_cooking_easy"),
  COOKED_CHICKEN(Material.COOKED_CHICKEN, "hololive_sports_festival_2023_himerace_cooking_easy"),
  COOKED_BEEF(Material.COOKED_BEEF, "hololive_sports_festival_2023_himerace_cooking_easy"),
  COOKED_MUTTON(Material.COOKED_MUTTON, "hololive_sports_festival_2023_himerace_cooking_easy"),
  COOKED_RABBIT(Material.COOKED_RABBIT, "hololive_sports_festival_2023_himerace_cooking_easy"),

  MIO_HAMBERGER_STEAK(Material.COOKED_BEEF, "hololive_sports_festival_2023_himerace_cooking_difficult"),
  MIKO_PANCAKES(Material.PUMPKIN_PIE, "hololive_sports_festival_2023_himerace_cooking_difficult"),
  SUBARU_FRIED_CHICKEN(Material.COOKED_CHICKEN, "hololive_sports_festival_2023_himerace_cooking_difficult");

  final Material material;
  final String tag;

  CookingTask(Material material, String tag) {
    this.material = material;
    this.tag = tag;
  }

  Component[] getRecipePageJp() {
    return switch (this) {
      // https://youtu.be/aca8Oy9v8tQ?t=9795
      case BAKED_POTATO -> new Component[]{
        Text("[ベイクドポテト]", NamedTextColor.BLUE).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・ジャガイモ", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①かまどで精錬！", NamedTextColor.BLACK))
      };
      case COOKED_CHICKEN -> new Component[]{
        Text("[焼き鳥]", NamedTextColor.BLUE).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・生の鶏肉", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①かまどで精錬！", NamedTextColor.BLACK))
      };
      case COOKED_BEEF -> new Component[]{
        Text("[ステーキ]", NamedTextColor.BLUE).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・生の牛肉", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①かまどで精錬！", NamedTextColor.BLACK))
      };
      case COOKED_MUTTON -> new Component[]{
        Text("[焼き羊肉]", NamedTextColor.BLUE).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・生の羊肉", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①かまどで精錬！", NamedTextColor.BLACK))
      };
      case COOKED_RABBIT -> new Component[]{
        Text("[焼き兎肉]", NamedTextColor.BLUE).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・生の兎肉", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①かまどで精錬！", NamedTextColor.BLACK))
      };
      case MIO_HAMBERGER_STEAK -> new Component[]{
        Text("[ミオしゃ特製ハンバーグ♡]", NamedTextColor.DARK_RED).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・生の牛肉", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・ジャガイモ", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・ニンジン", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①まな板で生の牛肉、ジャガイモ、ニンジンを切る！", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("②鉄板で牛ひき肉と切ったジャガイモと切ったニンジンを一緒に焼く！", NamedTextColor.BLACK))
      };
      // https://youtu.be/vHk29E_TIDc?t=3066
      case SUBARU_FRIED_CHICKEN -> new Component[]{
        Text("[スバルの唐揚げ]", NamedTextColor.DARK_RED).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・生の鶏肉", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・小麦", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・油", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①まな板で生の鶏肉、小麦を切る！", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("②鍋に切った鶏肉、小麦粉、油を入れて揚げる！", NamedTextColor.BLACK))
      };
      case MIKO_PANCAKES -> new Component[]{
        Text("[えりぃとパンケーキ]", NamedTextColor.DARK_RED).appendNewline()
          .appendNewline()
          .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・小麦", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・卵", NamedTextColor.BLACK)).appendNewline()
          .append(Text("・スイートベリー", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("①まな板で小麦、スイートベリーを切る！", NamedTextColor.BLACK)).appendNewline()
          .appendNewline()
          .append(Text("②鉄板で小麦粉と卵を一緒に焼く！", NamedTextColor.BLACK)),
        Text("[えりぃとパンケーキ]", NamedTextColor.DARK_RED).appendNewline()
          .appendNewline()
          .append(Text("③盛り付け台でただのパンケーキと切ったスイートベリーを盛り付ける！", NamedTextColor.BLACK))
      };
    };
  }

  Component[] getRecipePageEn() {
    //TODO: 英訳したものになっているはずだけど一旦日本語版と同じにしてある
    return getRecipePageJp();
  }
}

