package fi.haju.haju3d.client.chunk;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.ByteArray3d;

public class ChunkLighting {
  
  private final ByteArray3d light;
  
  public ChunkLighting(int chunkSize) {
    light = new ByteArray3d(chunkSize, chunkSize, chunkSize);
  }
  
  public int getLight(Vector3i pos) {
    if(!light.isInside(pos)) return 0;
    return light.get(pos);
  }
  
  public void setLight(Vector3i pos, int lightValue) {
    light.set(pos, (byte)lightValue);
  }
  
}
