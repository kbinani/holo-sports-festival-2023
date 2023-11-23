package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

class BuildStage extends AbstractStage {
  interface Delegate {
    void buildStageDidFinish();
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
  private Question first;
  private Question second;
  private int step = 0;

  BuildStage(World world, JavaPlugin owner, Point3i origin, @Nonnull Delegate delegate) {
    super(world, owner, origin);
    this.delegate = delegate;
    this.first = new Question(Question.first);
    this.second = new Question(Question.second);
  }

  @Override
  protected void onStart() {
    openGate();
  }

  @Override
  protected void onFinish() {
    delegate.buildStageDidFinish();
  }

  @Override
  protected void onReset() {
    step = 0;
    first = new Question(Question.first);
    second = new Question(Question.second);
  }

  @Override
  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {

  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {

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
        yield Component.text("建築で『").color(Colors.lime)
          .append(Component.text(question.answer.description).color(Colors.orange))
          .append(Component.text("』を姫に伝えよう！").color(Colors.lime));
      }
    };
  }
}
