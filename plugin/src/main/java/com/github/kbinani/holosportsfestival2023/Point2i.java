package com.github.kbinani.holosportsfestival2023;

import java.util.Objects;

public class Point2i {
  public int x;
  public int z;

  public Point2i(int x, int z) {
    this.x = x;
    this.z = z;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof Point2i v)) {
      return false;
    }
    return v.x == x && v.z == z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, z);
  }

  @Override
  public String toString() {
    return String.format("[%d,%d]", x, z);
  }
}
