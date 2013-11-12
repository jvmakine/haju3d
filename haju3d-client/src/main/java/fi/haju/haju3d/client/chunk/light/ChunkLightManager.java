package fi.haju.haju3d.client.chunk.light;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Optional;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import fi.haju.haju3d.client.chunk.ChunkProvider;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.ChunkCoordinateSystem;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.TilePosition;

@Singleton
public class ChunkLightManager {

  private Map<ChunkPosition, ChunkLighting> chunkLights = new ConcurrentHashMap<>();
  
  public static final int AMBIENT = 5;
  public static final int DAY_LIGHT = 100;
  private static final double LIGHT_FALLOFF = 0.8;

  private ChunkCoordinateSystem chunkCoordinateSystem = ChunkCoordinateSystem.DEFAULT;


  
  @Inject
  private ChunkProvider chunkProvider;
    
  public int getLight(ChunkPosition chunkPosition, LocalTilePosition position) {
    ChunkLighting light = chunkLights.get(chunkPosition);
    if(light == null) return AMBIENT;
    return light.getLight(position);
  }
  
  private void setLight(TilePosition pos, int val) {
    ChunkLighting light = chunkLights.get(pos.getChunkPosition());
    if(light == null) return;
    light.setLight(pos.getTileWithinChunk(), val);
  }
  
  public int getLight(TilePosition pos) {
    return getLight(pos.getChunkPosition(), pos.getTileWithinChunk());
  }
  
  public int getLightAtWorldPos(GlobalTilePosition worldPosition) {
    return getLight(chunkCoordinateSystem.getChunkPosition(worldPosition), chunkCoordinateSystem.getPositionWithinChunk(worldPosition));
  }
  
  public void calculateChunkLighting(Chunk chunk) {
    if (chunk.hasLight()) {
      Set<TilePosition> sunned = calculateDirectSunLight(chunk);
      calculateReflectedLight(sunned);
    }
  }

  private Set<TilePosition> calculateDirectSunLight(Chunk chunk) {
    Set<TilePosition> res = Sets.newHashSet();
    ChunkPosition pos = chunk.getPosition();
    ChunkLighting lighting = chunkLights.get(pos);
    if(lighting == null) {
      lighting = new ChunkLighting(chunkCoordinateSystem.getChunkSize());
      chunkLights.put(pos, lighting);
    }
    int cs = chunkCoordinateSystem.getChunkSize();
    for (int x = 0; x < cs; x++) {
      for (int z = 0; z < cs; z++) {
        int light = DAY_LIGHT;
        for (int y = cs - 1; y >= 0; y--) {
          if (chunk.get(x, y, z) != Tile.AIR) {
            break;
          }
          LocalTilePosition p = new LocalTilePosition(x, y, z); 
          lighting.setLight(p, light);
          res.add(new TilePosition(pos, p));
        }
      }
    }
    return res;
  }
  
  public void addOpaqueBlock(TilePosition position) {
    //TODO: More efficient implementation
    resetChunkLighting(position.getChunkPosition());
    calculateChunkLighting(chunkProvider.getChunkIfLoaded(position.getChunkPosition()).get());
  }
  
  private void resetChunkLighting(ChunkPosition chunkPosition) {
    chunkLights.put(chunkPosition, new ChunkLighting(chunkCoordinateSystem.getChunkSize()));
  }

  public void removeOpaqueBlock(TilePosition position) {
    updateLightFromNeighbours(position);
    calculateReflectedLight(Sets.newHashSet(position));
  }

  private void updateLightFromNeighbours(TilePosition position) {
    List<TilePosition> neighbours = position.getDirectNeighbourTiles(chunkCoordinateSystem.getChunkSize());
    int light = getLight(position);
    for(TilePosition nPos : neighbours) {
      int nLight = getLight(nPos);
      int rLight = (int)(nLight * LIGHT_FALLOFF); 
      if(rLight > light) {
        light = rLight;
        setLight(position, rLight);
      }
    }
  }
  
  private void calculateReflectedLight(Set<TilePosition> updateStarters) {
    Queue<TilePosition> tbp = Queues.newArrayDeque(updateStarters);
    // Variables to prevent continuous refetching of the active chunk data.
    ChunkPosition lastChunkPos = null;
    ChunkLighting lighting = null;
    Chunk lastChunk = null;
    while(!tbp.isEmpty()) {
      TilePosition pos = tbp.remove();
      // Get new chunk only if chunk position changes
      if(!pos.getChunkPosition().equals(lastChunkPos)) {
        Optional<Chunk> optChunk = chunkProvider.getChunkIfLoaded(pos.getChunkPosition());
        if(!optChunk.isPresent()) continue;
        lighting = chunkLights.get(pos.getChunkPosition());
        lastChunkPos = pos.getChunkPosition();
        lastChunk = optChunk.get();
      }
      List<TilePosition> positions = pos.getDirectNeighbourTiles(chunkCoordinateSystem.getChunkSize());
      int light = lighting == null ? AMBIENT : lighting.getLight(pos.getTileWithinChunk());
      int nv = (int)(LIGHT_FALLOFF * light);
      if(nv > AMBIENT) {
        for(TilePosition nPos : positions) {
          // Get new chunk only if chunk position changes
          if(!nPos.getChunkPosition().equals(lastChunkPos)) {
            Optional<Chunk> optChunk = chunkProvider.getChunkIfLoaded(pos.getChunkPosition());
            if(!optChunk.isPresent()) continue;
            lighting = chunkLights.get(pos.getChunkPosition());
            lastChunkPos = pos.getChunkPosition();
            lastChunk = optChunk.get();
          }
          if(lighting == null) continue;
          if(lastChunk.get(nPos.getTileWithinChunk()) != Tile.AIR) continue;
          int val = lighting.getLight(nPos.getTileWithinChunk());
          if(val < nv) {
            lighting.setLight(nPos.getTileWithinChunk(), nv);
            tbp.add(nPos);
          }
        }
      }
    }
  }
  
}
