package fi.haju.haju3d.client.chunk;

import fi.haju.haju3d.protocol.PositionWithinChunk;
import fi.haju.haju3d.protocol.world.ByteArray3d;

public class ChunkLighting {
  
  private final ByteArray3d light;
  
  public ChunkLighting(int chunkSize) {
    light = new ByteArray3d(chunkSize, chunkSize, chunkSize);
  }
  
  public int getLight(PositionWithinChunk pos) {
    if(!light.isInside(pos)) return 0;
    return light.get(pos);
  }
  
  public void setLight(PositionWithinChunk pos, int lightValue) {
    light.set(pos, (byte)lightValue);
  }
  
}
