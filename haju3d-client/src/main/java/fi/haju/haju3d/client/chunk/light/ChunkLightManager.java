package fi.haju.haju3d.client.chunk.light;

import java.util.List;
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
  
  public static final int AMBIENT = 5;
  public static final int DAY_LIGHT = 100;
  
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
      calculateDirectSunLight(chunk);
      calculateReflectedLight(chunk);
    }
  }

  private void calculateDirectSunLight(Chunk chunk) {
    ChunkPosition pos = chunk.getPosition();
    ChunkLighting lighting = chunkLights.get(pos);
    if(lighting == null) {
      lighting = new ChunkLighting(World.CHUNK_SIZE);
      chunkLights.put(pos, lighting);
    }
    for (int x = 0; x < chunk.getWidth(); x++) {
      for (int z = 0; z < chunk.getDepth(); z++) {
        int light = DAY_LIGHT;
        for (int y = chunk.getHeight() - 1; y >= 0; y--) {
          if (chunk.get(x, y, z) != Tile.AIR) {
            light = AMBIENT;
          }
          lighting.setLight(new LocalTilePosition(x, y, z), light);
        }
      }
    }
  }
  
  private void calculateReflectedLight(Chunk chunk) {
    ChunkLighting lighting = chunkLights.get(chunk.getPosition());
    for (int x = 1; x < chunk.getWidth() - 1; x++) {
      for (int z = 1; z < chunk.getDepth() - 1; z++) {
        for (int y = 1; y < chunk.getHeight() - 1; y++) {
          if (chunk.get(x, y, z) == Tile.AIR) {
            LocalTilePosition pos = new LocalTilePosition(x,y,z);
            List<LocalTilePosition> positions = chunk.getNeighbours(pos);
            int maxLight = lighting.getLight(pos);
            for(LocalTilePosition nPos : positions) {
              int val = (int)(lighting.getLight(nPos) * 0.8);
              if(val > maxLight) maxLight = val;
            }
            lighting.setLight(pos, maxLight);
          }
        }
      }
    }
  }

  
}
