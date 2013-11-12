package fi.haju.haju3d.server.world;

import java.io.Serializable;

public class WorldInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String worldName;
  private final int seed;

  public WorldInfo(String worldName, int seed) {
    this.worldName = worldName;
    this.seed = seed;
  }

  public String getWorldName() {
    return worldName;
  }

  public int getSeed() {
    return seed;
  }

}
