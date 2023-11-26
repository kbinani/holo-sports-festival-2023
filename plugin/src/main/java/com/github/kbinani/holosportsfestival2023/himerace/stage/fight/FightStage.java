package  com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.kbinani.holosportsfestival2023.ComponentSupport.Text;

public class FightStage extends AbstractStage {
  public interface Delegate {
    void fightStageDidFinish();
  }

  @Nullable
  Delegate delegate;

  public FightStage(World world, JavaPlugin owner, Point3i origin, Point3i southEast, Delegate delegate) {
    super(world, owner, origin, southEast.x - origin.x, southEast.z - origin.z);
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
  public float getProgress() {
    //TODO:
    return 0;
  }

  @Override
  public @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case PRINCESS -> Text("騎士と一緒にモンスターを倒そう！", NamedTextColor.GREEN);
      case KNIGHT -> Text("姫と一緒にモンスターを倒そう！", NamedTextColor.GREEN);
    };
  }
}
