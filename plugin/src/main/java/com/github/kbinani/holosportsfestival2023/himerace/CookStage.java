package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.ItemTag;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BookMeta;
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

  static final Material sProductPlaceholderMaterial = Material.OAK_BUTTON;

  private final @Nonnull Delegate delegate;
  private @Nullable CuttingBoardKitchenware cuttingBoard;
  private @Nullable ServingTableKitchenware servingTable;
  private @Nullable CauldronKitchenware cauldron;
  private @Nullable HotPlateKitchenware hotPlate;

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
        player.openInventory(ensureCauldron().inventory);
      } else if (location.equals(servingTablePos)) {
        player.openInventory(ensureServingTable().inventory);
      } else if (cuttingBoardBlocks.stream().anyMatch(p -> p.equals(location))) {
        player.openInventory(ensureCuttingBoard().inventory);
      } else if (hotPlateBlocks.stream().anyMatch(p -> p.equals(location))) {
        player.openInventory(ensureHotPlate().inventory);
      } else {
        return;
      }
      e.setCancelled(true);
    }
  }

  @Override
  protected void onInventoryClick(InventoryClickEvent e, Participation participation) {
    if (cuttingBoard != null) {
      cuttingBoard.onInventoryClick(e);
    }
    if (servingTable != null) {
      servingTable.onInventoryClick(e);
    }
    if (cauldron != null) {
      cauldron.onInventoryClick(e);
    }
    if (hotPlate != null) {
      hotPlate.onInventoryClick(e);
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
    ItemTag.AddByte(result, Stage.COOK.tag);
    for (var taskItem : CookingTaskItem.values()) {
      var task = taskItem.task;
      if (task == null || taskItem.material != result.getType() || taskItem.customModelData != null || taskItem.specialName != null) {
        continue;
      }
      ItemTag.AddByte(result, task.tag);
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

  private static CookingTask[] GetRecipeBookTasks() {
    return new CookingTask[]{
      CookingTask.BAKED_POTATO,
      CookingTask.COOKED_CHICKEN,
      CookingTask.COOKED_BEEF,
      CookingTask.COOKED_MUTTON,
      CookingTask.COOKED_RABBIT,
      CookingTask.MIO_HAMBERGER_STEAK,
      CookingTask.SUBARU_FRIED_CHICKEN,
      CookingTask.MIKO_PANCAKES
    };
  }

  static @Nonnull ItemStack CreateRecipeBook0() {
    var book = ItemBuilder.For(Material.WRITTEN_BOOK)
      .displayName(Text("秘伝のレシピブック", NamedTextColor.GOLD))
      .customByteTag(Stage.COOK.tag)
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
      .customByteTag(Stage.COOK.tag)
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
