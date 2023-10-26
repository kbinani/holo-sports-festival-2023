package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

class CookStage extends Stage {
  interface Delegate {
    void cookStageDidFinish();
  }

  @Nullable
  Delegate delegate;

  CookStage(World world, JavaPlugin owner, Point3i origin, Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
  }

  @Override
  void stageStart() {
    stageOpenGate();
    if (delegate != null) {
      //TODO:
      delegate.cookStageDidFinish();
    }
  }

  @Override
  void stageReset() {
    stageCloseGate();
  }

  @Override
  void stageOnPlayerMove(PlayerMoveEvent e, Participation participation, Team team) {

  }

  @Override
  void stageOnPlayerInteract(PlayerInteractEvent e, Participation participation, Team team) {

  }
}
