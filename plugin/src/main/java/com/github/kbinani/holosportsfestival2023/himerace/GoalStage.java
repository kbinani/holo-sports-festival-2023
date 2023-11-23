package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Point2i;
import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.World;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;

class GoalStage extends AbstractStage {
  interface Delegate {
    void goalStageDidFinish();
  }

  private final @Nonnull Delegate delegate;
  private final double startZ = z(86);
  private final double goalZ = z(96);
  private float progress = 0;

  GoalStage(World world, JavaPlugin owner, Point3i origin, @Nonnull Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
  }

  @Override
  protected void onStart() {
  }

  @Override
  protected void onFinish() {
    delegate.goalStageDidFinish();
  }

  @Override
  protected void onReset() {
    progress = 0;
  }

  @Override
  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {
    if (participation.role == Role.PRINCESS) {
      var player = e.getPlayer();
      var z = player.getLocation().getZ();
      progress = Math.min(Math.max((float) ((z - startZ) / (goalZ - startZ)), 0), 1);
    }
  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    if (participation.role != Role.PRINCESS) {
      return;
    }
    var block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    if (e.getAction() != Action.PHYSICAL) {
      return;
    }
    var location = new Point3i(block.getLocation());
    if (location.equals(pos(-94, 82, 96))) {
      setFinished(true);
    }
  }

  @Override
  protected float getProgress() {
    if (finished) {
      return 1;
    } else {
      return progress;
    }
  }

  private int x(int x) {
    return x + 100 + origin.x;
  }

  private int y(int y) {
    return y - 80 + origin.y;
  }

  private int z(int z) {
    return z - 84 + origin.z;
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x(x), y(y), z(z));
  }

  private Point2i pos(int x, int z) {
    return new Point2i(x(x), z(z));
  }
}
