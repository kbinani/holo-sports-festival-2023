package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class FightStage extends AbstractStage {
  interface Delegate {
    void fightStageDidFinish();
  }

  @Nullable
  Delegate delegate;

  FightStage(World world, JavaPlugin owner, Point3i origin, Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
  }

  @Override
  protected void onStart() {
    setFinished(true);
  }

  @Override
  protected void onFinish() {
    if (delegate != null) {
      //TODO:
      delegate.fightStageDidFinish();
    }
  }

  @Override
  protected void onReset() {
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
      case PRINCESS -> Component.text("騎士と一緒にモンスターを倒そう！").color(Colors.lime);
      case KNIGHT -> Component.text("姫と一緒にモンスターを倒そう！").color(Colors.lime);
    };
  }
}
