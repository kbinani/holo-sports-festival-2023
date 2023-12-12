package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.ItemStackExtension;
import lombok.experimental.ExtensionMethod;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.kbinani.holosportsfestival2023.himerace.stage.cook.CookStage.AddItemTag;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@ExtensionMethod({ItemStackExtension.class})
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
        text("[ベイクドポテト]", BLUE).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・ジャガイモ", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①かまどで精錬！", BLACK))
      };
      case COOKED_CHICKEN -> new Component[]{
        text("[焼き鳥]", BLUE).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・生の鶏肉", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①かまどで精錬！", BLACK))
      };
      case COOKED_BEEF -> new Component[]{
        text("[ステーキ]", BLUE).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・生の牛肉", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①かまどで精錬！", BLACK))
      };
      case COOKED_MUTTON -> new Component[]{
        text("[焼き羊肉]", BLUE).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・生の羊肉", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①かまどで精錬！", BLACK))
      };
      case COOKED_RABBIT -> new Component[]{
        text("[焼き兎肉]", BLUE).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・生の兎肉", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①かまどで精錬！", BLACK))
      };
      case MIO_HAMBERGER_STEAK -> new Component[]{
        text("[ミオしゃ特製ハンバーグ♡]", DARK_RED).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・生の牛肉", BLACK)).appendNewline()
          .append(text("・ジャガイモ", BLACK)).appendNewline()
          .append(text("・ニンジン", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①まな板で生の牛肉、ジャガイモ、ニンジンを切る！", BLACK)).appendNewline()
          .appendNewline()
          .append(text("②鉄板で牛ひき肉と切ったジャガイモと切ったニンジンを一緒に焼く！", BLACK))
      };
      // https://youtu.be/vHk29E_TIDc?t=3066
      case SUBARU_FRIED_CHICKEN -> new Component[]{
        text("[スバルの唐揚げ]", DARK_RED).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・生の鶏肉", BLACK)).appendNewline()
          .append(text("・小麦", BLACK)).appendNewline()
          .append(text("・油", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①まな板で生の鶏肉、小麦を切る！", BLACK)).appendNewline()
          .appendNewline()
          .append(text("②鍋に切った鶏肉、小麦粉、油を入れて揚げる！", BLACK))
      };
      case MIKO_PANCAKES -> new Component[]{
        text("[えりぃとパンケーキ]", DARK_RED).appendNewline()
          .appendNewline()
          .append(text("材料", BLACK)).appendNewline()
          .append(text("・小麦", BLACK)).appendNewline()
          .append(text("・卵", BLACK)).appendNewline()
          .append(text("・スイートベリー", BLACK)).appendNewline()
          .appendNewline()
          .append(text("①まな板で小麦、スイートベリーを切る！", BLACK)).appendNewline()
          .appendNewline()
          .append(text("②鉄板で小麦粉と卵を一緒に焼く！", BLACK)),
        text("[えりぃとパンケーキ]", DARK_RED).appendNewline()
          .appendNewline()
          .append(text("③盛り付け台でただのパンケーキと切ったスイートベリーを盛り付ける！", BLACK))
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
      case OIL -> ItemBuilder.For(taskItem.material)
        .amount(amount)
        .potion(PotionType.STRENGTH)
        .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
        .build();
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
        item.addCustomTag(task.getTag());
      }
    }
    return AddItemTag(item);
  }
}
