package com.github.kbinani.holosportsfestival2023;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public class Point3i {
  public int x;
  public int y;
  public int z;

  public Point3i(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Point3i(Location l) {
    this.x = l.getBlockX();
    this.y = l.getBlockY();
    this.z = l.getBlockZ();
  }

  public Point3i(Point3i other) {
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof Point3i)) {
      return false;
    }
    Point3i v = (Point3i) o;
    return v.x == x && v.y == y && v.z == z;
  }

  public Point3i added(int x, int y, int z) {
    return new Point3i(this.x + x, this.y + y, this.z + z);
  }

  public Location toLocation(World world, float pitch, float yaw) {
    return new Location(world, x, y, z, pitch, yaw);
  }

  public Location toLocation(World world) {
    return toLocation(world, 0, 0);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, z);
  }

  @Override
  public String toString() {
    return String.format("[%d,%d,%d]", x, y, z);
  }
}
