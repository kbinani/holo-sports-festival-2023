package  com.github.kbinani.holosportsfestival2023.himerace.stage.carry;

import com.github.kbinani.holosportsfestival2023.*;
import com.github.kbinani.holosportsfestival2023.himerace.Participation;
import com.github.kbinani.holosportsfestival2023.himerace.Role;
import com.github.kbinani.holosportsfestival2023.himerace.Team;
import com.github.kbinani.holosportsfestival2023.himerace.stage.AbstractStage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

public class CarryStage extends AbstractStage {
  public interface Delegate {
    void carryStageDidFinish();
  }

  private enum PrincessStatus {
    STATIONAL,
    FALL,
  }

  static final String scoreboardTag = "hololive_sports_festival_2023.himerace.carry_stage";
  private static final Material[] kHeadBlockMaterials = new Material[]{
    Material.MANGROVE_PLANKS,
    Material.BIRCH_PLANKS,
    Material.CHERRY_PLANKS,
  };

  private final Region2D[] firstFloorRegions;
  private final Region2D[] secondFloorRegions;
  private Set<Point2i> activeFloorBlocks = new HashSet<>();
  private final Map<Player, BlockDisplay> headBlocks = new HashMap<>();
  private final BoundingBox boundingBox;
  private boolean firstGateOpen = false;
  private boolean secondGateOpen = false;
  private final @Nonnull Delegate delegate;
  private PrincessStatus princessStatus = PrincessStatus.FALL;
  private float progress = 0;
  private final double startZ = z(-57);
  private final double goalZ = z(-23);

  public CarryStage(World world, JavaPlugin owner, Point3i origin, Point3i southEast, @Nonnull Delegate delegate) {
    super(world, owner, origin, southEast.x - origin.x, southEast.z - origin.z);
    this.delegate = delegate;
    this.firstFloorRegions = new Region2D[]{
      new Region2D(pos(-99, -59), pos(-96, -39)),
      new Region2D(pos(-95, -59), pos(-93, -53)),
      new Region2D(pos(-92, -59), pos(-89, -39)),
      new Region2D(pos(-95, -48), pos(-93, -45)),
      new Region2D(pos(-95, -41), pos(-93, -39)),
    };
    this.secondFloorRegions = new Region2D[]{
      new Region2D(pos(-99, -37), pos(-97, -37)),
      new Region2D(pos(-95, -37), pos(-93, -37)),
      new Region2D(pos(-91, -37), pos(-89, -37)),
      new Region2D(pos(-99, -36), pos(-89, -35)),

      new Region2D(pos(-95, -34), pos(-93, -31)),
      new Region2D(pos(-99, -33), pos(-96, -31)),
      new Region2D(pos(-92, -33), pos(-89, -31)),
      new Region2D(pos(-99, -30), pos(-97, -25)),
      new Region2D(pos(-91, -30), pos(-89, -25)),
      new Region2D(pos(-96, -27), pos(-92, -25)),
    };
    this.boundingBox = new BoundingBox(x(-100), y(80), z(-61), x(-87), y(80), z(-18));
  }

  @Override
  protected void onStart() {
  }

  @Override
  protected void onReset() {
    resetFloors();
    headBlocks.clear();
    Kill.EntitiesByScoreboardTag(world, scoreboardTag);
    princessStatus = PrincessStatus.FALL;
    setOpenFirstGate(false);
    setOpenSecondGate(false);
    progress = 0;
  }

  @Override
  protected void onPlayerMove(PlayerMoveEvent e, Participation participation) {
    var player = e.getPlayer();
    var team = participation.team;
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
        if (!finished) {
          var z = player.getLocation().getZ();
          progress = Math.min(Math.max((float) ((z - startZ) / (goalZ - startZ)), 0.0f), 1.0f);
        }
      }
    }
  }

  @Override
  protected void onPlayerInteract(PlayerInteractEvent e, Participation participation) {
    if (participation.role != Role.PRINCESS) {
      return;
    }
    if (e.getAction() != Action.PHYSICAL) {
      return;
    }
    var block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    var location = new Point3i(block.getLocation());
    if (location.equals(pos(-94, 80, -36))) {
      setOpenFirstGate(true);
    } else if (location.equals(pos(-94, 80, -21))) {
      setOpenSecondGate(true);
      setFinished(true);
    }
  }

  @Override
  protected void onFinish() {
    Kill.EntitiesByScoreboardTag(world, scoreboardTag);
    delegate.carryStageDidFinish();
  }


  @Override
  public float getProgress() {
    if (finished) {
      return 1;
    } else {
      return progress;
    }
  }

  @Override
  public @Nonnull Component getActionBar(Role role) {
    return switch (role) {
      case PRINCESS -> text("騎士達に向こうの足場まで運んでもらいましょう！", GREEN);
      case KNIGHT -> text("向こうの足場までお姫様を運んであげましょう！", GREEN);
    };
  }

  void setOpenFirstGate(boolean open) {
    if (firstGateOpen == open) {
      return;
    }
    firstGateOpen = open;
    var block = open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]";
    Editor.Fill(world, pos(-98, 80, -38), pos(-97, 80, -38), block);
    Editor.Fill(world, pos(-95, 80, -38), pos(-93, 80, -38), block);
    Editor.Fill(world, pos(-91, 80, -38), pos(-90, 80, -38), block);
  }

  void setOpenSecondGate(boolean open) {
    if (secondGateOpen == open) {
      return;
    }
    secondGateOpen = open;
    var block = open ? "air" : "dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]";
    Editor.Fill(world, pos(-98, 80, -24), pos(-90, 80, -24), block);
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

    int y = this.y(82);
    for (var p : blocks) {
      if (activeFloorBlocks.contains(p)) {
        continue;
      }
      Editor.Fill(world, new Point3i(p.x, y, p.z), new Point3i(p.x, y, p.z), Material.BARRIER);
    }
    for (var p : activeFloorBlocks) {
      if (blocks.contains(p)) {
        continue;
      }
      Editor.Fill(world, new Point3i(p.x, y, p.z), new Point3i(p.x, y, p.z), Material.AIR);
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
    var floorY = y(83);
    if (location.getY() < floorY) {
      return false;
    }
    var minY = -64;
    var maxY = 448;
    var base = new BoundingBox(
      x(-95) - 0.3, minY, z(-52) - 0.3,
      x(-92) + 0.3, maxY, z(-48) + 0.3);
    if (base.contains(location.toVector())) {
      return true;
    }
    var gate1 = new BoundingBox(
      x(-99) - 0.3, minY, z(-38) - 0.3,
      x(-88) + 0.3, maxY, z(-37) + 0.3
    );
    if (gate1.contains(location.toVector())) {
      return true;
    }
    var gate2 = new BoundingBox(
      x(-99) - 0.3, minY, z(-24) - 0.3,
      x(-88) + 0.3, maxY, z(-23) + 0.3
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
    var y = y(82) + 0.001 * (index + 1);
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
      Editor.Fill(world, new Point3i(region.northWest.x, y(82), region.northWest.z), new Point3i(region.southEast.x, y(82), region.southEast.z), "air");
    }
    for (var region : secondFloorRegions) {
      Editor.Fill(world, new Point3i(region.northWest.x, y(82), region.northWest.z), new Point3i(region.southEast.x, y(82), region.southEast.z), "air");
    }
  }

  private int x(int x) {
    return x + 100 + origin.x;
  }

  private int y(int y) {
    return y - 80 + origin.y;
  }

  private int z(int z) {
    return z + 61 + origin.z;
  }

  private Point3i pos(int x, int y, int z) {
    // [-100, 80, -61] は赤チーム用 Level の origin
    return new Point3i(x(x), y(y), z(z));
  }

  private Point2i pos(int x, int z) {
    return new Point2i(x(x), z(z));
  }
}
