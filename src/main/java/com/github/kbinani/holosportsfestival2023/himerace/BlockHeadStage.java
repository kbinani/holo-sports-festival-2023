package  com.github.kbinani.holosportsfestival2023.himerace;

import com.github.kbinani.holosportsfestival2023.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

class BlockHeadStage implements Stage {
  private final World world;
  private final JavaPlugin owner;
  private final Point3i origin;
  private final Region2D[] firstFloorRegions;
  private final Region2D[] secondFloorRegions;
  private Set<Point2i> activeFloorBlocks = new HashSet<>();
  private final Map<Player, BlockDisplay> headBlocks = new HashMap<>();
  private final BoundingBox boundingBox;
  static final String scoreboardTag = "hololive_sports_festival_2023.himerace.block_head";
  private boolean firstGateOpen = false;
  private boolean secondGateOpen = false;
  private boolean finished = false;

  interface Delegate {
    void blockHeadStageDidFinish();
  }
  @Nullable
  Delegate delegate;

  enum PrincessStatus {
    STATIONAL,
    FALL,
  }

  private PrincessStatus princessStatus = PrincessStatus.FALL;

  private static final Material[] kHeadBlockMaterials = new Material[]{
      Material.MANGROVE_PLANKS,
      Material.BIRCH_PLANKS,
  };

  BlockHeadStage(World world, JavaPlugin owner, Point3i origin) {
    this.world = world;
    this.owner = owner;
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
    this.boundingBox = new BoundingBox(x(-23), y(-60), z(-16), x(-10), y(-60), z(27));
  }

  @Override
  public void stageReset() {
    resetFloors();
    stageCloseGate();
    headBlocks.clear();
    Kill.EntitiesByScoreboardTag(world, scoreboardTag);
    princessStatus = PrincessStatus.FALL;
    setOpenFirstGate(false);
    setOpenSecondGate(false);
    setFinished(false);
  }

  @Override
  public void stageOnPlayerMove(PlayerMoveEvent e, Participation participation, Team team) {
    var player = e.getPlayer();
    var knights = team.getKnights();
    setFloorForKnights(knights);
    switch (participation.role) {
      case KNIGHT -> {
        setHeadBlocksForKnights(player, knights);
        var princess = team.getPrincess();
        if (princess != null) {
          setPrincessStatus(isPrincessOnHeadBlock(princess) ? PrincessStatus.STATIONAL : PrincessStatus.FALL, team);
        }
      }
      case PRINCESS -> {
        setPrincessStatus(isPrincessOnHeadBlock(player) ? PrincessStatus.STATIONAL : PrincessStatus.FALL, team);
      }
    }
  }

  @Override
  public void stageOnPlayerInteract(PlayerInteractEvent e, Participation participation, Team team) {
    if (participation.role != Role.PRINCESS) {
      return;
    }
    var block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    var location = new Point3i(block.getLocation());
    switch (e.getAction()) {
      case PHYSICAL -> {
        if (location.equals(pos(-17, -60, 9))) {
          setOpenFirstGate(true);
        } else if (location.equals(pos(-17, -60, 24))) {
          setOpenSecondGate(true);
          setFinished(true);
        }
      }
    }
  }

  @Override
  public void stageOpenGate() {
    Stage.OpenGate(owner, world, pos(-19, -60, -16));
  }

  @Override
  public void stageCloseGate() {
    Stage.CloseGate(owner, world, pos(-19, -60, -16));
  }

  private void setFinished(boolean b) {
    if (finished == b) {
      return;
    }
    finished = b;
    if (b && delegate != null) {
      delegate.blockHeadStageDidFinish();
    }
  }

  void setOpenFirstGate(boolean open) {
    if (firstGateOpen == open) {
      return;
    }
    firstGateOpen = open;
    var block = open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]";
    Editor.Fill(world, pos(-21, -60, 7), pos(-20, -60, 7), block);
    Editor.Fill(world, pos(-18, -60, 7), pos(-16, -60, 7), block);
    Editor.Fill(world, pos(-14, -60, 7), pos(-13, -60, 7), block);
  }

  void setOpenSecondGate(boolean open) {
    if (secondGateOpen == open) {
      return;
    }
    secondGateOpen = open;
    var block = open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]";
    Editor.Fill(world, pos(-21, -60, 21), pos(-13, -60, 21), block);
  }

  void setPrincessStatus(PrincessStatus status, Team team) {
    if (princessStatus == status) {
      return;
    }
    princessStatus = status;
    switch (princessStatus) {
      case FALL -> resetFloors();
      case STATIONAL -> setFloorForKnights(team.getKnights());
    }
  }

  void setFloorForKnights(List<Player> players) {
    if (princessStatus == PrincessStatus.FALL) {
      return;
    }
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
      display.setTeleportDuration(1);
      display.teleportAsync(location.toLocation(world));
    }
  }

  private boolean isPrincessOnHeadBlock(Player princess) {
    var location = princess.getLocation();
    var floorY = y(-57);
    if (location.getY() < floorY) {
      return false;
    }
    var minY = -64;
    var maxY = 448;
    var base = new BoundingBox(
        x(-18) - 0.3, minY, z(-7) - 0.3,
        x(-15) + 0.3, maxY, z(-3) + 0.3);
    if (base.contains(location.toVector())) {
      return true;
    }
    var gate1 = new BoundingBox(
      x(-22) - 0.3, minY, z(7) - 0.3,
        x(-11) + 0.3, maxY, z(8) + 0.3
    );
    if (gate1.contains(location.toVector())) {
      return true;
    }
    var gate2 = new BoundingBox(
        x(-22) - 0.3, minY, z(21) - 0.3,
        x(-11) + 0.3, maxY, z(22) + 0.3
    );
    if (gate2.contains(location.toVector())) {
      return true;
    }
    for (var headBlock : headBlocks.values()) {
      var center = headBlock.getLocation().toVector().add(new Vector(0.5, 0.0, 0.5));
      var dx = Math.abs(center.getX() - location.getX());
      var dz = Math.abs(center.getZ() - location.getZ());
      if (dx <= 0.8 && dz <= 0.8) {
        return true;
      }
    }
    return false;
  }

  private BlockDisplay summonBlockDisplay(int index) {
    return world.spawn(new Location(world, origin.x, origin.y, origin.z, 0, 0), BlockDisplay.class, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
      var material = kHeadBlockMaterials[index % kHeadBlockMaterials.length];
      it.setBlock(material.createBlockData());
      it.setBrightness(new Display.Brightness(15, 15));
      it.addScoreboardTag(scoreboardTag);
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
    activeFloorBlocks.clear();
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
