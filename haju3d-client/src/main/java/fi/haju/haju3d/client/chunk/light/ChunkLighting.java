package fi.haju.haju3d.client.chunk.light;

import fi.haju.haju3d.client.util.ShortArray3d;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;

public final class ChunkLighting {

  private final ShortArray3d light;
  
  public ChunkLighting(int chunkSize) {
    light = new ShortArray3d(chunkSize, chunkSize, chunkSize);
  }

  public TileLight getLight(LocalTilePosition pos) {
    if (!light.isInside(pos)) return new TileLight();
    return new TileLight(light.get(pos));
  }

  public void setLight(LocalTilePosition pos, TileLight val) {
    light.set(pos, val.getData());
  }
  
}
