package  com.github.kbinani.holosportsfestival2023.himerace.stage.build;

import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.ItemTag;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.Stage;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class BuildStage extends AbstractStage {
  public interface Delegate {
    void buildStageSignalActionBarUpdate();

    void buildStageDidFinish();

    void buildStagePlaySound(Sound sound);

    void buildStageSendTitle(Title title);
  }

  static class Question {
    final String answer;
    final String[] options;

    private Question(String[] options) {
      this.options = options;
      if (options.length != 10) {
        throw new IllegalArgumentException();
      }
      var index = ThreadLocalRandom.current().nextInt(options.length);
      this.answer = options[index];
    }

    static final String[] first = new String[]{
      "冷蔵庫 / Refrigerator", "洗濯機 / Washing machine", "テレビ / Television", "トマト / Tomato", "りんご / Apple",
      "パプリカ / Paprika", "パトカー / Police car", "救急車 / Ambulance", "猫 / Cat", "うさぎ / Rabbit",
    };
    static final String[] second = new String[]{
      "大空スバル / Oozora Subaru", "大神ミオ / Ookami Mio", "さくらみこ / Sakura Miko", "湊あくあ / Minato Aqua", "ラプラス・ダークネス / La+ Darknesss",
      "宝鐘マリン / Houshou Marine", "戌神ころね / Inugami Korone", "猫又おかゆ / Nekomata Okayu", "白上フブキ / Shirakami Fubuki", "姫森ルーナ / Himemori Luna",
    };
  }

  private static final int sPenaltySeconds = 5;

  private final @Nonnull Delegate delegate;
  private @Nullable Question first;
  private @Nullable Question second;
  private int step = 0;
  private @Nullable BukkitTask penaltyCooldown;

  public BuildStage(World world, JavaPlugin owner, Point3i origin, Point3i southEast, @Nonnull Delegate delegate) {
    super(world, owner, origin, southEast.x - origin.x, southEast.z - origin.z);
    this.delegate = delegate;
  }

  @Override
  protected void onStart() {
    this.first = new Question(Question.first);
  }

  @Override
  protected void onFinish() {
    delegate.buildStageDidFinish();
  }

  @Override
  protected void onReset() {
    closeGate();
    step = 0;
    first = null;
    second = null;
    if (penaltyCooldown != null) {
      penaltyCooldown.cancel();
      penaltyCooldown = null;
    }
  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    if (finished || !started) {
      return;
    }
    var action = e.getAction();
    var item = e.getItem();
    var player = e.getPlayer();
    if (participation.role != Role.PRINCESS) {
      return;
    }
    if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    if (item == null) {
      return;
    }
    if (item.getType() != Material.BOOK) {
      return;
    }
    if (!ItemTag.HasByte(item, Stage.BUILD.tag)) {
      return;
    }
    e.setCancelled(true);
    Question question;
    if (step == 0) {
      question = first;
    } else {
      question = second;
    }
    if (question == null) {
      return;
    }
    var inventory = Bukkit.createInventory(null, 18, text("解答する！", BLUE));
    for (int slot = 0; slot < 18; slot++) {
      var index = OptionIndexFromAnswerInventorySlot(slot);
      if (index < 0) {
        var glassPane = ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE)
          .displayName(Component.empty())
          .build();
        inventory.setItem(slot, glassPane);
      } else {
        var option = question.options[index];
        var paper = ItemBuilder.For(Material.PAPER)
          .displayName(text(option))
          .build();
        inventory.setItem(slot, paper);
      }
    }
    player.openInventory(inventory);
  }

  private static int OptionIndexFromAnswerInventorySlot(int slot) {
    if (2 <= slot && slot <= 6) {
      return slot - 2;
    } else if (11 <= slot && slot <= 15) {
      return slot - 11 + 5;
    } else {
      return -1;
    }
  }

  @Override
  public void onInventoryClick(InventoryClickEvent e, Participation participation) {
    if (finished || !started) {
      return;
    }
    if (participation.role != Role.PRINCESS) {
      return;
    }
    e.setCancelled(true);
    var index = OptionIndexFromAnswerInventorySlot(e.getSlot());
    if (index < 0) {
      return;
    }
    // 「はずれ」の時の様子
    //    - 騎士側: https://youtu.be/uEpmE5WJPW8?t=3342
    //    - 姫側: https://youtu.be/vHk29E_TIDc?t=3027
    if (step == 0) {
      if (first == null) {
        return;
      }
      e.getInventory().close();
      if (first.options[index].equals(first.answer)) {
        delegate.buildStageSendTitle(CreateCorrectAnswerTitle());
        delegate.buildStagePlaySound(Sound.ENTITY_PLAYER_LEVELUP);
        step = 1;
        nextQuiz(1);
        return;
      } else {
        first = null;
      }
    } else {
      if (second == null) {
        return;
      }
      e.getInventory().close();
      if (second.options[index].equals(second.answer)) {
        delegate.buildStageSendTitle(CreateCorrectAnswerTitle());
        delegate.buildStagePlaySound(Sound.ENTITY_PLAYER_LEVELUP);
        setFinished(true);
        return;
      } else {
        second = null;
      }
    }
    delegate.buildStageSignalActionBarUpdate();
    var title = CreatePenaltyTitle(sPenaltySeconds);
    delegate.buildStageSendTitle(title);
    delegate.buildStagePlaySound(Sound.ENTITY_ITEM_BREAK);
    if (penaltyCooldown != null) {
      penaltyCooldown.cancel();
    }
    final var count = new AtomicInteger(0);
    penaltyCooldown = Bukkit.getScheduler().runTaskTimer(owner, () -> {
      var c = count.incrementAndGet();
      if (c < sPenaltySeconds) {
        delegate.buildStageSendTitle(CreatePenaltyTitle(sPenaltySeconds - c));
      } else {
        if (this.penaltyCooldown != null) {
          this.penaltyCooldown.cancel();
          this.penaltyCooldown = null;
        }
        nextQuiz(step);
        delegate.buildStageSendTitle(CreateQuestionChangedTitle());
      }
    }, 0, 20);
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
      text("お題が変更されました。", GOLD),
      Component.empty(),
      times
    );
  }

  private static Title CreatePenaltyTitle(int seconds) {
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    return Title.title(
      text("はずれ！", RED),
      text(String.format("%d秒後にお題が変更されます。", seconds), GREEN),
      times
    );
  }

  @Override
  public float getProgress() {
    if (finished) {
      return 1;
    } else {
      if (step == 0) {
        return 0;
      } else {
        return 0.5f;
      }
    }
  }

  @Override
  public @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case PRINCESS -> text("騎士達が作っているものを答えよう！", GREEN);
      case KNIGHT -> {
        var question = step == 0 ? first : second;
        if (question == null) {
          //NOTE: 本家では解答が間違いだった時の出題クールタイム中も, 間違えた問題のお題が action bar に出たままになっている.
          // いったん action bar はクリアした方が分かりやすいはず.
          yield Component.empty();
        }
        yield text("建築で『", GREEN)
          .append(text(question.answer, GOLD))
          .append(text("』を姫に伝えよう！", GREEN));
      }
    };
  }

  private void nextQuiz(int step) {
    if (step == 0) {
      this.first = new Question(Question.first);
    } else {
      this.second = new Question(Question.second);
    }
    delegate.buildStageSignalActionBarUpdate();
  }

  private int x(int x) {
    return x + 100 + origin.x;
  }

  private int y(int y) {
    return y - 80 + origin.y;
  }

  private int z(int z) {
    return z + 18 + origin.z;
  }

  private Point3i pos(int x, int y, int z) {
    // [-100, 80, -18] は赤チーム用 Level の origin
    return new Point3i(x(x), y(y), z(z));
  }
}
