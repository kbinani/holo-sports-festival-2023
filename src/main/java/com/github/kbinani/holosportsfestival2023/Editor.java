package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;

import javax.annotation.Nonnull;

public class Editor {
  private Editor() {
  }

  public static void StandingSign(@Nonnull World world, Point3i p, Material material, int rot, Component line0, Component line1, Component line2, Component line3) {
    int cx = p.x >> 4;
    int cz = p.z >> 4;
    world.loadChunk(cx, cz);
    BlockData blockData = material.createBlockData("[rotation=" + 8 + "]");
    world.setBlockData(p.x, p.y, p.z, blockData);
    Block block = world.getBlockAt(p.x, p.y, p.z);
    BlockState state = block.getState();
    if (!(state instanceof Sign sign)) {
      return;
    }
    var side = sign.getSide(Side.FRONT);
    side.line(0, line0);
    side.line(1, line1);
    side.line(2, line2);
    side.line(3, line3);
    sign.update();
  }
}