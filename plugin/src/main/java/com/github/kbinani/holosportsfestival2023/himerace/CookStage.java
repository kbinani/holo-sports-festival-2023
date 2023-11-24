package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.SWEET_BERRIES).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EGG).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.POTION)
          .potion(PotionType.STRENGTH)
          .customByteTag(Stage.COOK.tag, (byte) 1)
          .displayName(Text("油 / Oil"))
          .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
          .build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.SWEET_BERRIES).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EGG).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.POTION)
          .potion(PotionType.STRENGTH)
          .customByteTag(Stage.COOK.tag, (byte) 1)
          .displayName(Text("油 / Oil"))
          .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
          .build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      it.setRecipes(recipes);
    });

    world.spawn(pos(-98, 81, 15).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(Text("精肉屋", NamedTextColor.GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.BUTCHER);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.CHICKEN).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.BEEF).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.MUTTON).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.RABBIT).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.CHICKEN).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.BEEF).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.MUTTON).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.RABBIT).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      it.setRecipes(recipes);
    });

    world.spawn(pos(-98, 81, 17).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(Text("雑貨屋", NamedTextColor.GOLD));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setProfession(Villager.Profession.SHEPHERD);
      it.setVillagerLevel(5);
      var recipes = new ArrayList<MerchantRecipe>();
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.COAL).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      recipes.add(CreateOffer(
        ItemBuilder.For(Material.COAL).customByteTag(Stage.COOK.tag, (byte) 1).build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      it.setRecipes(recipes);
    });
  }

  private @Nonnull Inventory ensureCuttingBoardInventory() {
    if (cuttingBoard != null) {
      return cuttingBoard;
    }
    var inventory = Bukkit.createInventory(null, 36, Text("まな板", NamedTextColor.GREEN));
    cuttingBoard = inventory;
    return inventory;
  }

  private @Nonnull Inventory ensureServingTableInventory() {
    if (servingTable != null) {
      return servingTable;
    }
    var inventory = Bukkit.createInventory(null, 54, Text("盛り付け台", NamedTextColor.GREEN));
    servingTable = inventory;
    return inventory;
  }

  private @Nonnull Inventory ensureCauldronInventory() {
    if (cauldron != null) {
      return cauldron;
    }
    var inventory = Bukkit.createInventory(null, 54, Text("鍋", NamedTextColor.GREEN));
    cauldron = inventory;
    return inventory;
  }

  private @Nonnull Inventory ensureHotPlateInventory() {
    if (hotPlate != null) {
      return hotPlate;
    }
    var inventory = Bukkit.createInventory(null, 54, Text("鉄板", NamedTextColor.GREEN));
    hotPlate = inventory;
    return inventory;
  }

  private static MerchantRecipe CreateOffer(ItemStack from, ItemStack to) {
    var recipe = new MerchantRecipe(to, 1);
    recipe.addIngredient(from);
    return recipe;
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
