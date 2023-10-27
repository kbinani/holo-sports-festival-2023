package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

class SolveStage extends Stage {
  interface Delegate {
    void solveStageDidFinish();
  }

  @Nullable
  Delegate delegate;

  SolveStage(World world, JavaPlugin owner, Point3i origin, Delegate delegate) {
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
      delegate.solveStageDidFinish();
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
}
