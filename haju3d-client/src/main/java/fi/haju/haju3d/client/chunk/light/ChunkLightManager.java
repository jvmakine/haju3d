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
import fi.haju.haju3d.protocol.coordinate.TilePosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.ChunkCoordinateSystem;
import fi.haju.haju3d.protocol.world.Tile;

@Singleton
public final class ChunkLightManager {

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
      calculateLightFromNeighbours(chunk);
    }
  }
  
  private Optional<Tile> getTileAt(TilePosition p) {
    Optional<Chunk> c = chunkProvider.getChunkIfLoaded(p.getChunkPosition());
    if(!c.isPresent()) {
      return Optional.absent();
    } else {
      return Optional.of(c.get().get(p.getTileWithinChunk()));
    }
  }

  private void calculateLightFromNeighbours(Chunk chunk) {
    Set<TilePosition> edge = chunk.getPosition().getEdgeTilePositions(chunkCoordinateSystem.getChunkSize());
    Set<TilePosition> updated = Sets.newHashSet();
    for(TilePosition pos : edge) {
      if(updateLightFromNeighbours(pos)) {
        updated.add(pos);
      }
    }
    calculateReflectedLight(updated);
  }

  private Set<TilePosition> calculateDirectSunLight(Chunk chunk) {
    Set<TilePosition> res = Sets.newHashSet();
    ChunkPosition pos = chunk.getPosition();
    ChunkLighting lighting = chunkLights.get(pos);
    int cs = chunkCoordinateSystem.getChunkSize();
    if(lighting == null) {
      lighting = new ChunkLighting(cs);
      chunkLights.put(pos, lighting);
    }
    for (int x = 0; x < cs; x++) {
      for (int z = 0; z < cs; z++) {
        int light = DAY_LIGHT;
        for (int y = cs - 1; y >= 0; y--) {
          if (isOpaque(chunk.get(x, y, z))) {
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
    int light = getLight(position);
    int maxDist = (int)Math.ceil(Math.log((double)AMBIENT/(double)light)/Math.log(LIGHT_FALLOFF));
    Set<TilePosition> edge = updateDarkness(position, maxDist);
    setLight(position, AMBIENT);
    calculateReflectedLight(edge);
  }
  
  private static class DarkUpdater {
    public TilePosition pos;
    public int dist;
    
    public DarkUpdater(TilePosition pos, int d) {
      this.pos = pos;
      this.dist = d;
    }
    
  }
  
  private Set<TilePosition> updateDarkness(TilePosition position, int maxDist) {
    Set<TilePosition> edge = Sets.newHashSet();
    Queue<DarkUpdater> tbd = Queues.newArrayDeque();
    tbd.add(new DarkUpdater(position, 0));
    while(!tbd.isEmpty()) {
      DarkUpdater current = tbd.remove();
      if(current.dist > maxDist) continue;
      int l = getLight(current.pos);
      setLight(current.pos, AMBIENT);
      List<TilePosition> neighs = current.pos.getDirectNeighbourTiles(chunkCoordinateSystem.getChunkSize());
      for(TilePosition n : neighs) {
        Optional<Tile> optTile = getTileAt(n);
        if(!optTile.isPresent() || isOpaque(optTile.get())) continue;
        int nl = getLight(n);
        if(nl > l) {
          edge.add(n);
        } else if(nl < l && nl > AMBIENT) {
          tbd.add(new DarkUpdater(n, current.dist+1));
        }
      }
    }
    return edge;
  }

  public void removeOpaqueBlock(TilePosition position) {
    updateLightFromNeighbours(position);
    calculateReflectedLight(Sets.newHashSet(position));
  }

  private boolean updateLightFromNeighbours(TilePosition position) {
    List<TilePosition> neighbours = position.getDirectNeighbourTiles(chunkCoordinateSystem.getChunkSize());
    int light = getLight(position);
    boolean updated = false;
    for(TilePosition nPos : neighbours) {
      Optional<Tile> optTile = getTileAt(nPos);
      if(!optTile.isPresent() || isOpaque(optTile.get())) continue;
      int nLight = getLight(nPos);
      int rLight = (int)(nLight * LIGHT_FALLOFF); 
      if(rLight > light) {
        updated = true;
        light = rLight;
        setLight(position, rLight);
      }
    }
    return updated;
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
          if(isOpaque(lastChunk.get(nPos.getTileWithinChunk()))) continue;
          int val = lighting.getLight(nPos.getTileWithinChunk());
          if(val < nv) {
            lighting.setLight(nPos.getTileWithinChunk(), nv);
            tbp.add(nPos);
          }
        }
      }
    }
  }
  
  private boolean isOpaque(Tile t) {
    return t != Tile.AIR;
  }
  
}
