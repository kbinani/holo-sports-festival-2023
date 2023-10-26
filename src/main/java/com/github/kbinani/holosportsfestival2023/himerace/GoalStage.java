package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Point2i;
import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.World;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

class GoalStage extends Stage {
  interface Delegate {
    void goalStageDidFinish();
  }

  @Nullable
  Delegate delegate;
  boolean finished = false;

  GoalStage(World world, JavaPlugin owner, Point3i origin, Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
  }

  void setFinished(boolean f) {
    if (finished == f) {
      return;
    }
    finished = f;
    if (finished && delegate != null) {
      delegate.goalStageDidFinish();
    }
  }

  @Override
  void stageStart() {
    stageOpenGate();
    finished = false;
  }

  @Override
  void stageReset() {
    stageCloseGate();
    finished = false;
  }

  @Override
  void stageOnPlayerMove(PlayerMoveEvent e, Participation participation) {

  }

  @Override
  void stageOnPlayerInteract(PlayerInteractEvent e, Participation participation) {
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
