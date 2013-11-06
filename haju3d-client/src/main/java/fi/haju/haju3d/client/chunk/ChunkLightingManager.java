package fi.haju.haju3d.client.chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Singleton;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.World;

@Singleton
public class ChunkLightingManager {

  private Map<Vector3i, ChunkLighting> chunkLights = new ConcurrentHashMap<>();
  //TODO: These should be defined only in one place
  private static final int CHUNK_SIZE = 64;
  private static final int CHUNK_OFFSET_INDEX = Integer.MAX_VALUE / 2 / CHUNK_SIZE;
  private static final int CHUNK_OFFSET_WORLD = CHUNK_OFFSET_INDEX * CHUNK_SIZE;
  
  public void setLight(Vector3i chunkPosition, Vector3i position, int light) {
    if(!chunkLights.containsKey(chunkPosition)) {
      chunkLights.put(chunkPosition, new ChunkLighting(CHUNK_SIZE));
    }
    chunkLights.get(chunkPosition).setLight(position, light);
  }
  
  public int getLight(Vector3i chunkPosition, Vector3i position) {
    ChunkLighting light = chunkLights.get(chunkPosition);
    if(light == null) return 0;
    return light.getLight(position);
  }
  
  public int getLightAtWorldPos(Vector3i worldPosition) {
    return getLight(World.getChunkIndex(worldPosition), World.getPositionWithinChunk(worldPosition));
  }

  
}
