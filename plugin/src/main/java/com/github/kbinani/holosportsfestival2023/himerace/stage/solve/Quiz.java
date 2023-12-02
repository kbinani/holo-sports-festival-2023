package com.github.kbinani.holosportsfestival2023.himerace.stage.solve;

import com.github.kbinani.holosportsfestival2023.Colors;
import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point2i;
import com.github.kbinani.holosportsfestival2023.Point3i;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.World;

import java.awt.*;
import java.util.Random;
import java.util.function.Function;

class Quiz {
  enum Cell {
    RED('r', Material.RED_WOOL, Colors.red),
    YELLOW('y', Material.YELLOW_WOOL, Colors.yellow),
    ORANGE('o', Material.ORANGE_WOOL, Colors.darkorange),
    PINK('p', Material.PINK_WOOL, Colors.hotpink);

    final char letter;
    final Material material;
    final Color color;

    Cell(char letter, Material material, TextColor color) {
      this.letter = letter;
      this.material = material;
      this.color = new Color(color.red(), color.green(), color.blue());
    }

    static final Cell[] all = new Cell[]{RED, YELLOW, ORANGE, PINK};
  }

  private final Cell[] cells;
  final Point2i answer;
  private static final int width = 15;
  private static final int actualWidth = 13;
  private static final int height = 4;
  /**
   * 計算量の上限に達して Quiz が生成できなかった時のための fallback.
   * https://youtu.be/dDv4L4rHwGU?t=526
   */
  private static final Quiz fallback = new Quiz(
    new Cell[]{
      Cell.RED, Cell.YELLOW, Cell.RED, Cell.ORANGE, Cell.RED, Cell.PINK, Cell.PINK, Cell.YELLOW, Cell.ORANGE, Cell.RED, Cell.ORANGE, Cell.RED, Cell.PINK,
      Cell.RED, Cell.RED, Cell.PINK, Cell.YELLOW, Cell.ORANGE, Cell.ORANGE, Cell.YELLOW, Cell.PINK, Cell.PINK, Cell.YELLOW, Cell.PINK, Cell.PINK, Cell.RED,
      Cell.YELLOW, Cell.PINK, Cell.RED, Cell.YELLOW, Cell.YELLOW, Cell.RED, Cell.YELLOW, Cell.RED, Cell.ORANGE, Cell.YELLOW, Cell.RED, Cell.PINK, Cell.ORANGE,
      Cell.RED, Cell.ORANGE, Cell.ORANGE, Cell.YELLOW, Cell.RED, Cell.RED, Cell.PINK, Cell.PINK, Cell.YELLOW, Cell.PINK, Cell.RED, Cell.ORANGE, Cell.YELLOW
    },
    new Point2i(3, 1)
  );

  private Quiz(Cell[] cells, Point2i answer) {
    this.cells = cells;
    this.answer = answer;
  }

  Cell answer() {
    return get(answer.x + 1, answer.z + 1);
  }

  Cell get(int x, int y) {
    if (x < 5) {
      return cells[actualWidth * y + x];
    } else if (x < 10) {
      return cells[actualWidth * y + x - 1];
    } else {
      return cells[actualWidth * y + x - 2];
    }
  }

  private boolean valid() {
    if (cells.length != 52) {
      return false;
    }
    var count = 0;
    for (int i = 0; i < 3; i++) {
      int x0 = i * 5;
      for (int j = 0; j < 4; j++) {
        int x = x0 + j;
        for (int z = 0; z < height - 1; z++) {
          boolean match = true;
          for (int t = 0; t < 2 && match; t++) {
            for (int s = 0; s < 2; s++) {
              if (s == 1 && t == 1) {
                continue;
              }
              if (get(answer.x + s, answer.z + t) != get(x + s, z + t)) {
                match = false;
                break;
              }
            }
          }
          if (match) {
            if (count > 0) {
              return false;
            }
            count++;
          }
        }
      }
    }
    return true;
  }

  private static Quiz Candidate(Random random) {
    var cells = new Cell[actualWidth * height];
    for (int i = 0; i < actualWidth * height; i++) {
      int index = random.nextInt(Cell.all.length);
      cells[i] = Cell.all[index];
    }
    int p = random.nextInt(3);
    int ix = random.nextInt(4);
    int x = p * 5 + ix;
    int z = random.nextInt(height - 1);
    return new Quiz(cells, new Point2i(x, z));
  }

  static Quiz Create(Random random) {
    for (int i = 0; i < 32; i++) {
      var quiz = Candidate(random);
      if (quiz.valid()) {
        return quiz;
      }
    }
    return fallback;
  }

  /**
   * get(0, 0) の Cell の座標を指定してブロックを設置する.
   */
  void build(World world, Point3i origin) {
    Build(world, origin, (pos) -> {
      var cell = get(pos.x, pos.z);
      return cell.material;
    });
  }

  static void Conceal(World world, Point3i origin, Material material) {
    Build(world, origin, (p) -> material);
  }

  private static void Build(World world, Point3i origin, Function<Point2i, Material> materialFromPointFunction) {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 5; j++) {
        if (i > 0 && j == 0) {
          continue;
        }
        Point3i localOrigin;
        Point3i direction = switch (i) {
          case 1 -> {
            localOrigin = origin.added(0, 0, -4);
            yield new Point3i(-1, 0, 0);
          }
          case 2 -> {
            localOrigin = origin.added(-4, 0, -4);
            yield new Point3i(0, 0, 1);
          }
          default -> {
            localOrigin = origin;
            yield new Point3i(0, 0, -1);
          }
        };
        var top = localOrigin.added(j * direction.x, j * direction.y, j * direction.z);
        var x = i * 5 + j;
        for (int y = 0; y < height; y++) {
          var material = materialFromPointFunction.apply(new Point2i(x, y));
          var location = top.added(0, -y, 0);
          Editor.Fill(world, location, location, material.createBlockData());
        }
      }
    }
  }

  @Override
  public String toString() {
    var ret = new StringBuilder();
    for (int z = 0; z < height; z++) {
      ret.append('|');
      for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 5; j++) {
          int x = i * 5 + j;
          var c = get(x, z).letter;
          if (answer.x <= x && x <= answer.x + 1 && answer.z <= z && z <= answer.z + 1) {
            c = Character.toUpperCase(c);
          }
          ret.append(c);
        }
        ret.append('|');
      }
      ret.append('\n');
    }
    return ret.substring(0, ret.length() - 1);
  }
}
