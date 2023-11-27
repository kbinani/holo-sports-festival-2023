package com.github.kbinani.holosportsfestival2023;

import net.kyori.adventure.text.format.TextColor;

import javax.annotation.Nonnull;

public class Colors {
  public final static @Nonnull TextColor aqua = fromHexString("#00FFFF");
  public final static @Nonnull TextColor darkorange = fromHexString("#FF8C00");
  public final static @Nonnull TextColor gray = fromHexString("#808080");
  public final static @Nonnull TextColor green = fromHexString("#008000");
  public final static @Nonnull TextColor hotpink = fromHexString("#FF69B4");
  public final static @Nonnull TextColor lightblue = fromHexString("#ADD8E6");
  public final static @Nonnull TextColor lightgray = fromHexString("#D3D3D3");
  public final static @Nonnull TextColor lightskyblue = fromHexString("#87CEFA");
  public final static @Nonnull TextColor lime = fromHexString("#00FF00");
  public final static @Nonnull TextColor magenta = fromHexString("#FF00FF");
  public final static @Nonnull TextColor orange = fromHexString("#FFA500");
  public final static @Nonnull TextColor orangered = fromHexString("#FF4500");
  public final static @Nonnull TextColor purple = fromHexString("#800080");
  public final static @Nonnull TextColor red = fromHexString("#FF0000");
  public final static @Nonnull TextColor white = fromHexString("#FFFFFF");
  public final static @Nonnull TextColor yellow = fromHexString("#FFFF00");

  private static @Nonnull TextColor fromHexString(String s) {
    var c = TextColor.fromHexString(s);
    if (c == null) {
      return TextColor.color(0);
    } else {
      return c;
    }
  }

  private Colors() {
  }
}
