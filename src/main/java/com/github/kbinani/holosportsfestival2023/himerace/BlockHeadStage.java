package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.Editor;
import com.github.kbinani.holosportsfestival2023.Point2i;
import com.github.kbinani.holosportsfestival2023.Point3i;
import com.github.kbinani.holosportsfestival2023.Region2D;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

class BlockHeadStage implements Stage {
  private final World world;
  private final Point3i origin;
  private final Region2D[] firstFloorRegions;
  private final Region2D[] secondFloorRegions;
  private Set<Point2i> activeFloorBlocks = new HashSet<>();
  private Map<Player, BlockDisplay> headBlocks = new HashMap<>();

  private static final Material[] kHeadBlockMaterials = new Material[]{
      Material.MANGROVE_PLANKS,
      Material.BIRCH_PLANKS,
  };

  BlockHeadStage(World world, JavaPlugin owner, Point3i origin) {
    this.world = world;
    this.origin = origin;
    this.firstFloorRegions = new Region2D[]{
        new Region2D(pos(-22, -14), pos(-19, 6)),
        new Region2D(pos(-18, -14), pos(-16, -8)),
        new Region2D(pos(-15, -14), pos(-12, 6)),
        new Region2D(pos(-18, -3), pos(-16, 0)),
        new Region2D(pos(-18, 4), pos(-16, 6)),
    };
    this.secondFloorRegions = new Region2D[]{
        new Region2D(pos(-22, 8), pos(-20, 8)),
        new Region2D(pos(-18, 8), pos(-16, 8)),
        new Region2D(pos(-14, 8), pos(-12, 8)),
        new Region2D(pos(-22, 9), pos(-12, 10)),

        new Region2D(pos(-18, 11), pos(-16, 14)),
        new Region2D(pos(-22, 12), pos(-19, 14)),
        new Region2D(pos(-15, 12), pos(-12, 14)),
        new Region2D(pos(-22, 15), pos(-20, 20)),
        new Region2D(pos(-14, 15), pos(-12, 20)),
        new Region2D(pos(-19, 18), pos(-15, 20)),
    };
  }

  @Override
  public void stageReset() {
    resetFloors();
  }

  @Override
  public void stageOnPlayerMove(Player player, Participation participation, Team team) {
    var knights = team.getKnights();
    setFloorForKnights(knights);
    if (participation.role == Role.KNIGHT) {
      setHeadBlocksForKnights(player, knights);
    }
  }

  void setFloorForKnights(List<Player> players) {
    var blocks = new HashSet<Point2i>();
    for (var player : players) {
      var location = player.getLocation();
      int minX = location.getBlockX() - 1;
      int minZ = location.getBlockZ() - 1;
      int maxX = location.getBlockX() + 1;
      int maxZ = location.getBlockZ() + 1;
      for (int x = minX; x <= maxX; x++) {
        for (int z = minZ; z <= maxZ; z++) {
          var p = new Point2i(x, z);
          if (isFloorBlock(p)) {
            blocks.add(p);
          }
        }
      }
    }

    var material = "barrier";
    int y = this.y(-58);
    for (var p : blocks) {
      if (activeFloorBlocks.contains(p)) {
        continue;
      }
      Editor.Fill(world, new Point3i(p.x, y, p.z), new Point3i(p.x, y, p.z), material);
    }
    for (var p : activeFloorBlocks) {
      if (blocks.contains(p)) {
        continue;
      }
      Editor.Fill(world, new Point3i(p.x, y, p.z), new Point3i(p.x, y, p.z), "air");
    }
    this.activeFloorBlocks = blocks;
  }

  void setHeadBlocksForKnights(Player knight, List<Player> knights) {
    int index = -1;
    for (Player player : knights) {
      index++;
      if (player != knight) {
        continue;
      }
      var display = headBlocks.get(player);
      if (display == null) {
        display = summonBlockDisplay(index);
        headBlocks.put(player, display);
      }
      var location = getBlockDisplayLocation(player, index);
      display.teleport(new Location(world, location.getX(), location.getY(), location.getZ(), 0, 0));
    }
  }

  private BlockDisplay summonBlockDisplay(int index) {
    return world.spawn(new Location(world, origin.x, origin.y, origin.z, 0, 0), BlockDisplay.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      var material = kHeadBlockMaterials[index % kHeadBlockMaterials.length];
      it.setBlock(material.createBlockData());
      it.setBrightness(new Display.Brightness(15, 15));
    });
  }

  private Vector getBlockDisplayLocation(Player player, int index) {
    var location = player.getLocation();
    var x = location.getX() - 0.5;
    // z-fighting を避けるため y 方向は少しずらす
    var y = y(-58) + 0.001 * (index + 1);
    var z = location.getZ() - 0.5;
    return new Vector(x, y, z);
  }

  private boolean isFloorBlock(Point2i p) {
    for (var region : firstFloorRegions) {
      if (region.contains(p)) {
        return true;
      }
    }
    for (var region : secondFloorRegions) {
      if (region.contains(p)) {
        return true;
      }
    }
    return false;
  }

  private void resetFloors() {
    for (var region : firstFloorRegions) {
      Editor.Fill(world, new Point3i(region.northWest.x, y(-58), region.northWest.z), new Point3i(region.southEast.x, y(-58), region.southEast.z), "air");
    }
    for (var region : secondFloorRegions) {
      Editor.Fill(world, new Point3i(region.northWest.x, y(-58), region.northWest.z), new Point3i(region.southEast.x, y(-58), region.southEast.z), "air");
    }
  }

  private int x(int x) {
    return x + 23 + origin.x;
  }

  private int y(int y) {
    return y + 60 + origin.y;
  }

  private int z(int z) {
    return z + 16 + origin.z;
  }

  private Point3i pos(int x, int y, int z) {
    // [-23, -60, -16] はステージを仮の座標で再現した時の、赤チーム用 Level の origin
    return new Point3i(x(x), y(y), z(z));
  }

  private Point2i pos(int x, int z) {
    return new Point2i(x(x), z(z));
  }
}
