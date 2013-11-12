package fi.haju.haju3d.client.chunk.light;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.ChunkCoordinateSystem;
import fi.haju.haju3d.protocol.world.Tile;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ChunkLightManager {

  private Map<ChunkPosition, ChunkLighting> chunkLights = new ConcurrentHashMap<>();

  public static final int AMBIENT = 5;
  public static final int DAY_LIGHT = 100;
  private static final double LIGHT_FALLOFF = 0.8;

  private ChunkCoordinateSystem chunkCoordinateSystem = ChunkCoordinateSystem.DEFAULT;

  public void setLight(ChunkPosition chunkPosition, LocalTilePosition position, int light) {
    if (!chunkLights.containsKey(chunkPosition)) {
      chunkLights.put(chunkPosition, new ChunkLighting(chunkCoordinateSystem.getChunkSize()));
    }
    chunkLights.get(chunkPosition).setLight(position, light);
  }

  public int getLight(ChunkPosition chunkPosition, LocalTilePosition position) {
    ChunkLighting light = chunkLights.get(chunkPosition);
    if (light == null) return AMBIENT;
    return light.getLight(position);
  }

  public int getLightAtWorldPos(GlobalTilePosition worldPosition) {
    return getLight(chunkCoordinateSystem.getChunkIndex(worldPosition), chunkCoordinateSystem.getPositionWithinChunk(worldPosition));
  }

  public void calculateChunkLighting(Chunk chunk) {
    if (chunk.hasLight()) {
      Set<LocalTilePosition> sunned = calculateDirectSunLight(chunk);
      calculateReflectedLight(chunk, sunned);
    }
  }

  private Set<LocalTilePosition> calculateDirectSunLight(Chunk chunk) {
    Set<LocalTilePosition> res = Sets.newHashSet();
    ChunkPosition pos = chunk.getPosition();
    ChunkLighting lighting = chunkLights.get(pos);
    if (lighting == null) {
      lighting = new ChunkLighting(chunkCoordinateSystem.getChunkSize());
      chunkLights.put(pos, lighting);
    }
    for (int x = 0; x < chunk.getWidth(); x++) {
      for (int z = 0; z < chunk.getDepth(); z++) {
        int light = DAY_LIGHT;
        for (int y = chunk.getHeight() - 1; y >= 0; y--) {
          if (chunk.get(x, y, z) != Tile.AIR) {
            break;
          }
          LocalTilePosition p = new LocalTilePosition(x, y, z);
          lighting.setLight(p, light);
          res.add(p);
        }
      }
    }
    return res;
  }

  private void calculateReflectedLight(Chunk chunk, Set<LocalTilePosition> sunned) {
    ChunkLighting lighting = chunkLights.get(chunk.getPosition());
    Queue<LocalTilePosition> tbp = Queues.newArrayDeque(sunned);
    while (!tbp.isEmpty()) {
      LocalTilePosition pos = tbp.remove();
      //TODO: Neighbouring chunks
      List<LocalTilePosition> positions = chunk.getNeighbours(pos);
      int light = lighting.getLight(pos);
      int nv = (int) (LIGHT_FALLOFF * light);
      for (LocalTilePosition nPos : positions) {
        if (chunk.get(nPos) != Tile.AIR) continue;
        int val = lighting.getLight(nPos);
        if (val < nv) {
          lighting.setLight(nPos, nv);
          tbp.add(nPos);
        }
      }
    }
  }


}
