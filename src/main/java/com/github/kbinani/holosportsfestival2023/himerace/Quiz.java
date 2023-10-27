package com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point2i;
import com.github.kbinani.holosportsfestival2023.Point3i;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Random;

class Quiz {
  enum Cell {
    RED('r', Material.RED_WOOL),
    YELLOW('y', Material.YELLOW_WOOL),
    ORANGE('o', Material.ORANGE_WOOL),
    PINK('p', Material.PINK_WOOL);

    final char letter;
    final Material material;

    Cell(char letter, Material material) {
      this.letter = letter;
      this.material = material;
    }

    static final Cell all[] = new Cell[]{RED, YELLOW, ORANGE, PINK};
  }

  private final Cell cells[];
  private final Point2i answer;
  private static final int width = 15;
  private static final int height = 4;

  private Quiz(Cell cells[], Point2i answer) {
    this.cells = cells;
    this.answer = answer;
  }

  Cell answer() {
    return get(answer.x + 1, answer.z + 1);
  }

  private Cell get(int x, int y) {
    if (x < 5) {
      return cells[width * y + x];
    } else if (x < 10) {
      return cells[width * y + x - 1];
    } else {
      return cells[width * y + x - 2];
    }
  }

  private boolean valid() {
    var count = 0;
    for (int z = 0; z < height - 1; z++) {
      for (int x = 0; x < width - 1; x++) {
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
    return true;
  }

  private static Quiz Candidate(Random random) {
    var cells = new Cell[width * height];
    for (int i = 0; i < width * height; i++) {
      int index = random.nextInt(Cell.all.length);
      cells[i] = Cell.all[index];
    }
    int x = random.nextInt(width - 1);
    int z = random.nextInt(height - 1);
    return new Quiz(cells, new Point2i(x, z));
  }

  static Quiz Create(Random random) {
    while (true) {
      var quiz = Candidate(random);
      if (quiz.valid()) {
        return quiz;
      }
    }
  }

  /**
   * get(0, 0) の Cell の座標を指定してブロックを設置する.
   */
  void build(World world, Point3i origin) {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 5; j++) {
        if (i > 0 && j == 0) {
          continue;
        }
        Point3i localOrigin;
        Point3i direction;
        switch (i) {
          case 1:
            localOrigin = origin.added(0, 0, -4);
            direction = new Point3i(-1, 0, 0);
            break;
          case 2:
            localOrigin = origin.added(-4, 0, -4);
            direction = new Point3i(0, 0, 1);
            break;
          default:
            localOrigin = origin;
            direction = new Point3i(0, 0, -1);
            break;
        }
        var top = localOrigin.added(j * direction.x, j * direction.y, j * direction.z);
        var x = i * 5 + j;
        for (int y = 0; y < height; y++) {
          var cell = get(x, y);
          var location = top.added(0, -y, 0);
          Editor.Fill(world, location, location, cell.material.createBlockData());
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
