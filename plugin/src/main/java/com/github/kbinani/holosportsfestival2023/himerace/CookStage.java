package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;

class CookStage extends AbstractStage {
  // 本番:
  //   赤組:
  //     姫: 常闇トワ https://youtu.be/0zFjBmflulU?t=9879
  //     騎士: 火威青 https://youtu.be/yMpj50YZHec?t=9808
  //     騎士: 紫咲シオン (配信枠無し)
  //     騎士: FUWAMOCO https://youtu.be/QBMF6LN1QyU?t=9855
  //     お題: ベイクドポテト / えりぃとパンケーキ
  //   白組:
  //     姫: 天音かなた https://youtu.be/aca8Oy9v8tQ?t=9783
  //     騎士: AZKi https://youtu.be/ls3kb0qhT4E?t=9777
  //     騎士: 風真いろは https://youtu.be/ZNGqqCothRc?t=9779
  //     騎士: Nanashi Mumei https://youtu.be/XwN95bpEaX0?t=9893
  //     お題: 焼き羊肉, ミオしゃ特製ハンバーグ♡
  //   黄組:
  //     姫: IRyS https://youtu.be/f3cUeNF_HwQ?t=9757
  //     騎士: 一条莉々華 https://youtu.be/D9fgFnjuzJ0?t=10104
  //     騎士: 轟はじめ https://youtu.be/TEqf-g0WlKY?t=9890
  //     騎士: 夏色まつり https://youtu.be/MKcNzz21P8g?t=9724
  //     お題: ステーキ / スバルの唐揚げ
  // (敬称略)
  interface Delegate {
    void cookStageDidFinish();
  }

  enum TaskItem {
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

    TaskItem(Material material, @Nullable Component specialName, @Nullable Integer customModelData) {
      this.material = material;
      this.specialName = specialName;
      this.customModelData = customModelData;
    }

    TaskItem(Material material) {
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

  enum Task {
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

    Task(Material material, String tag) {
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

  record Recipe(TaskItem[] materials, TaskItem product) {
    boolean consumeMaterialsIfPossible(Inventory inventory, int materialSlotFrom, int materialSlotTo, int productSlot) {
      var matches = new int[materials.length];
      var expected = new ItemStack[materials.length];
      for (int j = 0; j < materials.length; j++) {
        matches[j] = -1;
        expected[j] = materials[j].toItem();
      }
      for (int i = materialSlotFrom; i <= materialSlotTo; i++) {
        var item = inventory.getItem(i);
        if (item == null) {
          continue;
        }
        for (int j = 0; j < materials.length; j++) {
          if (matches[j] < 0 && item.isSimilar(expected[j])) {
            matches[j] = i;
            break;
          }
        }
      }

      record RecipeMatch(ItemStack item, int slot) {}
      var result = new ArrayList<RecipeMatch>();
      for (int j = 0; j < materials.length; j++) {
        if (matches[j] < 0) {
          return false;
        }
        var item = inventory.getItem(matches[j]);
        if (item == null) {
          return false;
        }
        result.add(new RecipeMatch(item, matches[j]));
      }
      for (var match : result) {
        inventory.setItem(match.slot, match.item.subtract());
      }
      var product = inventory.getItem(productSlot);
      if (product != null && product.isSimilar(this.product.toItem())) {
        inventory.setItem(productSlot, product.add());
      } else if (product == null || product.getType() == sProductPlaceholderMaterial) {
        inventory.setItem(productSlot, this.product.toItem());
      }
      return true;
    }
  }

  private static final Material sProductPlaceholderMaterial = Material.OAK_BUTTON;
  private static final Recipe[] sCuttingBoardRecipes = new Recipe[]{
    // https://youtu.be/ZNGqqCothRc?t=9807
    new Recipe(new TaskItem[]{TaskItem.POTATO}, TaskItem.CUT_POTATO),
    new Recipe(new TaskItem[]{TaskItem.CARROT}, TaskItem.CUT_CARROT),
    // https://youtu.be/ls3kb0qhT4E?t=9813
    new Recipe(new TaskItem[]{TaskItem.BEEF}, TaskItem.RAW_GROUND_BEEF),
    // https://youtu.be/yMpj50YZHec?t=9817
    new Recipe(new TaskItem[]{TaskItem.WHEAT}, TaskItem.FLOUR),
    // https://youtu.be/MKcNzz21P8g?t=9738
    new Recipe(new TaskItem[]{TaskItem.CHICKEN}, TaskItem.CHOPPED_CHICKEN),
    new Recipe(new TaskItem[]{TaskItem.SWEET_BERRIES}, TaskItem.CUT_SWEET_BERRIES),
  };
  private static final Recipe[] sServingTableRecipes = new Recipe[]{
    new Recipe(new TaskItem[]{TaskItem.PANCAKES, TaskItem.CUT_SWEET_BERRIES}, TaskItem.MIKO_PANCAKES),
  };
  private static final Recipe[] sCauldronRecipes = new Recipe[]{
    // https://youtu.be/TEqf-g0WlKY?t=9918
    // 7秒: "油" + "切った生の鶏肉" + "小麦粉" -> Text("スバルの唐揚げ / Subaru's Fried Chicken", NamedTextColor.GOLD)
    new Recipe(new TaskItem[]{TaskItem.OIL, TaskItem.CHOPPED_CHICKEN, TaskItem.FLOUR}, TaskItem.SUBARU_FRIED_CHICKEN),
  };
  private static final Recipe[] sHotPlateRecipes = new Recipe[]{
    new Recipe(new TaskItem[]{TaskItem.FLOUR, TaskItem.EGG}, TaskItem.PANCAKES),
    new Recipe(new TaskItem[]{TaskItem.RAW_GROUND_BEEF, TaskItem.CUT_POTATO, TaskItem.CUT_CARROT}, TaskItem.MIO_HAMBURGER_STEAK),
  };

  private final @Nonnull Delegate delegate;
  private @Nullable Inventory cuttingBoard;
  private @Nullable Inventory servingTable;
  private @Nullable Inventory cauldron;
  private @Nullable Inventory hotPlate;

  private final List<Point3i> cuttingBoardBlocks;
  private final Point3i servingTablePos = pos(-99, 81, 8);
  private final Point3i cauldronPos = pos(-99, 81, 9);
  private final List<Point3i> hotPlateBlocks;
  private final Point3i furnacePos = pos(-99, 81, 10);
  private final Point3i[] carrotCrops = new Point3i[]{pos(-90, 80, 7), pos(-89, 80, 7), pos(-90, 80, 8), pos(-89, 80, 8)};
  private final Point3i[] potatoCrops = new Point3i[]{pos(-90, 80, 10), pos(-89, 80, 10), pos(-90, 80, 11), pos(-89, 80, 11)};
  private final Point3i[] wheatCrops = new Point3i[]{pos(-90, 80, 13), pos(-89, 80, 13), pos(-90, 80, 14), pos(-89, 80, 14)};
  private final Point3i[] beetrootCrops = new Point3i[]{pos(-90, 80, 16), pos(-89, 80, 16), pos(-90, 80, 17), pos(-89, 80, 17)};

  CookStage(World world, JavaPlugin owner, Point3i origin, Point3i southEast, @Nonnull Delegate delegate) {
    super(world, owner, origin, southEast.x - origin.x, southEast.z - origin.z);
    this.cuttingBoardBlocks = Arrays.stream(new Point3i[]{
      pos(-98, 82, 6),
      pos(-97, 82, 6),
      pos(-98, 81, 6),
      pos(-97, 81, 6),
    }).collect(Collectors.toList());
    this.hotPlateBlocks = Arrays.stream(new Point3i[]{
      pos(-98, 82, 11),
      pos(-97, 82, 11),
      pos(-98, 81, 11),
      pos(-97, 81, 11),
    }).collect(Collectors.toList());
    this.delegate = delegate;
  }

  @Override
  protected void onStart() {
    prepare();
  }

  @Override
  protected void onFinish() {
    delegate.cookStageDidFinish();
  }

  @Override
  protected void onReset() {
    Kill.EntitiesByScoreboardTag(world, Stage.COOK.tag);
    if (cauldron != null) {
      cauldron.close();
      cauldron = null;
    }
    if (cuttingBoard != null) {
      cuttingBoard.close();
      cuttingBoard = null;
    }
    if (servingTable != null) {
      servingTable.close();
      servingTable = null;
    }
    if (hotPlate != null) {
      hotPlate.close();
      hotPlate = null;
    }
    var furnace = world.getBlockAt(furnacePos.toLocation(world));
    if (furnace.getState(false) instanceof Container container) {
      var inventory = container.getInventory();
      inventory.clear();
    }
  }

  @Override
  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {

  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    if (finished || !started) {
      return;
    }
    var action = e.getAction();
    var block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    var player = e.getPlayer();
    var location = new Point3i(block.getLocation());
    if (action == Action.RIGHT_CLICK_BLOCK) {
      if (location.equals(cauldronPos)) {
        player.openInventory(ensureCauldronInventory());
      } else if (location.equals(servingTablePos)) {
        player.openInventory(ensureServingTableInventory());
      } else if (cuttingBoardBlocks.stream().anyMatch(p -> p.equals(location))) {
        player.openInventory(ensureCuttingBoardInventory());
      } else if (hotPlateBlocks.stream().anyMatch(p -> p.equals(location))) {
        player.openInventory(ensureHotPlateInventory());
      } else {
        return;
      }
      e.setCancelled(true);
    }
  }

  @Override
  protected void onInventoryClick(InventoryClickEvent e, Participation participation) {
    var view = e.getView();
    var top = view.getTopInventory();
    var slot = e.getRawSlot();
    var item = e.getCurrentItem();
    if (top == cuttingBoard) {
      if (participation.role != Role.KNIGHT) {
        e.setCancelled(true);
        return;
      }
      if (!onClickProdctSlot(e, 15)) {
        return;
      }
      if (item == null) {
        return;
      }
      if (slot == 11) {
        // material
      } else if (slot == 13) {
        // iron_axe
        e.setCancelled(true);
        for (var recipe : sCuttingBoardRecipes) {
          if (recipe.consumeMaterialsIfPossible(cuttingBoard, 11, 11, 15)) {
            break;
          }
        }
      } else {
        e.setCancelled(true);
      }
    } else if (top == servingTable) {
      if (participation.role != Role.KNIGHT) {
        e.setCancelled(true);
        return;
      }
      if (!onClickProdctSlot(e, 14)) {
        return;
      }
      if (slot == 12) {
        // bowl
        e.setCancelled(true);
        for (var recipe : sServingTableRecipes) {
          if (recipe.consumeMaterialsIfPossible(servingTable, 29, 33, 14)) {
            break;
          }
        }
      } else if (29 <= slot && slot <= 33) {
        // material
      } else {
        e.setCancelled(true);
      }
    } else if (top == cauldron) {
      if (participation.role != Role.KNIGHT) {
        e.setCancelled(true);
        return;
      }
      if (!onClickProdctSlot(e, 14)) {
        return;
      }
      if (item == null) {
        return;
      }
      if (slot == 12) {
        // steel_and_flint
        //TODO: 着火するだけ, product の生成は 7 秒後
        e.setCancelled(true);
        for (var recipe : sCauldronRecipes) {
          if (recipe.consumeMaterialsIfPossible(cauldron, 30, 32, 14)) {
            break;
          }
        }
      } else if (30 <= slot && slot <= 32) {
        // material
      } else {
        e.setCancelled(true);
      }
    } else if (top == hotPlate) {
      if (participation.role != Role.KNIGHT) {
        e.setCancelled(true);
        return;
      }
      if (!onClickProdctSlot(e, 14)) {
        return;
      }
      if (item == null) {
        return;
      }
      if (slot == 12) {
        // steel_and_flint
        e.setCancelled(true);
        for (var recipe : sHotPlateRecipes) {
          if (recipe.consumeMaterialsIfPossible(hotPlate, 29, 33, 14)) {
            break;
          }
        }
      } else if (29 <= slot && slot <= 33) {
        // material
      } else {
        e.setCancelled(true);
      }
    }
  }

  private boolean onClickProdctSlot(InventoryClickEvent e, int productSlot) {
    var view = e.getView();
    var top = view.getTopInventory();
    var bottom = view.getBottomInventory();
    var clicked = e.getClickedInventory();
    var slot = e.getRawSlot();
    var item = e.getCurrentItem();
    var action = e.getAction();
    if (clicked == bottom) {
      if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || action == InventoryAction.PLACE_SOME) {
        var product = top.getItem(productSlot);
        if (product == null) {
          top.setItem(productSlot, ProductPlaceholderItem());
        }
      }
      return false;
    } else if (item != null) {
      if (slot == productSlot) {
        if (item.getType() == sProductPlaceholderMaterial) {
          e.setCancelled(true);
        } else {
          if (action == InventoryAction.PICKUP_ALL) {
            view.setCursor(item);
            view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            e.setCancelled(true);
          } else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            bottom.addItem(item);
            view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            e.setCancelled(true);
          } else if (action == InventoryAction.PICKUP_HALF) {
            var remain = item.getAmount() / 2;
            var amount = item.getAmount() - remain;
            view.setCursor(item.clone().subtract(remain));
            if (remain == 0) {
              view.setItem(e.getRawSlot(), ProductPlaceholderItem());
            } else {
              view.setItem(e.getRawSlot(), item.clone().subtract(amount));
            }
            e.setCancelled(true);
          } else {
            e.setCancelled(true);
          }
        }
        return false;
      } else {
        return true;
      }
    } else {
      return false;
    }
  }

  private static ItemStack ProductPlaceholderItem() {
    return ItemBuilder.For(sProductPlaceholderMaterial).displayName(Component.empty()).build();
  }

  @Override
  protected void onBlockDropItem(BlockDropItemEvent e) {
    var items = e.getItems();
    for (var item : items) {
      var location = item.getLocation();
      if (!bounds.contains(location.toVector())) {
        continue;
      }
      var stack = item.getItemStack();
      var type = stack.getType();
      switch (type) {
        case CARROT, POTATO, WHEAT, BEETROOT -> {
          item.addScoreboardTag(Stage.COOK.tag);
          stack.editMeta(ItemMeta.class, it -> {
            var store = it.getPersistentDataContainer();
            store.set(NamespacedKey.minecraft(Stage.COOK.tag), PersistentDataType.BYTE, (byte) 1);
          });
        }
      }
    }
  }

  @Override
  protected float getProgress() {
    //TODO:
    return 0;
  }

  @Override
  protected @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case KNIGHT -> Text("姫が食べたいものをプレゼントしてあげよう！", NamedTextColor.GREEN);
      case PRINCESS -> {
        //TODO:
        yield Component.empty();
      }
    };
  }

  @Override
  void tick() {
    for (var pos : carrotCrops) {
      growOrPlant(pos, Material.CARROTS);
    }
    for (var pos : potatoCrops) {
      growOrPlant(pos, Material.POTATOES);
    }
    for (var pos : wheatCrops) {
      growOrPlant(pos, Material.WHEAT);
    }
    for (var pos : beetrootCrops) {
      growOrPlant(pos, Material.BEETROOTS);
    }
  }

  private void growOrPlant(Point3i pos, Material material) {
    var block = world.getBlockAt(pos.x, pos.y, pos.z);
    var type = block.getType();
    if (type == material) {
      var blockData = block.getBlockData();
      if (blockData instanceof Ageable ageable) {
        ageable.setAge(Math.min(ageable.getAge() + 1, ageable.getMaximumAge()));
        block.setBlockData(blockData);
      } else {
      }
    } else if (type == Material.AIR) {
      world.setBlockData(pos.x, pos.y, pos.z, material.createBlockData("[age=1]"));
    }
  }

  private void prepare() {
    world.spawn(pos(-97, 82, 11).toLocation(world).add(0, -0.34, -0.02), TextDisplay.class, it -> {
      it.text(Text("鉄板", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (Math.PI)));
    });
    world.spawn(pos(-97, 82, 11).toLocation(world).add(0, -0.5, -0.02), TextDisplay.class, it -> {
      it.text(Text("Hot Plate", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-98, 82, 9).toLocation(world).add(0.02, -0.34, 0.5), TextDisplay.class, it -> {
      it.text(Text("鍋", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)));
    });
    world.spawn(pos(-98, 82, 9).toLocation(world).add(0.02, -0.5, 0.5), TextDisplay.class, it -> {
      it.text(Text("Cauldron", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-98, 82, 8).toLocation(world).add(0.02, -0.34, 0.5), TextDisplay.class, it -> {
      it.text(Text("盛り付け台", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)));
    });
    world.spawn(pos(-98, 82, 8).toLocation(world).add(0.02, -0.5, 0.5), TextDisplay.class, it -> {
      it.text(Text("Serving Table", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-97, 82, 7).toLocation(world).add(0, -0.34, 0.02), TextDisplay.class, it -> {
      it.text(Text("まな板", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
    });
    world.spawn(pos(-97, 82, 7).toLocation(world).add(0, -0.5, 0.02), TextDisplay.class, it -> {
      it.text(Text("Cutting Board", NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().scale(0.6f));
    });

    world.spawn(pos(-98, 81, 13).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(Text("八百屋", NamedTextColor.GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.FARMER);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(Material.EMERALD, Material.SWEET_BERRIES));
      recipes.add(CreateOffer(Material.EMERALD, Material.EGG));
      recipes.add(CreateOffer(
        Material.EMERALD,
        ItemBuilder.For(Material.POTION)
          .potion(PotionType.STRENGTH)
          .displayName(Text("油 / Oil"))
          .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
          .build()
      ));
      recipes.add(CreateOffer(Material.SWEET_BERRIES, Material.EMERALD));
      recipes.add(CreateOffer(Material.EGG, Material.EMERALD));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.POTION)
          .potion(PotionType.STRENGTH)
          .displayName(Text("油 / Oil"))
          .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
          .build(),
        Material.EMERALD
      ));
      it.setRecipes(recipes);
    });

    world.spawn(pos(-98, 81, 15).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(Text("精肉屋", NamedTextColor.GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.BUTCHER);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(Material.EMERALD, Material.CHICKEN));
      recipes.add(CreateOffer(Material.EMERALD, Material.BEEF));
      recipes.add(CreateOffer(Material.EMERALD, Material.MUTTON));
      recipes.add(CreateOffer(Material.EMERALD, Material.RABBIT));
      recipes.add(CreateOffer(Material.CHICKEN, Material.EMERALD));
      recipes.add(CreateOffer(Material.BEEF, Material.EMERALD));
      recipes.add(CreateOffer(Material.MUTTON, Material.EMERALD));
      recipes.add(CreateOffer(Material.RABBIT, Material.EMERALD));
      it.setRecipes(recipes);
    });

    world.spawn(pos(-98, 81, 17).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(Text("雑貨屋", NamedTextColor.GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.SHEPHERD);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(Material.EMERALD, Material.COAL));
      recipes.add(CreateOffer(Material.COAL, Material.EMERALD));
      it.setRecipes(recipes);
    });
  }

  private @Nonnull Inventory ensureCuttingBoardInventory() {
    if (cuttingBoard != null) {
      return cuttingBoard;
    }
    var inventory = Bukkit.createInventory(null, 36, Text("まな板", NamedTextColor.GREEN));
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
    cuttingBoard = inventory;
    return inventory;
  }

  private @Nonnull Inventory ensureServingTableInventory() {
    if (servingTable != null) {
      return servingTable;
    }
    var inventory = Bukkit.createInventory(null, 54, Text("盛り付け台", NamedTextColor.GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var a = new ItemStack(Material.AIR);
    final var b = ItemBuilder.For(Material.BOWL).displayName(Text("盛り付ける！", NamedTextColor.GREEN)).build();
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
    servingTable = inventory;
    return inventory;
  }

  private @Nonnull Inventory ensureCauldronInventory() {
    if (cauldron != null) {
      return cauldron;
    }
    var inventory = Bukkit.createInventory(null, 54, Text("鍋", NamedTextColor.GREEN));
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
    cauldron = inventory;
    return inventory;
  }

  private @Nonnull Inventory ensureHotPlateInventory() {
    if (hotPlate != null) {
      return hotPlate;
    }
    var inventory = Bukkit.createInventory(null, 54, Text("鉄板", NamedTextColor.GREEN));
    final var bsgp = ItemBuilder.For(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var gsgp = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var fas = ItemBuilder.For(Material.FLINT_AND_STEEL).displayName(Text("材料を焼く！", NamedTextColor.GREEN)).build();
    final var ob = ProductPlaceholderItem();
    final var a = new ItemStack(Material.AIR);
    final var wsgp = ItemBuilder.For(Material.WHITE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var c = new ItemStack(Material.CAMPFIRE);
    c.editMeta(ItemMeta.class, it -> {
      it.setCustomModelData(1);
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
    hotPlate = inventory;
    return inventory;
  }

  private static ItemStack AddItemTag(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      PersistentDataContainer container = meta.getPersistentDataContainer();
      container.set(NamespacedKey.minecraft(Stage.COOK.tag), PersistentDataType.BYTE, (byte) 1);
      item.setItemMeta(meta);
    }
    return item;
  }

  private static MerchantRecipe CreateOffer(ItemStack from, ItemStack to) {
    var recipe = new MerchantRecipe(AddItemTag(to), Integer.MAX_VALUE);
    recipe.addIngredient(AddItemTag(from));
    return recipe;
  }

  private static MerchantRecipe CreateOffer(Material from, Material to) {
    return CreateOffer(new ItemStack(from), new ItemStack(to));
  }

  private static MerchantRecipe CreateOffer(Material from, ItemStack to) {
    return CreateOffer(new ItemStack(from), to);
  }

  private static MerchantRecipe CreateOffer(ItemStack from, Material to) {
    return CreateOffer(from, new ItemStack(to));
  }

  private static Task[] GetRecipeBookTasks() {
    return new Task[]{
      Task.BAKED_POTATO,
      Task.COOKED_CHICKEN,
      Task.COOKED_BEEF,
      Task.COOKED_MUTTON,
      Task.COOKED_RABBIT,
      Task.MIO_HAMBERGER_STEAK,
      Task.SUBARU_FRIED_CHICKEN,
      Task.MIKO_PANCAKES
    };
  }

  static @Nonnull ItemStack CreateRecipeBook0() {
    var book = ItemBuilder.For(Material.WRITTEN_BOOK)
      .displayName(Text("秘伝のレシピブック", NamedTextColor.GOLD))
      .customByteTag(Stage.COOK.tag, (byte) 1)
      .build();
    book.editMeta(BookMeta.class, it -> {
      var tasks = GetRecipeBookTasks();
      var pages = Arrays.stream(tasks).flatMap(task -> Arrays.stream(task.getRecipePageJp()));
      var builder = it.toBuilder();
      pages.forEach(builder::addPage);
      it.setTitle("");
      it.setAuthor("");
    });
    return book;
  }

  static @Nonnull ItemStack CreateRecipeBook1() {
    var book = ItemBuilder.For(Material.WRITABLE_BOOK)
      .displayName(Text("The Secret Recipe Book", NamedTextColor.GOLD))
      .customByteTag(Stage.COOK.tag, (byte) 1)
      .build();
    book.editMeta(BookMeta.class, it -> {
      var tasks = GetRecipeBookTasks();
      var pages = Arrays.stream(tasks).flatMap(task -> Arrays.stream(task.getRecipePageEn()));
      var builder = it.toBuilder();
      pages.forEach(builder::addPage);
      it.setTitle("");
      it.setAuthor("");
    });
    return book;
  }

  private int x(int x) {
    return x + 100 + origin.x;
  }

  private int y(int y) {
    return y - 80 + origin.y;
  }

  private int z(int z) {
    return z - 1 + origin.z;
  }

  private Point3i pos(int x, int y, int z) {
    // [-100, 80, 1] は赤チーム用 Level の origin
    return new Point3i(x(x), y(y), z(z));
  }
}
