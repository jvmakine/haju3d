package fi.haju.haju3d.client.chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Singleton;

import fi.haju.haju3d.protocol.PositionWithinChunk;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.World;

@Singleton
public class ChunkLightingManager {

  private Map<Vector3i, ChunkLighting> chunkLights = new ConcurrentHashMap<>();
  
  public void setLight(Vector3i chunkPosition, PositionWithinChunk position, int light) {
    if(!chunkLights.containsKey(chunkPosition)) {
      chunkLights.put(chunkPosition, new ChunkLighting(World.CHUNK_SIZE));
    }
    chunkLights.get(chunkPosition).setLight(position, light);
  }
  
  public int getLight(Vector3i chunkPosition, PositionWithinChunk position) {
    ChunkLighting light = chunkLights.get(chunkPosition);
    if(light == null) return 0;
    return light.getLight(position);
  }
  
  public int getLightAtWorldPos(Vector3i worldPosition) {
    return getLight(World.getChunkIndex(worldPosition), World.getPositionWithinChunk(worldPosition));
  }

  
}
