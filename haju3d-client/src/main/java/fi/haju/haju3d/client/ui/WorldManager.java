package fi.haju.haju3d.client.ui;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import fi.haju.haju3d.client.ClientSettings;
import fi.haju.haju3d.client.chunk.ChunkLightingManager;
import fi.haju.haju3d.client.chunk.ChunkProvider;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.interaction.WorldEdit;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.TilePosition;
import fi.haju.haju3d.protocol.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class WorldManager {

  private static final float PICK_ACCURACY = 0.001f;

  public static final float SCALE = 1;

  @Inject
  private ChunkProvider chunkProvider;
  @Inject
  private ChunkSpatialBuilder builder;
  @Inject
  private ClientSettings settings;
  @Inject
  private ChunkLightingManager lightingManager;

  private World world = new World();
  private Map<ChunkPosition, ChunkSpatial> chunkSpatials = new ConcurrentHashMap<>();
  private Object lock = new Object();

  private volatile boolean running;
  private volatile ChunkPosition position;

  private Runnable runnable = new Runnable() {
    @Override
    public void run() {
      while (running) {
        try {
          Thread.sleep(10L);
        } catch (InterruptedException e) {
        }
        if (position != null) {
          makeChunkNearPosition(position);
        }
        removeFarChunks(position);
      }
    }
  };

  private GlobalTilePosition getWorldPosition(Vector3f location) {
    return new GlobalTilePosition((int) Math.floor(location.x / SCALE), (int) Math.floor(location.y / SCALE), (int) Math.floor(location.z / SCALE));
  }

  public ChunkPosition getChunkIndexForLocation(Vector3f location) {
    return World.getChunkIndex(getWorldPosition(location));
  }

  public Vector3f getGlobalPosition(GlobalTilePosition worldPosition) {
    return new Vector3f(worldPosition.x * SCALE, worldPosition.y * SCALE, worldPosition.z * SCALE);
  }

  public Vector3f getTerrainCollisionPoint(Vector3f from, Vector3f to, float distanceFix) {
    return getCollisionPoint(from, to, distanceFix, false);
  }

  public TilePosition getVoxelCollisionPoint(Vector3f from, Vector3f to) {
    Vector3f collision = getCollisionPoint(from, to, 0.0f, true);
    int chunkSize = world.getChunkSize();
    if (collision == null) return null;
    // Move collision slightly to the other side of the polygon
    Vector3f collisionTile = collision.add(to.subtract(from).normalize().mult(PICK_ACCURACY));
    return TilePosition.getTilePosition(SCALE, chunkSize, collisionTile);
  }

  public TilePosition getVoxelCollisionDirection(Vector3f from, Vector3f to) {
    Vector3f collision = getCollisionPoint(from, to, 0.0f, true);
    int chunkSize = world.getChunkSize();
    if (collision == null) return null;
    // Move collision slightly to the other side of the polygon
    Vector3f collisionTile = collision.subtract(to.subtract(from).normalize().mult(PICK_ACCURACY));
    return TilePosition.getTilePosition(SCALE, chunkSize, collisionTile);
  }

  private Vector3f getCollisionPoint(Vector3f from, Vector3f to, float distanceFix, boolean useBoxes) {
    Set<ChunkPosition> chunkPositions = Sets.newHashSet(getChunkIndexForLocation(from), getChunkIndexForLocation(to));
    Ray ray = new Ray(from, to.subtract(from).normalize());
    float distance = from.distance(to);
    for (ChunkPosition pos : chunkPositions) {
      ChunkSpatial cs = getChunkSpatial(pos);
      CollisionResults collision = new CollisionResults();
      Spatial spatial = cs == null ? null : (useBoxes ? cs.cubes : cs.lowDetail);
      if (spatial != null && spatial.collideWith(ray, collision) != 0) {
        Vector3f closest = collision.getClosestCollision().getContactPoint();
        boolean collided = closest.distance(from) <= distance + distanceFix;
        if (collided) {
          return closest;
        }
      }
    }
    return null;
  }

  private void removeFarChunks(Vector3i centerChunkIndex) {
    Set<Vector3i> remove = new HashSet<>();
    for (Map.Entry<ChunkPosition, ChunkSpatial> c : chunkSpatials.entrySet()) {
      ChunkPosition v = c.getKey();
      if (centerChunkIndex.distanceTo(v) > settings.getChunkRenderDistance() + 1) {
        remove.add(v);
      }
    }
    for (Vector3i r : remove) {
      chunkSpatials.remove(r);
    }
  }

  private void makeChunkNearPosition(ChunkPosition centerChunkIndex) {
    List<ChunkPosition> indexes = new ArrayList<>();
    indexes.add(centerChunkIndex);
    indexes.addAll(centerChunkIndex.getPositionsAtMaxDistance(settings.getChunkRenderDistance()));
    final Vector3i pos = new Vector3i(position.x, position.y, position.z);
    Collections.sort(indexes, new Comparator<Vector3i>() {
      @Override
      public int compare(Vector3i o1, Vector3i o2) {
        int d1 = o1.distanceTo(pos);
        int d2 = o2.distanceTo(pos);
        if (d1 < d2) {
          return -1;
        } else if (d1 > d2) {
          return 1;
        } else {
          return 0;
        }
      }
    });
    for (ChunkPosition i : indexes) {
      if (chunkSpatials.containsKey(i)) {
        continue;
      }
      makeChunkAt(i);
      break;
    }
  }

  public void rebuildChunkSpatial(ChunkSpatial spatial) {
    builder.rebuildChunkSpatial(world, spatial);
  }

  private void makeChunkAt(ChunkPosition chunkIndex) {
    // need 3x3 chunks around meshing area so that mesh borders can be handled correctly
    List<Chunk> chunks = chunkProvider.getChunks(chunkIndex.getSurroundingPositions());
    for (Chunk c : chunks) {
      calculateChunkLighting(c);
      world.setChunk(c.getPosition(), c);
    }
    ChunkSpatial spatial = builder.makeChunkSpatial(world, chunkIndex);
    chunkSpatials.put(chunkIndex, spatial);
  }

  public void setPosition(ChunkPosition position) {
    this.position = position;
    synchronized (lock) {
      lock.notify();
    }
  }

  public void start() {
    if (running) {
      return;
    }
    running = true;
    Thread thread = new Thread(runnable, "WorldManager");
    thread.setPriority(3);
    thread.start();
  }

  public void stop() {
    running = false;
    synchronized (lock) {
      lock.notify();
    }
  }

  public ChunkSpatial getChunkSpatial(ChunkPosition pos) {
    return chunkSpatials.get(pos);
  }

  public int getChunkSize() {
    return world.getChunkSize();
  }

  public void registerWorldEdits(List<WorldEdit> edits) {
    Set<ChunkPosition> spatialsToUpdate = new HashSet<>();
    for (WorldEdit edit : edits) {
      TilePosition tile = edit.getPosition();
      ChunkPosition chunkPos = tile.getChunkPosition();
      Chunk chunk = world.getChunk(chunkPos);
      int x = tile.getTileWithinChunk().x;
      int y = tile.getTileWithinChunk().y;
      int z = tile.getTileWithinChunk().z;
      chunk.set(x, y, z, edit.getNewTile());

      if (y < chunk.getHeight() - 1 && lightingManager.getLight(chunkPos, new LocalTilePosition(x, y + 1, z)) == 100) {
        if (edit.getNewTile() == Tile.AIR) {
          // fill darkness with light
          int light = 100;
          for (int yy = y; yy >= 0 && chunk.get(x, yy, z) == Tile.AIR; yy--) {
            lightingManager.setLight(chunkPos, new LocalTilePosition(x, yy, z), light);
          }
        } else if (edit.getNewTile() != Tile.AIR) {
          // fill light with darkness
          int light = 20;
          for (int yy = y; yy >= 0 && lightingManager.getLight(chunkPos, new LocalTilePosition(x, yy, z)) == 100; yy--) {
            lightingManager.setLight(chunkPos, new LocalTilePosition(x, yy, z), light);
          }
        }
      }

      spatialsToUpdate.add(tile.getChunkPosition());

      // Update also the bordering chunks if necessary
      if (x < ChunkSpatialBuilder.SMOOTH_BUFFER) {
        spatialsToUpdate.add(tile.getChunkPosition().add(-1, 0, 0));
      }
      if (x >= world.getChunkSize() - ChunkSpatialBuilder.SMOOTH_BUFFER) {
        spatialsToUpdate.add(tile.getChunkPosition().add(1, 0, 0));
      }
      if (y < ChunkSpatialBuilder.SMOOTH_BUFFER) {
        spatialsToUpdate.add(tile.getChunkPosition().add(0, -1, 0));
      }
      if (y >= world.getChunkSize() - ChunkSpatialBuilder.SMOOTH_BUFFER) {
        spatialsToUpdate.add(tile.getChunkPosition().add(0, 1, 0));
      }
      if (z < ChunkSpatialBuilder.SMOOTH_BUFFER) {
        spatialsToUpdate.add(tile.getChunkPosition().add(0, 0, -1));
      }
      if (z >= world.getChunkSize() - ChunkSpatialBuilder.SMOOTH_BUFFER) {
        spatialsToUpdate.add(tile.getChunkPosition().add(0, 0, 1));
      }
    }

    for (ChunkPosition s : spatialsToUpdate) {
      rebuildChunkSpatial(getChunkSpatial(s));
    }
  }

  private void calculateChunkLighting(Chunk chunk) {
    if (chunk.hasLight()) {
      for (int x = 0; x < chunk.getWidth(); x++) {
        for (int z = 0; z < chunk.getDepth(); z++) {
          int light = 100;
          for (int y = chunk.getHeight() - 1; y >= 0; y--) {
            if (chunk.get(x, y, z) != Tile.AIR) {
              light = 20;
            }
            lightingManager.setLight(chunk.getPosition(), new LocalTilePosition(x, y, z), light);
          }
        }
      }
    }
  }


}
