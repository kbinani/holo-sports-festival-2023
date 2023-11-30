package com.github.kbinani.holosportsfestival2023.himerace.stage.cook;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.ItemTag;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.Stage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.itemTag;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class CookStage extends AbstractStage {
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
  public interface Delegate {
    void cookStageSignalActionBarUpdate();
    void cookStageDidFinish();
  }

  static final Material sProductPlaceholderMaterial = Material.OAK_BUTTON;

  private final @Nonnull Delegate delegate;
  private final List<Point3i> cuttingBoardBlocks;
  private final Point3i servingTablePos = pos(-99, 81, 8);
  private final Point3i cauldronPos = pos(-99, 81, 9);
  private final List<Point3i> hotPlateBlocks;
  private final Point3i furnacePos = pos(-99, 81, 10);
  private final Point3i craftingTablePos = pos(-99, 81, 7);
  private final Point3i[] carrotCrops = new Point3i[]{pos(-90, 80, 7), pos(-89, 80, 7), pos(-90, 80, 8), pos(-89, 80, 8)};
  private final Point3i[] potatoCrops = new Point3i[]{pos(-90, 80, 10), pos(-89, 80, 10), pos(-90, 80, 11), pos(-89, 80, 11)};
  private final Point3i[] wheatCrops = new Point3i[]{pos(-90, 80, 13), pos(-89, 80, 13), pos(-90, 80, 14), pos(-89, 80, 14)};
  private final Point3i[] beetrootCrops = new Point3i[]{pos(-90, 80, 16), pos(-89, 80, 16), pos(-90, 80, 17), pos(-89, 80, 17)};

  private @Nullable CuttingBoardKitchenware cuttingBoard;
  private @Nullable ServingTableKitchenware servingTable;
  private @Nullable CauldronKitchenware cauldron;
  private @Nullable HotPlateKitchenware hotPlate;
  private Task easy;
  private Task difficult;
  private boolean isEasyTaskCleared = false;
  private boolean isDifficultTaskCleared = false;

  public CookStage(World world, JavaPlugin owner, Point3i origin, Point3i southEast, @Nonnull Delegate delegate) {
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
    this.easy = Task.selectRandomlyEasyTask();
    this.difficult = Task.selectRandomlyDifficultTask();
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
    closeGate();
    Kill.EntitiesByScoreboardTag(world, Stage.COOK.tag);
    if (cauldron != null) {
      cauldron.dispose();
      cauldron = null;
    }
    if (cuttingBoard != null) {
      cuttingBoard.dispose();
      cuttingBoard = null;
    }
    if (servingTable != null) {
      servingTable.dispose();
      servingTable = null;
    }
    if (hotPlate != null) {
      hotPlate.dispose();
      hotPlate = null;
    }
    var furnace = world.getBlockAt(furnacePos.toLocation(world));
    if (furnace.getState(false) instanceof Container container) {
      var inventory = container.getInventory();
      inventory.clear();
    }
    this.easy = Task.selectRandomlyEasyTask();
    this.difficult = Task.selectRandomlyDifficultTask();
    this.isEasyTaskCleared = false;
    this.isDifficultTaskCleared = false;
  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    var action = e.getAction();
    var block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    var player = e.getPlayer();
    var location = new Point3i(block.getLocation());
    if (action == Action.RIGHT_CLICK_BLOCK) {
      if (location.equals(cauldronPos)) {
        if (participation.role == Role.KNIGHT) {
          player.openInventory(ensureCauldron().inventory);
        }
        player.swingMainHand();
      } else if (location.equals(servingTablePos)) {
        if (participation.role == Role.KNIGHT) {
          player.openInventory(ensureServingTable().inventory);
        }
        player.swingMainHand();
      } else if (cuttingBoardBlocks.stream().anyMatch(p -> p.equals(location))) {
        if (participation.role == Role.KNIGHT) {
          player.openInventory(ensureCuttingBoard().inventory);
        }
        player.swingMainHand();
      } else if (hotPlateBlocks.stream().anyMatch(p -> p.equals(location))) {
        if (participation.role == Role.KNIGHT) {
          player.openInventory(ensureHotPlate().inventory);
        }
        player.swingMainHand();
      } else if (participation.role == Role.PRINCESS && (furnacePos.equals(location) || craftingTablePos.equals(location))) {
        player.swingMainHand();
      } else {
        return;
      }
      e.setCancelled(true);
    }
  }

  @Override
  protected void onInventoryClick(InventoryClickEvent e, Participation participation) {
    if (cuttingBoard != null) {
      cuttingBoard.onInventoryClick(e, owner);
    }
    if (servingTable != null) {
      servingTable.onInventoryClick(e, owner);
    }
    if (cauldron != null) {
      cauldron.onInventoryClick(e, owner);
    }
    if (hotPlate != null) {
      hotPlate.onInventoryClick(e, owner);
    }
  }

  @Override
  protected void onPlayerItemConsume(PlayerItemConsumeEvent e, Participation participation) {
    if (participation.role != Role.PRINCESS) {
      return;
    }
    var item = e.getItem();
    if (!isEasyTaskCleared && Task.ToItem(easy.item).isSimilar(item)) {
      isEasyTaskCleared = true;
      delegate.cookStageSignalActionBarUpdate();
    } else if (!isDifficultTaskCleared && Task.ToItem(difficult.item).isSimilar(item)) {
      isDifficultTaskCleared = true;
      delegate.cookStageSignalActionBarUpdate();
    } else {
      return;
    }
    e.setCancelled(true);
    var player = e.getPlayer();
    var inventory = player.getInventory();
    inventory.remove(item);
    if (isEasyTaskCleared && isDifficultTaskCleared) {
      player.setFoodLevel(20);
      delegate.cookStageDidFinish();
    } else if (isEasyTaskCleared || isDifficultTaskCleared) {
      //NOTE: 本家ではクリアした時に満腹度が全回復する.
      // しかし, アイテムを消費する毎に満腹度が回復したほうが, 姫に言わせたセリフとの矛盾感が減るはず.
      player.setFoodLevel(2 + (20 - 2) / 2);
    }
  }

  static ItemStack ProductPlaceholderItem() {
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
      ItemTag.AddByte(stack, itemTag);
      var type = stack.getType();
      switch (type) {
        case CARROT, POTATO, WHEAT, BEETROOT -> {
          item.addScoreboardTag(Stage.COOK.tag);
          ItemTag.AddByte(stack, Stage.COOK.tag);
        }
      }
    }
  }

  @Override
  protected void onFurnaceSmelt(FurnaceSmeltEvent e) {
    var block = e.getBlock();
    var location = new Point3i(block.getLocation());
    if (!location.equals(furnacePos)) {
      return;
    }
    var result = e.getResult();
    for (var taskItem : TaskItem.values()) {
      if (taskItem.material != result.getType()) {
        continue;
      }
      if (taskItem.customModelData != null) {
        continue;
      }
      e.setResult(Task.ToItem(taskItem));
      break;
    }
  }

  @Override
  public float getProgress() {
    float ret = 0;
    if (isEasyTaskCleared) {
      ret += 0.5f;
    }
    if (isDifficultTaskCleared) {
      ret += 0.5f;
    }
    return ret;
  }

  @Override
  public @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case KNIGHT -> text("姫が食べたいものをプレゼントしてあげよう！", GREEN);
      case PRINCESS -> {
        var easyDecor = isEasyTaskCleared ? AQUA : RED;
        var difficultDecor = isDifficultTaskCleared ? AQUA : RED;
        yield text("『", easyDecor)
          .append(this.easy.item.getDescription().colorIfAbsent(easyDecor))
          .append(text("』", easyDecor))
          .append(text("と", GREEN))
          .append(text("『", difficultDecor))
          .append(this.difficult.item.getDescription().colorIfAbsent(difficultDecor))
          .append(text("』", difficultDecor))
          .append(text("が食べたい！", GREEN));
      }
    };
  }

  @Override
  protected void onTick() {
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
    if (cuttingBoard != null) {
      cuttingBoard.updateValidationMarker();
    }
    if (servingTable != null) {
      servingTable.updateValidationMarker();
    }
    if (cauldron != null) {
      cauldron.updateValidationMarker();
    }
    if (hotPlate != null) {
      hotPlate.updateValidationMarker();
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
      }
    } else if (type == Material.AIR) {
      world.setBlockData(pos.x, pos.y, pos.z, material.createBlockData("[age=1]"));
    }
  }

  private void prepare() {
    world.spawn(pos(-97, 82, 11).toLocation(world).add(0, -0.34, -0.02), TextDisplay.class, it -> {
      it.text(text("鉄板", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (Math.PI)));
    });
    world.spawn(pos(-97, 82, 11).toLocation(world).add(0, -0.5, -0.02), TextDisplay.class, it -> {
      it.text(text("Hot Plate", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-98, 82, 9).toLocation(world).add(0.02, -0.34, 0.5), TextDisplay.class, it -> {
      it.text(text("鍋", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)));
    });
    world.spawn(pos(-98, 82, 9).toLocation(world).add(0.02, -0.5, 0.5), TextDisplay.class, it -> {
      it.text(text("Cauldron", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-98, 82, 8).toLocation(world).add(0.02, -0.34, 0.5), TextDisplay.class, it -> {
      it.text(text("盛り付け台", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)));
    });
    world.spawn(pos(-98, 82, 8).toLocation(world).add(0.02, -0.5, 0.5), TextDisplay.class, it -> {
      it.text(text("Serving Table", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-97, 82, 7).toLocation(world).add(0, -0.34, 0.02), TextDisplay.class, it -> {
      it.text(text("まな板", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
    });
    world.spawn(pos(-97, 82, 7).toLocation(world).add(0, -0.5, 0.02), TextDisplay.class, it -> {
      it.text(text("Cutting Board", GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().scale(0.6f));
    });

    world.spawn(pos(-98, 81, 13).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(text("八百屋", GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.FARMER);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.SWEET_BERRIES));
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.EGG));
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.OIL));
      recipes.add(CreateOffer(TaskItem.SWEET_BERRIES, TaskItem.EMERALD));
      recipes.add(CreateOffer(TaskItem.EGG, TaskItem.EMERALD));
      recipes.add(CreateOffer(TaskItem.OIL, TaskItem.EMERALD));
      it.setRecipes(recipes);
    });

    world.spawn(pos(-98, 81, 15).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(text("精肉屋", GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.BUTCHER);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.CHICKEN));
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.BEEF));
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.MUTTON));
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.RABBIT));
      recipes.add(CreateOffer(TaskItem.CHICKEN, TaskItem.EMERALD));
      recipes.add(CreateOffer(TaskItem.BEEF, TaskItem.EMERALD));
      recipes.add(CreateOffer(TaskItem.MUTTON, TaskItem.EMERALD));
      recipes.add(CreateOffer(TaskItem.RABBIT, TaskItem.EMERALD));
      it.setRecipes(recipes);
    });

    world.spawn(pos(-98, 81, 17).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(text("雑貨屋", GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.SHEPHERD);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(TaskItem.EMERALD, TaskItem.COAL));
      recipes.add(CreateOffer(TaskItem.COAL, TaskItem.EMERALD));
      it.setRecipes(recipes);
    });
  }

  private @Nonnull CuttingBoardKitchenware ensureCuttingBoard() {
    if (cuttingBoard == null) {
      cuttingBoard = new CuttingBoardKitchenware();
    }
    return cuttingBoard;
  }

  private @Nonnull ServingTableKitchenware ensureServingTable() {
    if (servingTable == null) {
      servingTable = new ServingTableKitchenware();
    }
    return servingTable;
  }

  private @Nonnull CauldronKitchenware ensureCauldron() {
    if (cauldron == null) {
      cauldron = new CauldronKitchenware();
    }
    return cauldron;
  }

  private @Nonnull HotPlateKitchenware ensureHotPlate() {
    if (hotPlate == null) {
      hotPlate = new HotPlateKitchenware();
    }
    return hotPlate;
  }

  static ItemStack AddItemTag(ItemStack item) {
    ItemTag.AddByte(item, Stage.COOK.tag);
    ItemTag.AddByte(item, itemTag);
    return item;
  }

  private static MerchantRecipe CreateOffer(TaskItem from, TaskItem to) {
    var recipe = new MerchantRecipe(Task.ToItem(to), Integer.MAX_VALUE);
    recipe.addIngredient(Task.ToItem(from));
    return recipe;
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

  public static @Nonnull ItemStack CreateRecipeBook0() {
    var book = ItemBuilder.For(Material.WRITTEN_BOOK)
      .displayName(text("秘伝のレシピブック", GOLD))
      .customByteTag(Stage.COOK.tag)
      .customByteTag(itemTag)
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

  public static @Nonnull ItemStack CreateRecipeBook1() {
    var book = ItemBuilder.For(Material.WRITTEN_BOOK)
      .displayName(text("The Secret Recipe Book", GOLD))
      .customByteTag(Stage.COOK.tag)
      .customByteTag(itemTag)
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
