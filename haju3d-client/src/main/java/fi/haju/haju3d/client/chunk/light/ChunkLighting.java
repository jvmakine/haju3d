package fi.haju.haju3d.client.chunk.light;

import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.world.ByteArray3d;

public final class ChunkLighting {

  private final ByteArray3d light;
  private final ByteArray3d sunLight;

  public ChunkLighting(int chunkSize) {
    light = new ByteArray3d(chunkSize, chunkSize, chunkSize);
    sunLight = new ByteArray3d(chunkSize, chunkSize, chunkSize);
  }

  public int getLight(LocalTilePosition pos) {
    if (!light.isInside(pos)) return 0;
    int val = light.get(pos);
    return val < ChunkLightManager.AMBIENT ? ChunkLightManager.AMBIENT : val;
  }

  public void setLight(LocalTilePosition pos, int lightValue) {
    light.set(pos, (byte) lightValue);
  }
  
  public boolean isSunLight(LocalTilePosition pos) {
    if (!sunLight.isInside(pos)) return false;
    return sunLight.get(pos) == 1;
  }
  
  public void setSunLight(LocalTilePosition pos, boolean isInSunLight) {
    sunLight.set(pos, (byte)(isInSunLight ? 1 : 0));
  }

}
