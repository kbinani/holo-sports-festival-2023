package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;

class CookStage extends AbstractStage {
  interface Delegate {
    void cookStageDidFinish();
  }

  private final @Nonnull  Delegate delegate;

  CookStage(World world, JavaPlugin owner, Point3i origin, @Nonnull Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
  }

  @Override
  protected void onStart() {
    setFinished(true);
    prepare();
  }

  @Override
  protected void onFinish() {
    delegate.cookStageDidFinish();
  }

  @Override
  protected void onReset() {
    Kill.EntitiesByScoreboardTag(world, Stage.COOK.tag);
  }

  @Override
  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {

  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {

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
      case KNIGHT -> Component.text("姫が食べたいものをプレゼントしてあげよう！").color(NamedTextColor.GREEN);
      case PRINCESS -> {
        //TODO:
        yield Component.empty();
      }
    };
  }

  private void prepare() {
    world.spawn(pos(-97, 82, 11).toLocation(world).add(0, -0.34, -0.02), TextDisplay.class, it -> {
      it.text(Component.text("鉄板").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (Math.PI)));
    });
    world.spawn(pos(-97, 82, 11).toLocation(world).add(0, -0.5, -0.02), TextDisplay.class, it -> {
      it.text(Component.text("Hot Plate").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-98, 82, 9).toLocation(world).add(0.02, -0.34, 0.5), TextDisplay.class, it -> {
      it.text(Component.text("鍋").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)));
    });
    world.spawn(pos(-98, 82, 9).toLocation(world).add(0.02, -0.5, 0.5), TextDisplay.class, it -> {
      it.text(Component.text("Cauldron").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-98, 82, 8).toLocation(world).add(0.02, -0.34, 0.5), TextDisplay.class, it -> {
      it.text(Component.text("盛り付け台").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)));
    });
    world.spawn(pos(-98, 82, 8).toLocation(world).add(0.02, -0.5, 0.5), TextDisplay.class, it -> {
      it.text(Component.text("Serving Table").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().rotateY((float) (90.0 / 180.0 * Math.PI)).scale(0.6f));
    });

    world.spawn(pos(-97, 82, 7).toLocation(world).add(0, -0.34, 0.02), TextDisplay.class, it -> {
      it.text(Component.text("まな板").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
    });
    world.spawn(pos(-97, 82, 7).toLocation(world).add(0, -0.5, 0.02), TextDisplay.class, it -> {
      it.text(Component.text("Cutting Board").color(NamedTextColor.GREEN));
      it.addScoreboardTag(Stage.COOK.tag);
      it.setTransformationMatrix(new Matrix4f().scale(0.6f));
    });

    world.spawn(pos(-98, 81, 13).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(Component.text("八百屋").color(NamedTextColor.GOLD));
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
          .displayName(Component.text("油 / Oil"))
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
          .displayName(Component.text("油 / Oil"))
          .flags(ItemFlag.HIDE_ITEM_SPECIFICS)
          .build(),
        ItemBuilder.For(Material.EMERALD).customByteTag(Stage.COOK.tag, (byte) 1).build()
      ));
      it.setRecipes(recipes);
    });

    world.spawn(pos(-98, 81, 15).toLocation(world).add(0.5, 0, 0.5), Villager.class, it -> {
      it.customName(Component.text("精肉屋").color(NamedTextColor.GOLD));
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
      it.customName(Component.text("雑貨屋").color(NamedTextColor.GOLD));
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
