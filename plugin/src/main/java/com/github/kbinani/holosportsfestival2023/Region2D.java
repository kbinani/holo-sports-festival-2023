package com.github.kbinani.holosportsfestival2023;

public class Region2D {
  public final Point2i northWest;
  public final Point2i southEast;

  public Region2D(Point2i northWest, Point2i southEast) {
    this.northWest = northWest;
    this.southEast = southEast;
  }

  public boolean contains(Point2i p) {
    return northWest.x <= p.x && p.x <= southEast.x && northWest.z <= p.z && p.z <= southEast.z;
  }
}

