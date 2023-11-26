package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.ItemTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;
import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.AddItemTag;

public enum Task {
  BAKED_POTATO(TaskItem.BAKED_POTATO, true),
  COOKED_CHICKEN(TaskItem.COOKED_CHICKEN, true),
  COOKED_BEEF(TaskItem.COOKED_BEEF, true),
  COOKED_MUTTON(TaskItem.COOKED_MUTTON, true),
  COOKED_RABBIT(TaskItem.COOKED_RABBIT, true),

  MIO_HAMBERGER_STEAK(TaskItem.MIO_HAMBURGER_STEAK, false),
  MIKO_PANCAKES(TaskItem.MIKO_PANCAKES, false),
  SUBARU_FRIED_CHICKEN(TaskItem.SUBARU_FRIED_CHICKEN, false);

  static final String easyTaskTag = "hololive_sports_festival_2023_himerace_cooking_easy";
  static final String difficultTaskTag = "hololive_sports_festival_2023_himerace_cooking_difficult";

  final TaskItem item;
  final boolean isEasy;

  Task(TaskItem item, boolean isEasy) {
    this.item = item;
    this.isEasy = isEasy;
  }

  String getTag() {
    if (isEasy) {
      return easyTaskTag;
    } else {
      return difficultTaskTag;
    }
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

  static Task selectRandomlyEasyTask() {
    var tasks = Arrays.stream(values()).filter(it -> it.isEasy).toList();
    var index = ThreadLocalRandom.current().nextInt(tasks.size());
    return tasks.get(index);
  }

  static Task selectRandomlyDifficultTask() {
    var tasks = Arrays.stream(values()).filter(it -> !it.isEasy).toList();
    var index = ThreadLocalRandom.current().nextInt(tasks.size());
    return tasks.get(index);
  }

  public static @Nonnull ItemStack ToItem(TaskItem taskItem) {
    return ToItem(taskItem, 1);
  }

  public static @Nonnull ItemStack ToItem(TaskItem taskItem, int amount) {
    var item = switch (taskItem) {
      case OIL -> AddItemTag(
        ItemBuilder.For(Material.POTION)
          .amount(amount)
          .potion(PotionType.STRENGTH)
          .displayName(Text("油 / Oil"))
          .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
          .build()
      );
      default -> new ItemStack(taskItem.material, amount);
    };
    item.editMeta(ItemMeta.class, it -> {
      if (taskItem.specialItemName != null) {
        it.displayName(taskItem.specialItemName);
      }
      if (taskItem.customModelData != null) {
        it.setCustomModelData(taskItem.customModelData);
      }
    });
    for (var task : Task.values()) {
      if (task.item == taskItem) {
        ItemTag.AddByte(item, task.getTag());
      }
    }
    return AddItemTag(item);
  }
}

