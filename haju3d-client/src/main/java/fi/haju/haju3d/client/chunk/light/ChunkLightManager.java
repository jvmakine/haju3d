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
import fi.haju.haju3d.client.util.Profiled;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.coordinate.TilePosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.ChunkCoordinateSystem;
import fi.haju.haju3d.protocol.world.Tile;

@Singleton
public class ChunkLightManager {

  private Map<ChunkPosition, ChunkLighting> chunkLights = new ConcurrentHashMap<>();
  
  public static final TileLight AMBIENT = new TileLight(0, 0, 0, false, false);
  public static final TileLight DAY_LIGHT = new TileLight(14, 14, 14, true, true);

  private ChunkCoordinateSystem chunkCoordinateSystem = ChunkCoordinateSystem.DEFAULT;

  
  @Inject
  private ChunkProvider chunkProvider;
    
  public TileLight getLight(ChunkPosition chunkPosition, LocalTilePosition position) {
    ChunkLighting light = chunkLights.get(chunkPosition);
    if(light == null) return new TileLight();
    return light.getLight(position);
  }
    
  public TileLight getLight(TilePosition pos) {
    return getLight(pos.getChunkPosition(), pos.getTileWithinChunk());
  }
  
  public TileLight getLightAtWorldPos(GlobalTilePosition worldPosition) {
    return getLight(chunkCoordinateSystem.getChunkPosition(worldPosition), chunkCoordinateSystem.getPositionWithinChunk(worldPosition));
  }
  
  public void updateChunkLigh(Chunk chunk) {
    if (chunk.hasLight()) {
      calculateChunkLight(chunk);
    }
  }
  
  @Profiled
  protected void calculateChunkLight(Chunk chunk) {
    Set<TilePosition> lit = calculateDirectSunLight(chunk);
    lit.addAll(calculateLightFromNeighbours(chunk));
    calculateReflectedLight(lit);
  }
  
  /**
   * @return set of chunks affected by this operation
   */
  @Profiled
  public Set<ChunkPosition> removeOpaqueBlock(TilePosition position) {
    if(new LightAccessor().isSunLight(position.add(LocalTilePosition.UP, chunkCoordinateSystem.getChunkSize()))) {
      addSunlightFrom(position);
    }
    updateLightFromNeighbours(position);
    return calculateReflectedLight(Sets.newHashSet(position));
  }

  /**
   * @return set of chunks affected by this operation
   */
  @Profiled
  public Set<ChunkPosition> addOpaqueBlock(TilePosition position) {
    Set<TilePosition> edge = null;
    Set<ChunkPosition> result = Sets.newHashSet();
    if(new LightAccessor().isSunLight(position)) {
      edge = blockSunLight(position, TileLight.MAX_DISTANCE);
    } else {
      edge = updateDarkness(position, TileLight.MAX_DISTANCE);
    }
    for(TilePosition p : edge) {
      result.add(p.getChunkPosition());
    }
    result.addAll(calculateReflectedLight(edge));
    return result;
  }
  
  private void addSunlightFrom(TilePosition position) {
    Queue<TilePosition> workQueue = Queues.newArrayDeque();
    workQueue.add(position);
    LightAccessor accessor = new LightAccessor();
    while(!workQueue.isEmpty()) {
      TilePosition pos = workQueue.remove();
      accessor.setLight(pos, DAY_LIGHT);
      accessor.setSunLight(pos, true);
      calculateReflectedLight(Sets.newHashSet(pos));
      TilePosition next = pos.add(LocalTilePosition.DOWN, chunkCoordinateSystem.getChunkSize());
      if(!isOpaque(accessor.getTileAt(next))) {
        workQueue.add(next);
      }
    }
  }
  
  private Set<TilePosition> blockSunLight(TilePosition position, int maxDist) {
    LightAccessor accessor = new LightAccessor();
    accessor.setSunLight(position, false);
    TilePosition down = position.add(LocalTilePosition.DOWN, chunkCoordinateSystem.getChunkSize());
    Set<TilePosition> edge = Sets.newHashSet();
    if(accessor.isSunLight(down)) {
      edge.addAll(blockSunLight(down, maxDist));
    }
    edge.addAll(updateDarkness(position, maxDist));
    return edge;
  }
  
  private Set<TilePosition> calculateLightFromNeighbours(Chunk chunk) {
    Set<TilePosition> edge = chunk.getPosition().getEdgeTilePositions(chunkCoordinateSystem.getChunkSize());
    Set<TilePosition> updated = Sets.newHashSet();
    LightAccessor accessor = new LightAccessor();
    for(TilePosition pos : edge) {
      if(isOpaque(accessor.getTileAt(pos))) continue;
      if(updateLightFromNeighbours(pos)) {
        updated.add(pos);
      }
    }
    return updated;
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
        for (int y = cs - 1; y >= 0; y--) {
          if (isOpaque(chunk.get(x, y, z))) {
            break;
          }
          LocalTilePosition p = new LocalTilePosition(x, y, z); 
          lighting.setLight(p, DAY_LIGHT);
          res.add(new TilePosition(pos, p));
        }
      }
    }
    return res;
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
    LightAccessor accessor = new LightAccessor();
    Set<TilePosition> edge = Sets.newHashSet();
    Set<TilePosition> processed = Sets.newHashSet();
    Queue<DarkUpdater> tbd = Queues.newArrayDeque();
    tbd.add(new DarkUpdater(position, 0));
    while(!tbd.isEmpty()) {
      DarkUpdater current = tbd.remove();
      TileLight l = accessor.getLight(current.pos);
      accessor.setLight(current.pos, AMBIENT);
      processed.add(current.pos);
      if(edge.contains(current.pos)) {
        edge.remove(current.pos);
      }
      List<TilePosition> neighs = current.pos.getDirectNeighbourTiles(chunkCoordinateSystem.getChunkSize());
      for(TilePosition n : neighs) {
        Optional<Tile> optTile = accessor.getTileAt(n);
        if(!optTile.isPresent() || isOpaque(optTile.get())) {
          continue;
        }
        if(current.dist+1 > maxDist) {
          if(!processed.contains(n)) edge.add(n);
          continue;
        }
        TileLight nl = accessor.getLight(n);
        if(nl.hasBrighter(l.getDimmer())) {
          if(!processed.contains(n)) edge.add(n);
        } else if(l.hasBrighter(nl) && nl.hasBrighter(AMBIENT)) {
          tbd.add(new DarkUpdater(n, current.dist+1));
        }
      }
    }
    return edge;
  }

  private boolean updateLightFromNeighbours(TilePosition position) {
    List<TilePosition> neighbours = position.getDirectNeighbourTiles(chunkCoordinateSystem.getChunkSize());
    TileLight light = getLight(position);
    boolean updated = false;
    LightAccessor accessor = new LightAccessor();
    for(TilePosition nPos : neighbours) {
      Optional<Tile> optTile = accessor.getTileAt(nPos);
      if(!optTile.isPresent() || isOpaque(optTile.get())) continue;
      TileLight nLight = accessor.getLight(nPos);
      TileLight rLight = nLight.getDimmer(); 
      if(rLight.hasBrighter(light)) {
        updated = true;
        light = rLight;
        accessor.setLight(position, rLight);
      }
    }
    return updated;
  }
  
  /**
   * Helper class to prevent continuous references chunkLights hashMap.
   * Optimizes light calculations
   */
  private final class LightAccessor {
    private ChunkLighting lastLighting;
    private ChunkPosition lastPosition;
    private Chunk lastChunk;
    
    private void updatePos(ChunkPosition pos) {
      if(!pos.equals(lastPosition)) {
        lastPosition = pos;
        lastLighting = chunkLights.get(lastPosition);
        Optional<Chunk> c = chunkProvider.getChunkIfLoaded(lastPosition);
        if(c.isPresent()) lastChunk = c.get();
        else lastChunk = null;
      }
    }
    
    public void setLight(TilePosition pos, TileLight light) {
      updatePos(pos.getChunkPosition());
      if(lastLighting != null) lastLighting.setLight(pos.getTileWithinChunk(), light);
    }
    
    public TileLight getLight(TilePosition pos) {
      updatePos(pos.getChunkPosition());
      return lastLighting == null ? new TileLight() : lastLighting.getLight(pos.getTileWithinChunk());
    }
    
    public Optional<Tile> getTileAt(TilePosition pos) {
      updatePos(pos.getChunkPosition());
      if(lastChunk == null) {
        return Optional.absent();
      } else {
        return Optional.of(lastChunk.get(pos.getTileWithinChunk()));
      }
    }
    
    public void setSunLight(TilePosition pos, boolean sun) {
      updatePos(pos.getChunkPosition());
      if(lastLighting == null) return;
      TileLight val = lastLighting.getLight(pos.getTileWithinChunk());
      val.inSun = sun;
      val.source = sun;
      lastLighting.setLight(pos.getTileWithinChunk(), val);
    }
    
    public boolean isSunLight(TilePosition pos) {
      updatePos(pos.getChunkPosition());
      if(lastLighting == null) return false;
      return lastLighting.getLight(pos.getTileWithinChunk()).inSun;
    }
    
  }
  
  @Profiled
  protected Set<ChunkPosition> calculateReflectedLight(Set<TilePosition> updateStarters) {
    int chunkSize = chunkCoordinateSystem.getChunkSize();
    LightAccessor accessor = new LightAccessor();
    Set<ChunkPosition> affectedChunks = Sets.newHashSet();
    Queue<TilePosition> tbp = Queues.newArrayDeque(updateStarters);
    while(!tbp.isEmpty()) {
      TilePosition pos = tbp.remove();
      TileLight nv = accessor.getLight(pos);
      nv.setDimmer();
      if(nv.hasLight()) {
        List<TilePosition> positions = pos.getDirectNeighbourTiles(chunkSize);
        for(TilePosition nPos : positions) {
          if(isOpaque(accessor.getTileAt(nPos))) continue;
          TileLight val = accessor.getLight(nPos);
          if(nv.hasBrighter(val)) {
            accessor.setLight(nPos, val.combineBrightest(nv));
            affectedChunks.add(nPos.getChunkPosition());
            tbp.add(nPos);
          }
        }
      }
    }
    return affectedChunks;
  }
  
  private boolean isOpaque(Tile t) {
    return t != Tile.AIR;
  }
  
  private boolean isOpaque(Optional<Tile> t) {
    return !t.isPresent() || isOpaque(t.get());
  }
  
}
