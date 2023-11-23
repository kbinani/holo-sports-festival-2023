package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

class SolveStage extends AbstractStage {
  interface Delegate {
    void solveStageDidFinish();
  }

  class Renderer extends MapRenderer {
    private static final int gap = 8;
    private static final int size = (128 - gap * 3) / 2;
    private static final Font font = new Font(Font.SERIF, Font.PLAIN, size);
    record Rendered(Quiz quiz, BufferedImage image) {}
    private @Nullable Rendered rendered;

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
      if (!started) {
        return;
      }
      var q = quiz;
      if (q == null) {
        return;
      }
      if (rendered != null && rendered.quiz == q) {
        canvas.drawImage(0, 0, rendered.image);
        return;
      }
      var answer = q.answer;
      var img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
      var g = img.createGraphics();
      g.setColor(new Color(96, 61, 39));
      g.fillRect(0, 0, 128, 128);
      for (int x = 0; x < 2; x++) {
        for (int y = 0; y < 2; y++) {
          Color color;
          if (x == 1 && y == 1) {
            color = Color.lightGray;
          } else {
            var cell = q.get(answer.x + x, answer.z + y);
            color = cell.color;
          }
          g.setColor(color);
          g.fillRoundRect(gap + (gap + size) * x, gap + (gap + size) * y, size, size, 4, 4);
        }
      }
      g.setColor(Color.red);
      g.setFont(font);
      g.drawString("?", 84, 113);
      canvas.drawImage(0, 0, img);
      rendered = new Rendered(q, img);
    }
  }

  @Nullable
  Delegate delegate;
  private Quiz quiz;
  private final Material quizConcealer;
  private final Point3i quizOrigin;
  private boolean quizStarted = false;
  private @Nullable ItemFrame itemFrame;
  private final int mapId;
  static final String scoreboardTag = "hololive_sports_festival_2023.himerace.solve_stage";

  SolveStage(World world, JavaPlugin owner, Point3i origin, Material quizConcealer, int mapId, Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
    this.quizOrigin = pos(-92, 83, 38);
    this.quiz = Quiz.Create(ThreadLocalRandom.current());
    this.quizConcealer = quizConcealer;
    this.mapId = mapId;
  }

  private Quiz getCurrentQuiz() {
    return quiz;
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
    setGateOpened(false);
    quiz = Quiz.Create(ThreadLocalRandom.current());
    Kill.EntitiesByScoreboardTag(world, scoreboardTag);
    summonItemFrame();
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
        var quiz = this.quiz;
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
            this.quiz = Quiz.Create(ThreadLocalRandom.current());
            this.quiz.build(world, quizOrigin);
          }
          e.setCancelled(true);
        }
      }
    }
  }

  @Override
  protected float getProgress() {
    //TODO:
    return 0;
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
    quiz.build(world, quizOrigin);
  }

  private void summonItemFrame() {
    if (itemFrame != null) {
      itemFrame.remove();
    }
    itemFrame = world.spawn(pos(-91, 87, 35).toLocation(world).add(0.5, 0.5, 0.96875), ItemFrame.class, CreatureSpawnEvent.SpawnReason.COMMAND, (it) -> {
      it.addScoreboardTag(scoreboardTag);
      var item = new ItemStack(Material.FILLED_MAP, 1);
      if (!(item.getItemMeta() instanceof MapMeta meta)) {
        return;
      }
      meta.setMapId(mapId);
      var view = meta.getMapView();
      if (view == null) {
        return;
      }
      view.setLocked(true);
      var renderers = view.getRenderers();
      renderers.forEach(view::removeRenderer);
      view.addRenderer(new Renderer());
      item.setItemMeta(meta);
      it.setItem(item);
    });
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
}
