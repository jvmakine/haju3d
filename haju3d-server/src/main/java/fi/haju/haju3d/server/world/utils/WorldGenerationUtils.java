package fi.haju.haju3d.server.world.utils;

import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;

public final class WorldGenerationUtils {

  private WorldGenerationUtils() {
  }

  public static int findGround(Chunk chunk, int h, int midX, int midZ) {
    for (int y = 0; y < h; y++) {
      int testY = h - 1 - y;
      if (chunk.get(midX, testY, midZ) != Tile.AIR) {
        return testY;
      }
    }
    return -1;
  }

}
