package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;

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

  private final @Nonnull World world;
  private final @Nonnull Point3i offset;
  private @Nullable Mode mode;
  private final @Nonnull Component kibasenTitle;

  public TrackAndField(@Nonnull World world, @Nonnull Point3i offset) {
    this.world = world;
    this.offset = offset;
    System.out.println("TAF.ctor; offset=" + offset);
    this.kibasenTitle = com.github.kbinani.holosportsfestival2023.kibasen.KibasenEventListener.title;
    kibasenJoinRedSign = pos(-30, 80, 50);
    kibasenJoinWhiteSign = pos(-30, 80, 51);
    kibasenJoinYellowSign = pos(-30, 80, 52);
    kibasenStartSign = pos(-30, 80, 54);
    kibasenAbortSign = pos(-30, 80, 55);
    kibasenEntryListSign = pos(-30, 80, 56);
  }

  public void setMode(Mode mode) {
    if (this.mode != null && this.mode == mode) {
      return;
    }
    switch (mode) {
      case IDLE -> {
        setEnablePhotoSpot(true);
        setEnableKibasenSigns(true);
        setEnableWall(false);
      }
      case TRACK -> {
        setEnableKibasenSigns(false);
        setEnablePhotoSpot(true);
        setEnableWall(true);
      }
      case FIELD -> {
        setEnableKibasenSigns(true);
        setEnablePhotoSpot(false);
        setEnableWall(true);
      }
    }
    this.mode = mode;
  }

  private void setEnablePhotoSpot(boolean enable) {
    if (enable) {
      Editor.Fill(world, pos(-6, 80, 48), pos(0, 80, 53), Material.WHITE_CONCRETE);
      Editor.Fill(world, pos(-6, 81, 48), pos(9, 81, 51), Material.WHITE_CONCRETE);
      Editor.Fill(world, pos(-6, 82, 48), pos(0, 82, 49), Material.WHITE_CONCRETE);
      Editor.Fill(world, pos(1, 80, 48), pos(7, 80, 53), Material.PINK_CONCRETE);
      Editor.Fill(world, pos(1, 81, 48), pos(7, 81, 51), Material.PINK_CONCRETE);
      Editor.Fill(world, pos(1, 82, 48), pos(7, 82, 49), Material.PINK_CONCRETE);
      Editor.Fill(world, pos(8, 80, 48), pos(14, 80, 53), Material.YELLOW_CONCRETE);
      Editor.Fill(world, pos(8, 81, 48), pos(14, 81, 51), Material.YELLOW_CONCRETE);
      Editor.Fill(world, pos(8, 82, 48), pos(14, 82, 49), Material.YELLOW_CONCRETE);
    } else {
      Editor.Fill(world, pos(-6, 80, 48), pos(14, 80, 53), Material.AIR);
      Editor.Fill(world, pos(-6, 81, 48), pos(14, 81, 51), Material.AIR);
      Editor.Fill(world, pos(-6, 82, 48), pos(14, 82, 49), Material.AIR);
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

  private void setEnableWall(boolean enable) {
    var material = enable ? Material.BARRIER : Material.AIR;
    Editor.Fill(world, pos(-24, 81, 31), pos(-24, 86, 73), material);
    Editor.Fill(world, pos(32, 81, 31), pos(32, 86, 73), material);
    Editor.Fill(world, pos(-23, 81, 31), pos(31, 86, 31), material);
    Editor.Fill(world, pos(-23, 81, 73), pos(31, 86, 73), material);
  }

  private Point3i pos(int x, int y, int z) {
    return new Point3i(x + this.offset.x, y + this.offset.y, z + this.offset.z);
  }
}
