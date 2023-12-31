package com.github.kbinani.holosportsfestival2023.himerace.stage.solve;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Kill;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.WorldExtension;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.Stage;
import com.github.kbinani.holosportsfestival2023.himerace.Team;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import lombok.experimental.ExtensionMethod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.kbinani.holosportsfestival2023.himerace.HimeraceEventListener.itemTag;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@ExtensionMethod({WorldExtension.class})
public class SolveStage extends AbstractStage {
  public interface Delegate {
    void solveStagePlaySound(Sound sound);

    void solveStageSendTitle(Title title);

    void solveStageDidFinish();
  }

  class Renderer extends MapRenderer {
    private static final int gap = 8;
    private static final int size = (128 - gap * 3) / 2;
    private static final Font font = new Font(Font.SERIF, Font.PLAIN, size);

    record Rendered(Quiz quiz, BufferedImage image) {
    }

    private @Nullable Rendered rendered;

    @Override
    public void render(@Nonnull MapView map, @Nonnull MapCanvas canvas, @Nonnull Player player) {
      if (!started) {
        canvas.drawImage(0, 0, EnsureEmpty());
        return;
      }
      var q = quiz;
      if (q == null) {
        canvas.drawImage(0, 0, EnsureEmpty());
        return;
      }
      if (penaltyCooldown != null) {
        canvas.drawImage(0, 0, EnsureEmpty());
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

    private static @Nullable BufferedImage sEmpty;

    private static BufferedImage EnsureEmpty() {
      if (sEmpty == null) {
        sEmpty = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
      }
      return sEmpty;
    }
  }

  private static final int sPenaltySeconds = 5;

  final @Nonnull Delegate delegate;
  private Quiz quiz;
  private final Material quizConcealer;
  private final Point3i quizOrigin;
  private boolean quizStarted = false;
  private @Nullable ItemFrame itemFrame;
  private final int mapId;
  private @Nullable BukkitTask penaltyCooldown;
  private final Point3i pressurePlatePos = pos(-94, 80, 27);
  private final Point3i answerBlockPos = pos(-91, 86, 36);

  public SolveStage(World world, JavaPlugin owner, Point3i origin, Point3i southEast, Material quizConcealer, int mapId, @Nonnull Delegate delegate) {
    super(world, owner, origin, southEast.x - origin.x, southEast.z - origin.z);
    this.delegate = delegate;
    this.quizOrigin = pos(-92, 83, 38);
    this.quiz = Quiz.Create(ThreadLocalRandom.current());
    this.quizConcealer = quizConcealer;
    this.mapId = mapId;
  }

  @Override
  protected void onStart() {
    summonItemFrame();
  }

  @Override
  protected void onFinish() {
    delegate.solveStageDidFinish();
  }

  @Override
  protected void onReset() {
    closeGate();
    quizStarted = false;
    Quiz.Conceal(world, quizOrigin, quizConcealer);
    setGateOpened(false);
    quiz = Quiz.Create(ThreadLocalRandom.current());
    if (itemFrame != null) {
      itemFrame.remove();
      itemFrame = null;
    }
    Kill.EntitiesByScoreboardTag(world, Stage.SOLVE.tag);
    if (penaltyCooldown != null) {
      penaltyCooldown.cancel();
      penaltyCooldown = null;
    }
    world.set(pressurePlatePos, Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    if (participation.role != Role.PRINCESS) {
      return;
    }
    if (e.getAction() == Action.PHYSICAL) {
      var block = e.getClickedBlock();
      if (block == null) {
        return;
      }
      var location = new Point3i(block.getLocation());
      if (location.equals(pressurePlatePos)) {
        startQuiz(participation.team);
      }
    }
  }

  @Override
  protected void onBlockPlace(BlockPlaceEvent e, Participation participation) {
    var player = e.getPlayer();
    if (participation.role != Role.PRINCESS) {
      if (!player.isOp()) {
        e.setCancelled(true);
      }
      return;
    }
    var block = e.getBlock();
    var location = new Point3i(block.getLocation());
    if (!answerBlockPos.equals(location)) {
      if (!player.isOp()) {
        e.setCancelled(true);
      }
      return;
    }
    e.setCancelled(true);
    var quiz = this.quiz;
    if (quiz == null) {
      return;
    }
    if (penaltyCooldown != null) {
      return;
    }
    player.swingMainHand();
    var materialActual = block.getType();
    var materialExpected = quiz.answer().material;
    if (materialActual == materialExpected) {
      setGateOpened(true);
      setFinished(true);
      var title = CreateCorrectAnswerTitle();
      delegate.solveStageSendTitle(title);
      delegate.solveStagePlaySound(Sound.ENTITY_PLAYER_LEVELUP);
    } else {
      // 回答が間違いだった場合.
      // 26 日の配信では間違いだった場合は即ブロック設置がキャンセルになっていたぽいけど,
      // それだと騎士達の提案を待たずに姫側がガチャするのが最速になってしまう.
      // 対策として, 間違いだった場合は何秒か間を空けて違う問題を出題した方がいい気がする.
      var title = CreatePenaltyTitle(sPenaltySeconds);
      delegate.solveStageSendTitle(title);
      if (penaltyCooldown != null) {
        penaltyCooldown.cancel();
      }
      Quiz.Conceal(world, quizOrigin, quizConcealer);
      final var count = new AtomicInteger(sPenaltySeconds);
      penaltyCooldown = Bukkit.getScheduler().runTaskTimer(owner, () -> {
        if (finished || !started) {
          if (this.penaltyCooldown != null) {
            this.penaltyCooldown.cancel();
            this.penaltyCooldown = null;
          }
        }
        var c = count.decrementAndGet();
        if (c == 0) {
          this.penaltyCooldown.cancel();
          this.penaltyCooldown = null;
          this.delegate.solveStageSendTitle(CreateQuestionChangedTitle());
          this.quiz = Quiz.Create(ThreadLocalRandom.current());
          this.quiz.build(world, quizOrigin);
        } else {
          this.delegate.solveStageSendTitle(CreatePenaltyTitle(c));
        }
      }, 20, 20);
    }
  }

  @Override
  public float getProgress() {
    if (finished) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case KNIGHT -> text("姫と一緒に問題に答えよう！", GREEN);
      case PRINCESS -> {
        if (quizStarted) {
          yield text("騎士と一緒に問題に答えよう！", GREEN);
        } else {
          yield text("感圧板を踏んだらスタート！", GREEN);
        }
      }
    };
  }

  private static Title CreateCorrectAnswerTitle() {
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    return Title.title(
      text("正解！", GREEN),
      Component.empty(),
      times
    );
  }

  private static Title CreateQuestionChangedTitle() {
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    return Title.title(
      text("問題が変更されました。", GOLD),
      Component.empty(),
      times
    );
  }

  private static Title CreatePenaltyTitle(int seconds) {
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    return Title.title(
      text("はずれ！", RED),
      text(String.format("%d秒後に問題が変更されます。", seconds), GREEN),
      times
    );
  }

  private void setGateOpened(boolean open) {
    world.fill(pos(-95, 85, 36), pos(-93, 85, 36), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
    world.set(pos(-98, 80, 39), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=false]");
    world.set(pos(-97, 80, 39), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
    world.set(pos(-91, 80, 39), open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]");
    world.set(pos(-90, 80, 39), open ? "air" : "dark_oak_fence[east=false,north=false,south=false,waterlogged=false,west=true]");
  }

  private void startQuiz(Team team) {
    if (quizStarted) {
      return;
    }
    quizStarted = true;
    var princessTracking = team.getPrincess();
    if (princessTracking != null) {
      var princess = princessTracking.get();
      princess.teleport(pos(-91, 85, 34).toLocation(world).add(0.5, 0, 0.5));
      var inventory = princess.getInventory();
      var materials = new Material[]{Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL, Material.PINK_WOOL};
      for (var i = 0; i < materials.length; i++) {
        var item = ItemBuilder.For(materials[i])
          .displayName(text("ブロックを置いて解答！ / Place the block to answer!"))
          .customTag(itemTag)
          .customTag(Stage.SOLVE.tag)
          .build();
        inventory.setItem(i, item);
      }
    }
    quiz.build(world, quizOrigin);
    Bukkit.getScheduler().runTaskLater(owner, () -> {
      if (finished || !started) {
        return;
      }
      world.set(pressurePlatePos, Material.AIR);
    }, 0);
  }

  private void summonItemFrame() {
    if (itemFrame != null) {
      itemFrame.remove();
    }
    var x = x(-91);
    var y = y(87);
    var z = z(35);
    Kill.EntitiesByType(world, new BoundingBox(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1), EntityType.ITEM_FRAME);
    itemFrame = world.spawn(new Point3i(x, y, z).toLocation(world).add(0.5, 0.5, 0.96875), ItemFrame.class, CreatureSpawnEvent.SpawnReason.COMMAND, (it) -> {
      it.addScoreboardTag(Stage.SOLVE.tag);
      it.setFixed(true);
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
