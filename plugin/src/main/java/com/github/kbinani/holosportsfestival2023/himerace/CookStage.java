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
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
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
  interface Delegate {
    void cookStageDidFinish();
  }

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

  CookStage(World world, JavaPlugin owner, Point3i origin, @Nonnull Delegate delegate) {
    super(world, owner, origin);
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
    if (action != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    var block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    var player = e.getPlayer();
    var location = new Point3i(block.getLocation());
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

  @Override
  protected void onInventoryClick(InventoryClickEvent e, Participation participation) {

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
    final var ob = ItemBuilder.For(Material.OAK_BUTTON).displayName(Component.empty()).build();
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
    final var b = ItemBuilder.For(Material.BOWL).build();
    final var ob = ItemBuilder.For(Material.OAK_BUTTON).displayName(Component.empty()).build();
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
    final var ob = ItemBuilder.For(Material.OAK_BUTTON).displayName(Component.empty()).build();
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
    final var fas = ItemBuilder.For(Material.FLINT_AND_STEEL).displayName(Text("調理する！", NamedTextColor.GREEN)).build();
    final var ob = ItemBuilder.For(Material.OAK_BUTTON).displayName(Component.empty()).build();
    final var a = new ItemStack(Material.AIR);
    final var wsgp = ItemBuilder.For(Material.WHITE_STAINED_GLASS_PANE).displayName(Component.empty()).build();
    final var c = new ItemStack(Material.CAMPFIRE);
    c.editMeta(ItemMeta.class, it -> {
      it.setCustomModelData(1);
      //TODO: 着火した時は NamedTextColor.RED
      //NOTE: 鉄板の焚き火は最初から着火しているように見える: https://youtu.be/ZNGqqCothRc?t=9815
      it.displayName(Text("🔥🔥🔥", NamedTextColor.DARK_GRAY));
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

  static @Nonnull ItemStack CreateRecipeBook0() {
    var book = ItemBuilder.For(Material.WRITTEN_BOOK)
      .displayName(Text("秘伝のレシピブック", NamedTextColor.GOLD))
      .customByteTag(Stage.COOK.tag, (byte) 1)
      .build();
    book.editMeta(BookMeta.class, it -> {
      // https://youtu.be/aca8Oy9v8tQ?t=9795
      var page0 = Text("[ベイクドポテト]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・ジャガイモ", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page1 = Text("[焼き鳥]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の鶏肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page2 = Text("[ステーキ]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の牛肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page3 = Text("[焼き羊肉]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の羊肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page4 = Text("[焼き兎肉]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の兎肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page5 = Text("[ミオしゃ特製ハンバーグ♡]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の牛肉", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・ジャガイモ", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・ニンジン", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①まな板で生の牛肉、ジャガイモ、ニンジンを切る！", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("②鉄板で牛ひき肉と切ったジャガイモと切ったニンジンを一緒に焼く！", NamedTextColor.BLACK));
      // https://youtu.be/vHk29E_TIDc?t=3066
      var page6 = Text("[スバルの唐揚げ]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の鶏肉", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・小麦", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・油", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①まな板で生の鶏肉、小麦を切る！", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("②鍋に切った鶏肉、小麦粉、油を入れて揚げる！", NamedTextColor.BLACK));
      var page7 = Text("[えりぃとパンケーキ]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・小麦", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・卵", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・スイートベリー", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①まな板で小麦、スイートベリーを切る！", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("②鉄板で小麦粉と卵を一緒に焼く！", NamedTextColor.BLACK));
      var page8 = Text("[えりぃとパンケーキ]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("③盛り付け台でただのパンケーキと切ったスイートベリーを盛り付ける！", NamedTextColor.BLACK));
      it.addPages(page0, page1, page2, page3, page4, page5, page6, page7, page8);
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
    //TODO: 英訳したものになっているはずだけど一旦日本語版と同じにしてある
    book.editMeta(BookMeta.class, it -> {
      // https://youtu.be/aca8Oy9v8tQ?t=9795
      var page0 = Text("[ベイクドポテト]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・ジャガイモ", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page1 = Text("[焼き鳥]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の鶏肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page2 = Text("[ステーキ]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の牛肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page3 = Text("[焼き羊肉]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の羊肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page4 = Text("[焼き兎肉]", NamedTextColor.BLUE).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の兎肉", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①かまどで精錬！", NamedTextColor.BLACK));
      var page5 = Text("[ミオしゃ特製ハンバーグ♡]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の牛肉", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・ジャガイモ", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・ニンジン", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①まな板で生の牛肉、ジャガイモ、ニンジンを切る！", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("②鉄板で牛ひき肉と切ったジャガイモと切ったニンジンを一緒に焼く！", NamedTextColor.BLACK));
      // https://youtu.be/vHk29E_TIDc?t=3066
      var page6 = Text("[スバルの唐揚げ]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・生の鶏肉", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・小麦", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・油", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①まな板で生の鶏肉、小麦を切る！", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("②鍋に切った鶏肉、小麦粉、油を入れて揚げる！", NamedTextColor.BLACK));
      var page7 = Text("[えりぃとパンケーキ]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("材料", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・小麦", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・卵", NamedTextColor.BLACK)).appendNewline()
        .append(Text("・スイートベリー", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("①まな板で小麦、スイートベリーを切る！", NamedTextColor.BLACK)).appendNewline()
        .appendNewline()
        .append(Text("②鉄板で小麦粉と卵を一緒に焼く！", NamedTextColor.BLACK));
      var page8 = Text("[えりぃとパンケーキ]", NamedTextColor.DARK_RED).appendNewline()
        .appendNewline()
        .append(Text("③盛り付け台でただのパンケーキと切ったスイートベリーを盛り付ける！", NamedTextColor.BLACK));
      it.addPages(page0, page1, page2, page3, page4, page5, page6, page7, page8);
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
