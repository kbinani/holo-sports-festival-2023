package com.github.kbinani.holosportsfestival2023;

import com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener;
import com.github.kbinani.holosportsfestival2023.relay.RelayEventListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class TrackAndField {
  public enum Mode {
    IDLE,
    TRACK,
    FIELD,
    ;
  }

  public final Point3i kibasenJoinRedSign;
  public final Point3i kibasenJoinWhiteSign;
  public final Point3i kibasenJoinYellowSign;
  public final Point3i kibasenStartSign;
  public final Point3i kibasenAbortSign;
  public final Point3i kibasenEntryListSign;

  public final Point3i relayJoinRedSign;
  public final Point3i relayJoinWhiteSign;
  public final Point3i relayJoinYellowSign;
  public final Point3i relayStartSign;
  public final Point3i relayAbortSign;
  public final Point3i relayAnnounceEntryListSign;
  public final Point3i relayResumeSign;

  public final BoundingBox announceBounds;
  public final BoundingBox photoSpotBounds;

  private final @Nonnull World world;
  private final @Nonnull Point3i offset;
  private @Nullable Mode mode;
  private final @Nonnull Component kibasenTitle;
  private final @Nonnull Component relayTitle;

  public TrackAndField(@Nonnull World world, @Nonnull Point3i offset) {
    this.world = world;
    this.offset = offset;
    this.kibasenTitle = KibasenEventListener.title;
    this.relayTitle = RelayEventListener.title;
    kibasenJoinRedSign = pos(-30, 80, 50);
    kibasenJoinWhiteSign = pos(-30, 80, 51);
    kibasenJoinYellowSign = pos(-30, 80, 52);
    kibasenStartSign = pos(-30, 80, 54);
    kibasenAbortSign = pos(-30, 80, 55);
    kibasenEntryListSign = pos(-30, 80, 56);

    relayJoinRedSign = pos(7, 80, 71);
    relayJoinWhiteSign = pos(6, 80, 71);
    relayJoinYellowSign = pos(5, 80, 71);
    relayStartSign = pos(3, 80, 71);
    relayAbortSign = pos(2, 80, 71);
    relayAnnounceEntryListSign = pos(1, 80, 71);
    relayResumeSign = pos(0, 80, 71);

    announceBounds = new BoundingBox(x(-63), y(80), z(13), x(72), 500, z(92));
    photoSpotBounds = new BoundingBox(x(-6), y(80), z(48), x(15), y(83), z(54));
  }

  public void setMode(Mode mode) {
    if (this.mode != null && this.mode == mode) {
      return;
    }
    switch (mode) {
      case IDLE -> {
        setEnablePhotoSpot(true);

        setEnableKibasenSigns(true);
        setEnableKibasenWall(false);

        setEnableRelaySigns(true);
        setEnableRelayWall(false);
        setRelayStartGateEnabled(false);
      }
      case TRACK -> {
        setEnablePhotoSpot(true);

        setEnableKibasenSigns(false);
        setEnableKibasenWall(false);

        setEnableRelaySigns(true);
        setEnableRelayWall(true);
        setRelayStartGateEnabled(false);
      }
      case FIELD -> {
        setEnablePhotoSpot(false);

        setEnableKibasenSigns(true);
        setEnableKibasenWall(true);

        setEnableRelaySigns(false);
        setEnableRelayWall(false);
        setRelayStartGateEnabled(false);
      }
    }
    this.mode = mode;
  }

  public void setRelayStartGateEnabled(boolean enable) {
    if (enable) {
      Editor.Set(world, pos(5, 80, 82), "dark_oak_fence[east=false,north=true,south=false,waterlogged=false,west=false]");
      Editor.Set(world, pos(5, 80, 76), "dark_oak_fence[east=false,north=false,south=true,waterlogged=false,west=false]");
      Editor.Fill(world, pos(5, 80, 81), pos(5, 80, 77), "dark_oak_fence[east=false,north=true,south=true,waterlogged=false,west=false]");
      Editor.Set(world, pos(5, 80, 82), "dark_oak_fence[east=false,north=true,south=false,waterlogged=false,west=false]");
      Editor.Set(world, pos(5, 80, 76), "dark_oak_fence[east=false,north=false,south=true,waterlogged=false,west=false]");
    } else {
      fill(pos(5, 80, 82), pos(5, 80, 76), Material.AIR);
    }
  }

  public void setEnablePhotoSpot(boolean enable) {
    if (enable) {
      fill(pos(-6, 80, 48), pos(0, 80, 53), Material.WHITE_CONCRETE);
      fill(pos(-6, 81, 48), pos(9, 81, 51), Material.WHITE_CONCRETE);
      fill(pos(-6, 82, 48), pos(0, 82, 49), Material.WHITE_CONCRETE);
      fill(pos(1, 80, 48), pos(7, 80, 53), Material.PINK_CONCRETE);
      fill(pos(1, 81, 48), pos(7, 81, 51), Material.PINK_CONCRETE);
      fill(pos(1, 82, 48), pos(7, 82, 49), Material.PINK_CONCRETE);
      fill(pos(8, 80, 48), pos(14, 80, 53), Material.YELLOW_CONCRETE);
      fill(pos(8, 81, 48), pos(14, 81, 51), Material.YELLOW_CONCRETE);
      fill(pos(8, 82, 48), pos(14, 82, 49), Material.YELLOW_CONCRETE);
    } else {
      fill(pos(-6, 80, 48), pos(14, 80, 53), Material.AIR);
      fill(pos(-6, 81, 48), pos(14, 81, 51), Material.AIR);
      fill(pos(-6, 82, 48), pos(14, 82, 49), Material.AIR);
    }
  }

  private void setEnableKibasenSigns(boolean enable) {
    if (enable) {
      Editor.StandingSign(
        world, kibasenJoinRedSign, Material.OAK_SIGN, 4,
        kibasenTitle, TeamColor.RED.component(), Component.empty(), text("右クリでエントリー！", GREEN)
      );
      Editor.StandingSign(
        world, kibasenJoinWhiteSign, Material.OAK_SIGN, 4,
        kibasenTitle, TeamColor.WHITE.component(), Component.empty(), text("右クリでエントリー！", GREEN)
      );
      Editor.StandingSign(
        world, kibasenJoinYellowSign, Material.OAK_SIGN, 4,
        kibasenTitle, TeamColor.YELLOW.component(), Component.empty(), text("右クリでエントリー！", GREEN)
      );

      Editor.StandingSign(
        world, kibasenStartSign, Material.OAK_SIGN, 4,
        kibasenTitle, Component.empty(), Component.empty(), text("ゲームスタート", GREEN)
      );
      Editor.StandingSign(
        world, kibasenAbortSign, Material.OAK_SIGN, 4,
        kibasenTitle, Component.empty(), Component.empty(), text("ゲームを中断する", RED)
      );
      Editor.StandingSign(
        world, kibasenEntryListSign, Material.OAK_SIGN, 4,
        kibasenTitle, Component.empty(), Component.empty(), text("エントリーリスト", AQUA)
      );
    } else {
      Editor.Set(world, kibasenJoinRedSign, Material.AIR);
      Editor.Set(world, kibasenJoinWhiteSign, Material.AIR);
      Editor.Set(world, kibasenJoinYellowSign, Material.AIR);
      Editor.Set(world, kibasenStartSign, Material.AIR);
      Editor.Set(world, kibasenAbortSign, Material.AIR);
      Editor.Set(world, kibasenEntryListSign, Material.AIR);
    }
  }

  private void setEnableKibasenWall(boolean enable) {
    var material = enable ? Material.BARRIER : Material.AIR;
    fill(pos(-24, 81, 31), pos(-24, 86, 73), material);
    fill(pos(32, 81, 31), pos(32, 86, 73), material);
    fill(pos(-23, 81, 31), pos(31, 86, 31), material);
    fill(pos(-23, 81, 73), pos(31, 86, 73), material);
  }

  private void setEnableRelaySigns(boolean enable) {
    if (enable) {
      Editor.StandingSign(
        world, relayJoinRedSign, Material.OAK_SIGN, 8,
        relayTitle, TeamColor.RED.component(), Component.empty(), text("右クリでエントリー！", GREEN)
      );
      Editor.StandingSign(
        world, relayJoinWhiteSign, Material.OAK_SIGN, 8,
        relayTitle, TeamColor.WHITE.component(), Component.empty(), text("右クリでエントリー！", GREEN)
      );
      Editor.StandingSign(
        world, relayJoinYellowSign, Material.OAK_SIGN, 8,
        relayTitle, TeamColor.YELLOW.component(), Component.empty(), text("右クリでエントリー！", GREEN)
      );

      Editor.StandingSign(
        world, relayStartSign, Material.OAK_SIGN, 8,
        relayTitle, Component.empty(), Component.empty(), text("ゲームスタート", GREEN)
      );
      Editor.StandingSign(
        world, relayAbortSign, Material.OAK_SIGN, 8,
        relayTitle, Component.empty(), Component.empty(), text("ゲームを中断する", RED)
      );
      Editor.StandingSign(
        world, relayAnnounceEntryListSign, Material.OAK_SIGN, 8,
        relayTitle, Component.empty(), Component.empty(), text("エントリーリスト", AQUA)
      );
      Editor.StandingSign(
        world, relayResumeSign, Material.OAK_SIGN, 8,
        relayTitle, Component.empty(), Component.empty(), text("ゲームを再開する", GOLD)
      );
    } else {
      Editor.Set(world, relayJoinRedSign, Material.AIR);
      Editor.Set(world, relayJoinWhiteSign, Material.AIR);
      Editor.Set(world, relayJoinYellowSign, Material.AIR);
      Editor.Set(world, relayStartSign, Material.AIR);
      Editor.Set(world, relayAbortSign, Material.AIR);
      Editor.Set(world, relayAnnounceEntryListSign, Material.AIR);
      Editor.Set(world, relayResumeSign, Material.AIR);
    }
  }

  private void setEnableRelayWall(boolean enable) {
    var material = enable ? Material.BARRIER : Material.AIR;
    // 内周
    fill(pos(-2, 85, 75), pos(44, 80, 75), material);
    fill(pos(55, 80, 66), pos(55, 90, 56), material);
    fill(pos(55, 90, 54), pos(55, 80, 48), material);
    fill(pos(55, 84, 47), pos(55, 80, 40), material);
    fill(pos(45, 82, 29), pos(44, 80, 29), material);
    fill(pos(17, 84, 29), pos(-39, 80, 29), material);
    fill(pos(-41, 85, 30), pos(-40, 80, 30), material);
    fill(pos(-43, 85, 31), pos(-42, 80, 31), material);
    fill(pos(-44, 85, 32), pos(-44, 80, 32), material);
    fill(pos(-45, 85, 33), pos(-45, 80, 34), material);
    fill(pos(-46, 85, 35), pos(-46, 80, 36), material);
    fill(pos(-47, 85, 37), pos(-47, 80, 63), material);
    fill(pos(-47, 87, 64), pos(-47, 80, 67), material);
    fill(pos(-46, 87, 68), pos(-46, 80, 69), material);
    fill(pos(-45, 87, 70), pos(-45, 80, 71), material);
    fill(pos(-44, 87, 72), pos(-44, 80, 72), material);
    fill(pos(-43, 80, 73), pos(-42, 87, 73), material);
    fill(pos(-41, 80, 74), pos(-40, 87, 74), material);
    fill(pos(-39, 87, 75), pos(-3, 80, 75), material);

    // 外周
    fill(pos(-2, 85, 83), pos(44, 80, 83), material);
    fill(pos(63, 90, 66), pos(63, 80, 56), material);
    fill(pos(63, 90, 54), pos(63, 80, 48), material);
    fill(pos(63, 84, 47), pos(63, 80, 40), material);
    fill(pos(45, 82, 21), pos(44, 80, 21), material);
    fill(pos(17, 84, 21), pos(-41, 80, 21), material);
    fill(pos(-42, 85, 22), pos(-45, 80, 22), material);
    fill(pos(-46, 85, 23), pos(-46, 80, 23), material);
    fill(pos(-47, 85, 24), pos(-47, 80, 24), material);
    fill(pos(-48, 85, 25), pos(-49, 80, 25), material);
    fill(pos(-50, 85, 26), pos(-50, 80, 26), material);
    fill(pos(-51, 85, 27), pos(-51, 80, 28), material);
    fill(pos(-52, 85, 29), pos(-52, 80, 30), material);
    fill(pos(-53, 85, 31), pos(-53, 80, 32), material);
    fill(pos(-54, 85, 33), pos(-54, 80, 34), material);
    fill(pos(-55, 85, 35), pos(-55, 80, 63), material);
    fill(pos(-55, 87, 64), pos(-55, 80, 69), material);
    fill(pos(-54, 87, 70), pos(-54, 80, 71), material);
    fill(pos(-53, 87, 72), pos(-53, 80, 73), material);
    fill(pos(-52, 87, 74), pos(-52, 80, 75), material);
    fill(pos(-51, 87, 76), pos(-51, 80, 77), material);
    fill(pos(-50, 87, 78), pos(-50, 80, 78), material);
    fill(pos(-49, 87, 79), pos(-48, 80, 79), material);
    fill(pos(-47, 87, 80), pos(-46, 80, 80), material);
    fill(pos(-45, 87, 81), pos(-45, 80, 81), material);
    fill(pos(-44, 87, 82), pos(-44, 80, 82), material);
    fill(pos(-43, 82, 82), pos(-43, 80, 82), material);
    fill(pos(-42, 87, 83), pos(-3, 80, 83), material);
  }

  private void fill(Point3i from, Point3i to, Material material) {
    Editor.Fill(world, from, to, material);
  }

  private int x(int x) {
    return x + offset.x;
  }

  private int y(int y) {
    return y + offset.y;
  }

  private int z(int z) {
    return z + offset.z;
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x + this.offset.x, y + this.offset.y, z + this.offset.z);
  }
}
