package  com.github.kbinani.holosportsfestival2023.himerace.stage.goal;

import com.github.kbinani.holosportsfestival2023.Point2i;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;

public class GoalStage extends AbstractStage {
  public interface Delegate {
    void goalStageDidFinish();
  }

  private final @Nonnull Delegate delegate;
  private final double startZ = z(86);
  private final double goalZ = z(96);

  public GoalStage(World world, JavaPlugin owner, Point3i origin, Point3i southEast, @Nonnull Delegate delegate) {
    super(world, owner, origin, southEast.x - origin.x, southEast.z - origin.z);
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
  public float getProgress() {
    return 1;
  }

  @Override
  public @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case PRINCESS -> Text("騎士と一緒にモンスターを倒そう！", NamedTextColor.GREEN);
      case KNIGHT -> Text("姫と一緒にモンスターを倒そう！", NamedTextColor.GREEN);
    };
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
