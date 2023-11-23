package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.ItemBuilder;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

class BuildStage extends AbstractStage {
  interface Delegate {
    void buildStageSignalActionBarUpdate();

    void buildStageDidFinish();

    void buildStagePlaySound(Sound sound);

    void buildStageSendTitle(Title title);
  }

  enum Option {
    // 1
    REFRIGERATOR("冷蔵庫 / Refrigerator"),
    WASHING_MACHINE("洗濯機 / Washing machine"),
    TELEVISION("テレビ / Television"),
    TOMATO("トマト / Tomato"),
    APPLE("りんご / Apple"),

    PAPRIKA("パプリカ / Paprika"),
    POLICE_CAR("パトカー / Police car"),
    AMBULANCE("救急車 / Ambulance"),
    CAT("猫 / Cat"),
    RABBIT("うさぎ / Rabbit"),

    // 2
    OOZORA_SUBARU("大空スバル / Oozora Subaru"),
    OOKAMI_MIO("大神ミオ / Ookami Mio"),
    SAKURA_MIKO("さくらみこ / Sakura Miko"),
    MINATO_AQUA("湊あくあ / Minato Aqua"),
    LAPLUS_DARKNESSS("ラプラス・ダークネス / La+ Darknesss"),

    HOUSHOU_MARINE("宝鐘マリン / Houshou Marine"),
    INUGAMI_KORONE("戌神ころね / Inugami Korone"),
    NEKOMATA_OKAYU("猫又おかゆ / Nekomata Okayu"),
    SHIRAKAMI_FUBUKI("白上フブキ / Shirakami Fubuki"),
    HIMEMORI_LUNA("姫森ルーナ / Himemori Luna");

    final String description;

    Option(String description) {
      this.description = description;
    }
  }

  static class Question {
    final Option answer;
    final Option[] options;

    private Question(Option[] options) {
      this.options = options;
      var index = ThreadLocalRandom.current().nextInt(options.length);
      this.answer = options[index];
    }

    static final Option[] first = new Option[]{
      Option.REFRIGERATOR, Option.WASHING_MACHINE, Option.TELEVISION, Option.TOMATO, Option.APPLE,
      Option.PAPRIKA, Option.POLICE_CAR, Option.AMBULANCE, Option.CAT, Option.RABBIT
    };
    static final Option[] second = new Option[]{
      Option.OOZORA_SUBARU, Option.OOKAMI_MIO, Option.SAKURA_MIKO, Option.MINATO_AQUA, Option.LAPLUS_DARKNESSS,
      Option.HOUSHOU_MARINE, Option.INUGAMI_KORONE, Option.NEKOMATA_OKAYU, Option.SHIRAKAMI_FUBUKI, Option.HIMEMORI_LUNA,
    };
  }

  private final @Nonnull Delegate delegate;
  private @Nullable Question first;
  private @Nullable Question second;
  private int step = 0;
  private @Nullable BukkitTask penaltyCooldown;

  BuildStage(World world, JavaPlugin owner, Point3i origin, @Nonnull Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
  }

  @Override
  protected void onStart() {
    openGate();
    this.first = new Question(Question.first);
  }

  @Override
  protected void onFinish() {
    delegate.buildStageDidFinish();
  }

  @Override
  protected void onReset() {
    step = 0;
    first = null;
    second = null;
    if (penaltyCooldown != null) {
      penaltyCooldown.cancel();
      penaltyCooldown = null;
    }
  }

  @Override
  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {

  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    var action = e.getAction();
    var item = e.getItem();
    var player = e.getPlayer();
    switch (participation.role) {
      case PRINCESS -> {
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
          return;
        }
        if (item == null) {
          return;
        }
        if (item.getType() != Material.BOOK) {
          return;
        }
        var meta = item.getItemMeta();
        if (meta == null) {
          return;
        }
        var store = meta.getPersistentDataContainer();
        if (!(store.has(NamespacedKey.minecraft(Stage.BUILD.itemTag), PersistentDataType.BYTE))) {
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
        var inventory = Bukkit.createInventory(null, 18, Component.text("解答する！").color(NamedTextColor.BLUE));
        for (int slot = 0; slot < 18; slot++) {
          var index = OptionIndexFromAnswerInventorySlot(slot);
          if (index < 0) {
            inventory.setItem(slot, ItemBuilder.For(Material.GRAY_STAINED_GLASS_PANE).build());
          } else {
            var option = question.options[index];
            var paper = ItemBuilder.For(Material.PAPER)
              .displayName(Component.text(option.description))
              .build();
            inventory.setItem(slot, paper);
          }
        }
        player.openInventory(inventory);
      }
    }
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
  protected void onInventoryClick(InventoryClickEvent e, Participation participation) {
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
      if (first.options[index] == first.answer) {
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
      if (second.options[index] == second.answer) {
        delegate.buildStageSendTitle(CreateCorrectAnswerTitle());
        delegate.buildStagePlaySound(Sound.ENTITY_PLAYER_LEVELUP);
        setFinished(true);
        return;
      } else {
        second = null;
      }
    }
    delegate.buildStageSignalActionBarUpdate();
    var title = CreatePenaltyTitle(5);
    delegate.buildStageSendTitle(title);
    delegate.buildStagePlaySound(Sound.ENTITY_ITEM_BREAK);
    if (penaltyCooldown != null) {
      penaltyCooldown.cancel();
    }
    final var count = new AtomicInteger(0);
    penaltyCooldown = Bukkit.getScheduler().runTaskTimer(owner, () -> {
      var c = count.incrementAndGet();
      if (c < 5) {
        delegate.buildStageSendTitle(CreatePenaltyTitle(5 - c));
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
      Component.text("正解！").color(NamedTextColor.GREEN),
      Component.empty(),
      times
    );
  }

  private static Title CreateQuestionChangedTitle() {
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    return Title.title(
      Component.text("お題が変更されました。").color(NamedTextColor.GOLD),
      Component.empty(),
      times
    );
  }

  private static Title CreatePenaltyTitle(int seconds) {
    var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500));
    return Title.title(
      Component.text("はずれ！").color(NamedTextColor.RED),
      Component.text(String.format("%d秒後にお題が変更されます。", seconds)).color(NamedTextColor.GREEN),
      times
    );
  }

  @Override
  protected float getProgress() {
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
  protected @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case PRINCESS -> Component.text("騎士達が作っているものを答えよう！").color(Colors.lime);
      case KNIGHT -> {
        var question = step == 0 ? first : second;
        if (question == null) {
          //NOTE: 本家では解答が間違いだった時の出題クールタイム中も, 間違えた問題のお題が action bar に出ている.
          // いったん action bar はクリアした方が分かりやすいはず.
          yield Component.empty();
        }
        yield Component.text("建築で『").color(Colors.lime)
          .append(Component.text(question.answer.description).color(Colors.orange))
          .append(Component.text("』を姫に伝えよう！").color(Colors.lime));
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
