package fi.haju.haju3d.client.chunk.light;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Singleton;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

@Singleton
public class ChunkLightManager {

  private Map<ChunkPosition, ChunkLighting> chunkLights = new ConcurrentHashMap<>();
  
  public void setLight(ChunkPosition chunkPosition, LocalTilePosition position, int light) {
    if(!chunkLights.containsKey(chunkPosition)) {
      chunkLights.put(chunkPosition, new ChunkLighting(World.CHUNK_SIZE));
    }
    chunkLights.get(chunkPosition).setLight(position, light);
  }
  
  public int getLight(ChunkPosition chunkPosition, LocalTilePosition position) {
    ChunkLighting light = chunkLights.get(chunkPosition);
    if(light == null) return 0;
    return light.getLight(position);
  }
  
  public int getLightAtWorldPos(GlobalTilePosition worldPosition) {
    return getLight(World.getChunkIndex(worldPosition), World.getPositionWithinChunk(worldPosition));
  }
  
  public void calculateChunkLighting(Chunk chunk) {
    if (chunk.hasLight()) {
      for (int x = 0; x < chunk.getWidth(); x++) {
        for (int z = 0; z < chunk.getDepth(); z++) {
          int light = 100;
          for (int y = chunk.getHeight() - 1; y >= 0; y--) {
            if (chunk.get(x, y, z) != Tile.AIR) {
              light = 20;
            }
            setLight(chunk.getPosition(), new LocalTilePosition(x, y, z), light);
          }
        }
      }
    }
  }

  
}
