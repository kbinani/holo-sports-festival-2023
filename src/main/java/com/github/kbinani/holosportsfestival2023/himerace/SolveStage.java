package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

class SolveStage extends Stage {
  interface Delegate {
    void solveStageDidFinish();
  }

  @Nullable
  Delegate delegate;
  private Quiz quiz;
  private final Material quizConcealer;
  private final Point3i quizOrigin;
  private boolean quizStarted = false;
  private Quiz activeQuiz;

  SolveStage(World world, JavaPlugin owner, Point3i origin, Material quizConcealer, Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
    this.quizOrigin = pos(-92, 83, 38);
    this.quiz = Quiz.Create(ThreadLocalRandom.current());
    this.quizConcealer = quizConcealer;
  }

  @Override
  protected void onStart() {

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
    quizStarted = false;
    Quiz.Conceal(world, quizOrigin, quizConcealer);
    activeQuiz = null;
    setGateOpened(false);
  }

  @Override
  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {

  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    if (participation.role != Role.PRINCESS) {
      return;
    }
    switch (e.getAction()) {
      case PHYSICAL -> {
        var block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        var location = new Point3i(block.getLocation());
        if (location.equals(pos(-94, 80, 27))) {
          startQuiz(participation.team);
        }
      }
      case RIGHT_CLICK_BLOCK -> {
        var block = e.getClickedBlock();
        if (block == null) {
          return;
        }
        var quiz = this.activeQuiz;
        if (quiz == null) {
          return;
        }
        var location = new Point3i(block.getLocation());
        var face = e.getBlockFace();
        if ((location.equals(pos(-92, 86, 36)) && face == BlockFace.EAST) ||
          (location.equals(pos(-91, 85, 36)) && face == BlockFace.UP) ||
          (location.equals(pos(-90, 86, 36)) && face == BlockFace.WEST) ||
          (location.equals(pos(-91, 87, 36)) && face == BlockFace.DOWN)) {
          var materialActual = e.getMaterial();
          var materialExpected = quiz.answer().material;
          if (materialActual == materialExpected) {
            setGateOpened(true);
            setFinished(true);
          } else {
            // 回答が間違いだった場合.
            // 26 日の配信では間違いだった場合は即ブロック設置がキャンセルになっていたぽいけど,
            // それだと騎士達の提案を待たずに姫側がガチャするのが最速になってしまう.
            // 対策として, 間違いだった場合は何秒か間を空けて違う問題を出題した方がいい気がする.
            //TODO: 時間を開けて再出題する
            activeQuiz = takeQuiz();
            activeQuiz.build(world, quizOrigin);
          }
          e.setCancelled(true);
        }
      }
    }
  }

  private void setGateOpened(boolean open) {
    Editor.Fill(world, pos(-95, 85, 36), pos(-93, 85, 36), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
    Editor.Set(world, pos(-98, 80, 39), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=false]");
    Editor.Set(world, pos(-97, 80, 39), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
    Editor.Set(world, pos(-91, 80, 39), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
    Editor.Set(world, pos(-90, 80, 39), open ? "air" : "dark_oak_fence[east=false,north=false,south=false,waterlogged=false,west=true]");
  }

  private void startQuiz(Team team) {
    if (quizStarted) {
      return;
    }
    quizStarted = true;
    var princess = team.getPrincess();
    if (princess != null) {
      princess.teleport(pos(-91, 85, 34).toLocation(world).add(0.5, 0, 0.5));
    }
    activeQuiz = takeQuiz();
    activeQuiz.build(world, quizOrigin);
  }

  private int x(int x) {
    return x + 100 + origin.x;
  }

  private int y(int y) {
    return y - 80 + origin.y;
  }

  private int z(int z) {
    return z - 22 + origin.z;
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x(x), y(y), z(z));
  }

  private Quiz takeQuiz() {
    var quiz = this.quiz;
    this.quiz = Quiz.Create(ThreadLocalRandom.current());
    return quiz;
  }
}
