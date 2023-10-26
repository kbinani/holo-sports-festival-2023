package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

class BuildStage extends Stage {
  interface Delegate {
    void buildStageDidFinish();
  }

  @Nullable
  Delegate delegate;

  BuildStage(World world, JavaPlugin owner, Point3i origin, Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
  }

  @Override
  public void stageStart(){
    stageOpenGate();
    if (delegate != null) {
      //TODO:
      delegate.buildStageDidFinish();
    }
  }

  @Override
  public void stageReset() {
    stageCloseGate();
  }

  @Override
  public void stageOnPlayerMove(PlayerMoveEvent e, Participation participation) {

  }

  @Override
  public void stageOnPlayerInteract(PlayerInteractEvent e, Participation participation) {

  }
}
